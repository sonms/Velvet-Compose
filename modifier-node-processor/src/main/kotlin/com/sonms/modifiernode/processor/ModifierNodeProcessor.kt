package com.sonms.modifiernode.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val FACTORY_ANNOTATION = "com.sonms.modifiernode.annotations.ModifierNodeFactory"

private val MODIFIER = ClassName("androidx.compose.ui", "Modifier")
private val MODIFIER_NODE_ELEMENT = ClassName("androidx.compose.ui.node", "ModifierNodeElement")
private val INSPECTOR_INFO = ClassName("androidx.compose.ui.platform", "InspectorInfo")
private val INVALIDATE_DRAW = MemberName("androidx.compose.ui.node", "invalidateDraw")
private val INVALIDATE_MEASUREMENT = MemberName("androidx.compose.ui.node", "invalidateMeasurement")
private val INVALIDATE_PLACEMENT = MemberName("androidx.compose.ui.node", "invalidatePlacement")

private const val DRAW_MODIFIER_NODE = "androidx.compose.ui.node.DrawModifierNode"
private const val LAYOUT_MODIFIER_NODE = "androidx.compose.ui.node.LayoutModifierNode"

/** 무효화 범위. 처리기 내부 표현. */
private enum class Scope { Measure, Placement, Draw, Semantics, ParentData, None }

/** @SkipWhenFalse / @SkipWhenTrue 처리기 내부 표현. */
private enum class SkipKind { WhenFalse, WhenTrue }

class ModifierNodeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(FACTORY_ANNOTATION).toList()
        val deferred = symbols.filterNot { it.validate() }

        symbols.filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .forEach { runCatching { generate(it) }.onFailure { e -> logger.error("codegen failed: ${e.message}", it) } }

        return deferred
    }

    private fun generate(node: KSClassDeclaration) {
        val pkg = node.packageName.asString()
        val nodeName = node.simpleName.asString()
        val nodeClass = ClassName(pkg, nodeName)

        val factoryAnn = node.annotations.first { it.shortName.asString() == "ModifierNodeFactory" }
        val explicitName = (factoryAnn.argValue("name") as? String).orEmpty()
        val funName = explicitName.ifBlank {
            nodeName.removeSuffix("Node").replaceFirstChar { it.lowercase() }
        }
        val elementName = "${nodeName.removeSuffix("Node")}Element"
        val elementClass = ClassName(pkg, elementName)

        // 확장 함수 가시성: @ModifierNodeFactory(visibility=...), 기본 Public.
        val funVisibility = when (factoryAnn.enumArgName("visibility")) {
            "Internal" -> KModifier.INTERNAL
            else -> KModifier.PUBLIC
        }
        // Element 는 노드 가시성과 무관하게 항상 internal (ABI 표면에서 제외).
        val elementVisibility = KModifier.INTERNAL

        // Node 가시성 검증.
        when {
            node.isPrivate() -> {
                logger.error(
                    "@ModifierNodeFactory node must not be private — the generated $elementName " +
                        "(separate file) cannot reference it. Use 'internal'.",
                    node,
                )
                return
            }
            node.isPublic() -> logger.warn(
                "@ModifierNodeFactory node '$nodeName' is public → it is part of your binary API. " +
                    "Make it 'internal' so only Modifier.$funName(...) is exposed.",
                node,
            )
        }

        val ctor = node.primaryConstructor
        if (ctor == null) {
            logger.error("@ModifierNodeFactory requires a primary constructor", node)
            return
        }

        val supported = node.supportedScopes()
        if (supported.isEmpty()) {
            logger.error(
                "@ModifierNodeFactory node must implement DrawModifierNode and/or LayoutModifierNode (MVP)",
                node,
            )
            return
        }

        val params = ctor.parameters.map { p -> resolveParam(p, node, supported) }

        // Compose 는 update() 직후 autoInvalidateUpdatedNode() 로 노드의 모든 capability 를
        // 무효화한다(NodeChain.updateNode → NodeKind.autoInvalidateNodeSelf). 세분화된 update()
        // 가 실효를 가지려면 Node 가 `shouldAutoInvalidate = false` 를 선언해야 한다.
        // (지원되는 문서화된 API — Compose 의 graphicsLayer/paint 모디파이어가 동일 패턴 사용.
        //  자세한 내용: ARCHITECTURE.md §10.)
        // codegen 은 Node 클래스를 수정 못 하므로, @Invalidates 를 쓴 경우 이 선언을 요구한다.
        val hasExplicitScopes = ctor.parameters.any { p ->
            p.annotations.any { it.shortName.asString() == "Invalidates" }
        }
        if (hasExplicitScopes && !nodeDeclaresShouldAutoInvalidate(node)) {
            logger.error(
                "@Invalidates 를 쓰려면 Node 에 다음 한 줄이 필요하다 (그래야 세분화가 실효를 가짐):\n" +
                    "    override val shouldAutoInvalidate: Boolean get() = false\n" +
                    "미선언 시 Compose 가 update() 후 모든 capability 를 무효화하여 @Invalidates 가 무의미해진다.",
                node,
            )
        }

        // @SkipWhenFalse / @SkipWhenTrue → 확장 함수 앞에 가드 삽입.
        val skipConditions = params.mapNotNull { p ->
            when (p.skip) {
                SkipKind.WhenFalse -> "!${p.name}"
                SkipKind.WhenTrue -> p.name
                null -> null
            }
        }

        // --- 확장 함수: fun Modifier.<name>(...): Modifier { if (<skip>) return this; return this.then(...) } ---
        val extFun = FunSpec.builder(funName)
            .addModifiers(funVisibility)
            .receiver(MODIFIER)
            .returns(MODIFIER)
            .apply { params.forEach { addParameter(it.name, it.type) } }
            .apply {
                if (skipConditions.isNotEmpty()) {
                    addStatement("if (%L) return this", skipConditions.joinToString(" || "))
                }
            }
            .addStatement(
                "return this.then(%T(%L))",
                elementClass,
                params.joinToString(", ") { it.name },
            )
            .build()

        // --- Element 클래스 ---
        val elementType = TypeSpec.classBuilder(elementClass)
            .addModifiers(elementVisibility)
            .superclass(MODIFIER_NODE_ELEMENT.parameterizedBy(nodeClass))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .apply { params.forEach { addParameter(it.name, it.type) } }
                    .build(),
            )
            .apply {
                params.forEach {
                    addProperty(
                        PropertySpec.builder(it.name, it.type, KModifier.PRIVATE)
                            .initializer(it.name)
                            .build(),
                    )
                }
            }
            .addFunction(createFun(nodeClass, params))
            .addFunction(updateFun(nodeClass, params))
            .addFunction(inspectableFun(funName, params))
            .addFunction(equalsFun(elementClass, params))
            .addFunction(hashCodeFun(params))
            .build()

        FileSpec.builder(pkg, elementName)
            .addFunction(extFun)
            .addType(elementType)
            .build()
            .writeTo(codeGenerator, aggregating = false, originatingKSFiles = listOfNotNull(node.containingFile))
    }

    private data class Param(
        val name: String,
        val type: com.squareup.kotlinpoet.TypeName,
        val scopes: Set<Scope>,
        val skip: SkipKind?,
    )

    private fun resolveParam(p: KSValueParameter, node: KSClassDeclaration, supported: Set<Scope>): Param {
        val name = p.name!!.asString()
        if (!p.isVar) {
            logger.error("Parameter '$name' must be a 'var' property parameter", p)
        }

        val skipFalse = p.annotations.any { it.shortName.asString() == "SkipWhenFalse" }
        val skipTrue = p.annotations.any { it.shortName.asString() == "SkipWhenTrue" }
        val isBoolean = p.type.resolve().declaration.qualifiedName?.asString() == "kotlin.Boolean"
        if ((skipFalse || skipTrue) && !isBoolean) {
            logger.error("@SkipWhen* requires a Boolean parameter; '$name' is not Boolean", p)
        }
        if (skipFalse && skipTrue) {
            logger.error("'$name' has both @SkipWhenFalse and @SkipWhenTrue", p)
        }
        val skip = when {
            skipFalse -> SkipKind.WhenFalse
            skipTrue -> SkipKind.WhenTrue
            else -> null
        }
        val invAnn = p.annotations.firstOrNull { it.shortName.asString() == "Invalidates" }
        val scopes: Set<Scope> = when {
            invAnn == null -> supported // 안전 기본값
            else -> {
                val declared = invAnn.enumArgNames("scopes").mapNotNull { runCatching { Scope.valueOf(it) }.getOrNull() }.toSet()
                when {
                    Scope.None in declared -> emptySet()
                    else -> {
                        declared.forEach { s ->
                            val ok = when (s) {
                                Scope.Draw -> Scope.Draw in supported
                                Scope.Measure, Scope.Placement -> Scope.Measure in supported
                                Scope.Semantics, Scope.ParentData ->
                                    false.also { logger.error("@Invalidates($s) not supported yet (MVP)", p) }
                                Scope.None -> true
                            }
                            if (!ok && s != Scope.Semantics && s != Scope.ParentData) {
                                logger.error(
                                    "@Invalidates($s) on '$name' but ${node.simpleName.asString()} does not implement the matching node interface",
                                    p,
                                )
                            }
                        }
                        declared
                    }
                }
            }
        }
        return Param(name, p.type.toTypeName(), scopes, skip)
    }

    private fun createFun(nodeClass: ClassName, params: List<Param>) = FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .returns(nodeClass)
        .addStatement("return %T(%L)", nodeClass, params.joinToString(", ") { it.name })
        .build()

    private fun updateFun(nodeClass: ClassName, params: List<Param>): FunSpec {
        val b = FunSpec.builder("update")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("node", nodeClass)

        val anyDraw = params.any { Scope.Draw in it.scopes }
        val anyMeasure = params.any { Scope.Measure in it.scopes }
        val anyPlacement = params.any { Scope.Placement in it.scopes && Scope.Measure !in it.scopes }

        if (anyDraw) b.addStatement("var redraw = false")
        if (anyMeasure) b.addStatement("var remeasure = false")
        if (anyPlacement) b.addStatement("var replace = false")

        params.forEach { p ->
            if (p.scopes.isEmpty()) {
                b.addStatement("if (node.%N != %N) node.%N = %N", p.name, p.name, p.name, p.name)
            } else {
                b.beginControlFlow("if (node.%N != %N)", p.name, p.name)
                b.addStatement("node.%N = %N", p.name, p.name)
                if (Scope.Draw in p.scopes) b.addStatement("redraw = true")
                if (Scope.Measure in p.scopes) b.addStatement("remeasure = true")
                else if (Scope.Placement in p.scopes) b.addStatement("replace = true")
                b.endControlFlow()
            }
        }

        // 계층: Measure > Placement > Draw. 상위 하나만 호출.
        when {
            anyMeasure && (anyPlacement || anyDraw) -> {
                b.beginControlFlow("if (remeasure)")
                b.addStatement("node.%M()", INVALIDATE_MEASUREMENT)
                if (anyPlacement) {
                    b.nextControlFlow("else if (replace)")
                    b.addStatement("node.%M()", INVALIDATE_PLACEMENT)
                }
                if (anyDraw) {
                    b.nextControlFlow("else if (redraw)")
                    b.addStatement("node.%M()", INVALIDATE_DRAW)
                }
                b.endControlFlow()
            }
            anyMeasure -> {
                b.beginControlFlow("if (remeasure)")
                b.addStatement("node.%M()", INVALIDATE_MEASUREMENT)
                b.endControlFlow()
            }
            anyPlacement && anyDraw -> {
                b.beginControlFlow("if (replace)")
                b.addStatement("node.%M()", INVALIDATE_PLACEMENT)
                b.nextControlFlow("else if (redraw)")
                b.addStatement("node.%M()", INVALIDATE_DRAW)
                b.endControlFlow()
            }
            anyPlacement -> {
                b.beginControlFlow("if (replace)")
                b.addStatement("node.%M()", INVALIDATE_PLACEMENT)
                b.endControlFlow()
            }
            anyDraw -> {
                b.beginControlFlow("if (redraw)")
                b.addStatement("node.%M()", INVALIDATE_DRAW)
                b.endControlFlow()
            }
        }
        return b.build()
    }

    private fun inspectableFun(name: String, params: List<Param>) = FunSpec.builder("inspectableProperties")
        .addModifiers(KModifier.OVERRIDE)
        .receiver(INSPECTOR_INFO)
        .addStatement("name = %S", name)
        .apply { params.forEach { addStatement("properties[%S] = %N", it.name, it.name) } }
        .build()

    private fun equalsFun(elementClass: ClassName, params: List<Param>): FunSpec {
        val b = FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", ANY_NULLABLE)
            .returns(Boolean::class)
            .addStatement("if (this === other) return true")
            .addStatement("if (other !is %T) return false", elementClass)
        params.forEach { b.addStatement("if (%N != other.%N) return false", it.name, it.name) }
        b.addStatement("return true")
        return b.build()
    }

    private fun hashCodeFun(params: List<Param>): FunSpec {
        val b = FunSpec.builder("hashCode")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Int::class)
        if (params.isEmpty()) {
            b.addStatement("return %T::class.hashCode()", MODIFIER)
            return b.build()
        }
        b.addStatement("var result = %N.hashCode()", params.first().name)
        params.drop(1).forEach { b.addStatement("result = 31 * result + %N.hashCode()", it.name) }
        b.addStatement("return result")
        return b.build()
    }

    private fun KSClassDeclaration.supportedScopes(): Set<Scope> {
        val supers = (listOf(this) + getAllSuperTypes().mapNotNull { it.declaration as? KSClassDeclaration })
            .mapNotNull { it.qualifiedName?.asString() }
            .toSet()
        return buildSet {
            if (DRAW_MODIFIER_NODE in supers) add(Scope.Draw)
            if (LAYOUT_MODIFIER_NODE in supers) {
                add(Scope.Measure)
                add(Scope.Placement)
            }
        }
    }

    private fun KSClassDeclaration.isPublic(): Boolean =
        modifiers.none { it.name == "INTERNAL" || it.name == "PRIVATE" || it.name == "PROTECTED" }

    private fun KSClassDeclaration.isPrivate(): Boolean =
        modifiers.any { it.name == "PRIVATE" }

    /** Node 클래스가 자체적으로 `shouldAutoInvalidate` 를 override 선언했는지 (값 false 여부는 신뢰). */
    private fun nodeDeclaresShouldAutoInvalidate(node: KSClassDeclaration): Boolean =
        node.getDeclaredProperties().any { it.simpleName.asString() == "shouldAutoInvalidate" }

    companion object {
        private val ANY_NULLABLE = ClassName("kotlin", "Any").copy(nullable = true)
    }
}

/** 이름 있는 인자 값 조회. */
private fun KSAnnotation.argValue(name: String): Any? =
    arguments.firstOrNull { it.name?.asString() == name }?.value

/** 단일 enum 인자를 엔트리 simpleName 으로 (없으면 null). */
private fun KSAnnotation.enumArgName(name: String): String? = when (val raw = argValue(name)) {
    is KSType -> raw.declaration.simpleName.asString()
    is KSClassDeclaration -> raw.simpleName.asString()
    null -> null
    else -> raw.toString().substringAfterLast('.')
}

/** enum vararg 인자를 엔트리 simpleName 리스트로. */
private fun KSAnnotation.enumArgNames(name: String): List<String> {
    val raw = argValue(name) ?: return emptyList()
    val list = when (raw) {
        is List<*> -> raw
        is Array<*> -> raw.toList()
        else -> listOf(raw)
    }
    return list.mapNotNull { entry ->
        when (entry) {
            is KSType -> entry.declaration.simpleName.asString()
            is KSClassDeclaration -> entry.simpleName.asString()
            else -> entry?.toString()?.substringAfterLast('.')
        }
    }
}
