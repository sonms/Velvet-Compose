# composed vs Modifier.Node(+codegen) — before/after

두 케이스로 비교한다. 소스: `src/main/kotlin/.../compare/`.

- **Case A — `accentOverlay`**: CompositionLocal 하나 읽어서 draw. (전형적 `composed` 사용)
- **Case B — `pressScale`**: `InteractionSource` 구독 + 애니메이션. (무거운 `composed` 사용)

손으로 쓴 코드 줄 수만 센다(주석 제외, 대략치). 생성 코드는 0으로 친다.

| | composed | Node(손) | codegen 없이 Node(손) | 생성물 |
|---|---:|---:|---:|---:|
| Case A | ~7 | **~10** | ~55 | ~46 |
| Case B | ~10 | **~40** | ~85 | ~53 |

---

## Case A — `accentOverlay`

### BEFORE

```kotlin
fun Modifier.accentOverlayComposed(alpha: Float): Modifier = composed {
    val color = LocalAccentColor.current
    drawWithContent {
        drawContent()
        drawRect(color.copy(alpha = alpha))
    }
}
```

숨은 비용: 반환 Modifier `equals` 불안정 → 부모 재구성 시 하위 재구성, `composed` 람다 매번
re-materialize, composition 밖 사용 불가, layout inspector 에 이름 안 뜸.

### AFTER

```kotlin
@ModifierNodeFactory(name = "accentOverlay")
internal class AccentOverlayNode(
    @Invalidates(Draw) var alpha: Float,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        val color = currentValueOf(LocalAccentColor)   // draw 에서 읽으면 관찰됨 → 로컬 변경 시 자동 redraw
        drawContent()
        drawRect(color.copy(alpha = alpha))
    }
}
```

생성: `fun Modifier.accentOverlay(alpha)` + `AccentOverlayElement`(create/update/equals/hashCode/inspector).

### 판정 A — **명확한 이득**

- 손 코드 7 → 10줄. 늘어난 3줄은 전부 "실제 정보"(무슨 노드인지 선언 + `var` 선언).
- codegen 없으면 7 → ~55줄. 이 55줄이 싫어서 다들 `composed` 에 머문다. **codegen 이 정확히 그 벽을 없앤다.**
- 덤: 안정 equals, inspector 이름, composition 밖 사용 가능, `@Invalidates(Draw)` ↔ `DrawModifierNode` 정합성 컴파일 체크.

---

## Case B — `pressScale`

### BEFORE

```kotlin
fun Modifier.pressScaleComposed(
    interactionSource: InteractionSource,
    pressedScale: Float,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, label = "pressScale")
    graphicsLayer { scaleX = scale; scaleY = scale }
}
```

`composed` 가 공짜로 해주는 것: 구독 lifecycle, 애니메이션 상태 remember,
`interactionSource` 키가 바뀌면 재구독.

### AFTER

```kotlin
@ModifierNodeFactory(name = "pressScale")
internal class PressScaleNode(
    @Invalidates(Draw) var pressedScale: Float,
    @Invalidates(None) var interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val scaleAnim = Animatable(1f)
    private var collectJob: Job? = null
    private var boundSource: InteractionSource? = null

    override fun onAttach() = rebind()
    override fun onDetach() { collectJob?.cancel(); collectJob = null; boundSource = null }

    private fun rebind() {
        if (boundSource === interactionSource) return
        collectJob?.cancel()
        boundSource = interactionSource
        collectJob = coroutineScope.launch {
            val presses = ArrayList<PressInteraction.Press>()
            interactionSource.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press   -> presses.add(i)
                    is PressInteraction.Release -> presses.remove(i.press)
                    is PressInteraction.Cancel  -> presses.remove(i.press)
                }
                launch { scaleAnim.animateTo(if (presses.isNotEmpty()) pressedScale else 1f) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        rebind()                       // ← codegen update 가 대입만 하므로 수동 재구독 확인
        val s = scaleAnim.value        // snapshot 상태 → 프레임마다 자동 redraw
        scale(s, s, center) { this@draw.drawContent() }
    }
}
```

### 판정 B — **codegen 만으로는 부족**

- 손 코드 10 → 40줄. codegen 이 Element(~50줄)를 없앴지만, interaction+animation 의
  **노드 행동 코드 자체가 `composed` 보다 훨씬 장황**하다.
- **새 한계 발견:** `interactionSource` 파라미터가 바뀔 때 재구독하려면 훅이 필요한데,
  codegen 의 `update` 는 `node.x = x` 대입만 한다. 여기서는 `draw()` 진입 시 `rebind()`
  수동 호출로 우회 — 지저분하다. → `update` 후 `node.onUpdated()` 를 부르거나,
  `@Invalidates` 에 "재바인드" 개념(예: `@OnChange`)이 필요.
- `Animatable` / press 카운팅 보일러플레이트는 codegen 범위 밖. 별도 노드 유틸이 있어야 함.

---

## 종합

| 모디파이어 부류 | codegen 효과 |
|---|---|
| **값(파라미터/CompositionLocal) 읽고 draw/measure 에서 반응** — 커스텀 모디파이어의 다수 | **결정적.** 7→10 vs 7→55. `composed` 를 벗어나게 해줌 |
| **interaction / coroutine / animation** | Element 제거는 도움되지만 균형을 못 뒤집음. `onUpdate` 훅 + 노드 유틸 필요 |
| 모든 부류 공통 | 안정 equals·inspector·`@Invalidates` 정합성 체크·(예정) ABI 표면 축소 |

**결정 게이트 조건 1** ("재작성이 원본보다 낫거나 동등"): `fadingEdge`(§ARCHITECTURE 9)로는 불충족이었으나
그건 애초에 `composed` 케이스가 아니었음. **Case A 로 재평가 시 충족.** Case B 는 codegen 의
경계를 명확히 보여줌 — 라이브러리화하면 `onUpdate` 훅을 v1.1 스코프에 넣어야 한다.
