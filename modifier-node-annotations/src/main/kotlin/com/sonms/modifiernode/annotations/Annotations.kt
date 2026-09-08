package com.sonms.modifiernode.annotations

/**
 * `Modifier.Node` 팩토리 생성 대상 표시.
 *
 * 이 어노테이션이 붙은 `Modifier.Node` 서브클래스로부터 KSP가 다음을 생성한다.
 *  - `<Name>Element : ModifierNodeElement<ThisNode>` (create / update / equals / hashCode / inspectableProperties)
 *  - `fun Modifier.<name>(...): Modifier` 공개 확장 함수
 *
 * 생성 규칙:
 *  - 파라미터 원천은 **주 생성자**다. 모든 추적 대상 파라미터는 `var` 프로퍼티 파라미터여야 한다.
 *  - 파라미터별 무효화 범위는 [Invalidates] 로 지정한다. 미지정 시 노드가 구현한 모든 범위를 무효화한다(안전 기본값).
 *
 * MVP 한계:
 *  - 기본값(default argument)은 생성 함수에 재현되지 않는다. 호출부에서 모든 인자를 전달해야 한다.
 *  - 지원 노드 인터페이스는 `DrawModifierNode`, `LayoutModifierNode` 뿐이다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ModifierNodeFactory(
    /** 생성될 확장 함수 이름. 비우면 `<NodeClassName>` 에서 `Node` 접미사를 떼고 첫 글자를 소문자로 바꿔 유도한다. */
    val name: String = "",
    /**
     * 생성될 `Modifier.<name>(...)` 확장 함수의 가시성.
     * 생성된 `<Name>Element` 는 이 값과 무관하게 항상 `internal` 이다 (ABI 표면에서 제외).
     */
    val visibility: GeneratedVisibility = GeneratedVisibility.Public,
)

/** [ModifierNodeFactory.visibility] 값. */
enum class GeneratedVisibility { Public, Internal }

/**
 * 이 파라미터가 바뀌었을 때 트리거할 무효화 범위.
 *
 * 여러 범위를 지정할 수 있다. 예: `@Invalidates(Draw, Semantics)`.
 * [InvalidationScope.None] 은 어떤 무효화도 하지 않음을 뜻한다(생성 시점에만 쓰이거나
 * `observeReads` 로 반응적으로 읽는 값).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Invalidates(
    vararg val scopes: InvalidationScope,
)

/**
 * 무효화 범위. 계층: [Measure] > [Placement] > [Draw] (상위 하나만 호출).
 * [Semantics], [ParentData] 는 직교(별도 호출). [None] 은 무효화 없음.
 */
enum class InvalidationScope {
    Measure,
    Placement,
    Draw,
    Semantics,
    ParentData,
    None,
}
