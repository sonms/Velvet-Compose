# modifier-node-codegen

`@ModifierNodeFactory` 를 붙인 `Modifier.Node` 서브클래스로부터 `ModifierNodeElement` 와
`Modifier` 확장 함수를 KSP 로 생성한다. `composed { }` 를 벗어나 `Modifier.Node` 를 쓸 때
반복되는 보일러플레이트(Element / `equals` / `hashCode` / `update` / `inspectableProperties`)를 없앤다.

> **포지셔닝.** Velvet-Compose 내부 인프라 + `Modifier.Node` 학습 목적의 도구다.
> `Modifier.Node` 를 직접 작성하는 사람(라이브러리 저자 등)만 대상이고, 니치가 좁다.
> 1인 유지보수 · SLA 없음 · API 호환성 보장 없음. 설계 배경과 판단 근거는
> [`ARCHITECTURE.md`](../ARCHITECTURE.md), `composed` 와의 비교는
> [`COMPARISON.md`](../modifier-node-sample/COMPARISON.md) 참고.

## Setup

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(project(":modifier-node-annotations"))
    ksp(project(":modifier-node-processor"))
}
```

배포 좌표는 `io.github.sonms:modifier-node-annotations` / `io.github.sonms:modifier-node-processor` 로
잡혀 있다(**아직 Maven Central 에 올라가지 않았다**). 올라간 뒤에는 이렇게 쓴다:

```kotlin
dependencies {
    // 어노테이션은 전부 SOURCE retention 이라 compileOnly 로 충분하다.
    compileOnly("io.github.sonms:modifier-node-annotations:0.0.1")
    ksp("io.github.sonms:modifier-node-processor:0.0.1")
}
```

## 사용법

`Modifier.Node` 서브클래스를 평소대로 작성하고 `@ModifierNodeFactory` 를 붙인다.
파라미터는 **주 생성자의 `var` 프로퍼티 파라미터**여야 한다.

```kotlin
@ModifierNodeFactory(name = "debugTint")
internal class DebugTintNode(
    @Invalidates(Draw) var color: Color,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false   // @Invalidates 를 쓰면 필수

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(color)
    }
}
```

생성물 (`DebugTintElement.kt`):

```kotlin
public fun Modifier.debugTint(color: Color): Modifier = this.then(DebugTintElement(color))

internal class DebugTintElement(
    private val color: Color,
) : ModifierNodeElement<DebugTintNode>() {
    override fun create() = DebugTintNode(color)
    override fun update(node: DebugTintNode) {
        if (node.color != color) { node.color = color; node.invalidateDraw() }
    }
    override fun InspectorInfo.inspectableProperties() { name = "debugTint"; properties["color"] = color }
    override fun equals(other: Any?): Boolean { /* per-field */ }
    override fun hashCode(): Int { /* per-field */ }
}
```

Node 클래스의 그리기/측정/포인터 **행동 코드만** 직접 쓰면 된다.

## 어노테이션

| 어노테이션 | 대상 | 하는 일 |
|---|---|---|
| `@ModifierNodeFactory(name, visibility)` | Node 클래스 | 생성 트리거. `name` 비우면 클래스명(`-Node`)에서 유도. `visibility` 는 생성 함수의 가시성(기본 `Public`), Element 는 항상 `internal` |
| `@Invalidates(vararg InvalidationScope)` | `var` 파라미터 | 이 파라미터가 바뀔 때 무효화할 범위. 미지정 시 노드가 구현한 모든 범위. `None` 은 무효화 없음 |
| `@SkipWhenFalse` / `@SkipWhenTrue` | `Boolean` 파라미터 | 조건 불충족 시 생성 함수가 `return this` — 노드 미attach. 여럿이면 OR |
| `@OnChange` | `var` 파라미터 | 값이 바뀌면 생성 `update()` 가 노드의 `fun on<Name>Changed()` 호출(재구독·effect 재시작용). 해당 함수 선언 필수 |

### `shouldAutoInvalidate = false`

Compose 는 `update()` 직후 노드의 모든 capability 를 자동 무효화한다. `@Invalidates` 의
파라미터 단위 제어가 실제로 먹으려면 Node 가 다음을 선언해야 한다(`@Invalidates` 사용 시 KSP 가 강제):

```kotlin
override val shouldAutoInvalidate: Boolean get() = false
```

지원되는 문서화된 API 이며, Compose 자체 `graphicsLayer` / `paint` 모디파이어가 같은 패턴을 쓴다.

## 성능에 대한 현실적 기대

`Modifier.Node` 성능의 대부분은 **equals 스킵**(파라미터 불변 시 `update()` 자체가 안 불림)이고,
이건 `@Invalidates` 없이도 codegen 이 정확한 `equals` 를 보장해서 얻는다. `@Invalidates` 의
파라미터 단위 invalidation 은 micro-optimization 이다 — Compose 조차 극소수 모디파이어에만 적용했고,
"파라미터가 자주 바뀌는데 그 변경이 draw 만 요구하는" 좁은 경우에만 이득이 있다.

즉 이 도구의 실질 가치는 **보일러플레이트 제거 + equals 안정성 보장 + ABI 표면 축소**다.

## 한계

- 생성 함수에 default argument 를 재현하지 못한다 (KSP 가 기본값 표현식을 못 읽음). 호출부에서 전 인자 전달.
- 지원 노드 인터페이스는 `DrawModifierNode`, `LayoutModifierNode` 뿐.
- 제네릭 Node, 다중 타입 파라미터 미검증.
- `equals` 는 참조형 파라미터의 `equals` 안정성에 의존한다. 함수 타입 파라미터는 경고하지만
  그 밖의 불안정 타입은 사용자 책임.
- 프로세서 자체 단위 테스트가 없다 (`:modifier-node-sample` 생성물 + Robolectric 로 간접 검증).
