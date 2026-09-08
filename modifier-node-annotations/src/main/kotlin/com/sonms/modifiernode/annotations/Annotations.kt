package com.sonms.modifiernode.annotations

/**
 * 이 `Modifier.Node` 서브클래스로부터 `ModifierNodeElement` 와 `Modifier` 확장 함수를 생성한다.
 * Generates a `ModifierNodeElement` and a `Modifier` extension function from this `Modifier.Node` subclass.
 *
 * 파라미터는 주 생성자에서 읽으며, 각각 `var` 프로퍼티 파라미터여야 한다.
 * Parameters are read from the primary constructor and must each be a `var` property parameter.
 *
 * @param name 생성될 확장 함수 이름. 비우면 클래스명에서 `Node` 접미사를 떼어 유도한다.
 *             Name of the generated extension function. Derived from the class name (minus `Node`) if blank.
 * @param visibility 생성될 확장 함수의 가시성. 생성된 Element 는 항상 `internal` 이다.
 *                   Visibility of the generated extension function. The generated Element is always `internal`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ModifierNodeFactory(
    val name: String = "",
    val visibility: GeneratedVisibility = GeneratedVisibility.Public,
)

/** [ModifierNodeFactory.visibility] 값. Value for [ModifierNodeFactory.visibility]. */
enum class GeneratedVisibility { Public, Internal }

/**
 * 이 파라미터가 바뀌었을 때 무효화할 범위. 미지정 시 노드가 구현한 모든 범위를 무효화한다.
 * Invalidation scopes to trigger when this parameter changes. Defaults to every scope the node implements.
 *
 * `@Invalidates` 를 쓰려면 노드가 `override val shouldAutoInvalidate get() = false` 를 선언해야 한다.
 * Using `@Invalidates` requires the node to declare `override val shouldAutoInvalidate get() = false`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Invalidates(vararg val scopes: InvalidationScope)

/**
 * 무효화 범위. 계층은 [Measure] > [Placement] > [Draw] (상위 하나만 호출).
 * Invalidation scope. Hierarchy is [Measure] > [Placement] > [Draw] (only the highest one fires).
 *
 * [None] 은 무효화 없음. 생성 시점에만 쓰거나 `observeReads` 로 읽는 값에 사용한다.
 * [None] means no invalidation — for values used only on create, or read via `observeReads`.
 */
enum class InvalidationScope { Measure, Placement, Draw, Semantics, ParentData, None }

/**
 * 이 `Boolean` 파라미터가 `false` 면 modifier 를 적용하지 않는다 (노드 미attach).
 * The modifier is not applied when this `Boolean` parameter is `false` (no node attached).
 *
 * 생성 함수 앞에 `if (!param) return this` 가 삽입된다. 여러 개면 OR 로 묶인다.
 * Prepends `if (!param) return this` to the generated function. Multiple markers are OR-joined.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class SkipWhenFalse

/**
 * [SkipWhenFalse] 의 반대 — 이 `Boolean` 파라미터가 `true` 면 modifier 를 적용하지 않는다.
 * Opposite of [SkipWhenFalse] — the modifier is not applied when this `Boolean` parameter is `true`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class SkipWhenTrue

/**
 * 이 파라미터가 바뀌면 생성된 `update()` 가 노드 필드 갱신 후 `on<Name>Changed()` 를 호출한다.
 * When this parameter changes, the generated `update()` calls `on<Name>Changed()` after updating the node field.
 *
 * 노드는 인자 없이 `Unit` 을 반환하는 `fun on<Name>Changed()` 를 선언해야 한다.
 * The node must declare `fun on<Name>Changed()` taking no arguments and returning `Unit`.
 * 예: `interactionSource` → `fun onInteractionSourceChanged()`.
 * e.g. `interactionSource` → `fun onInteractionSourceChanged()`.
 *
 * 콜백 시점에는 노드 필드가 이미 새 값이다. 코루틴 재구독, effect 재시작 등에 쓴다.
 * The node field already holds the new value when the callback runs. Use it to re-subscribe a coroutine, restart an effect, etc.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class OnChange
