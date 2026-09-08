# composed vs Modifier.Node(+codegen) — before/after

두 케이스로 비교한다. 소스: `src/main/kotlin/.../compare/`.

- **Case A — `accentOverlay`**: CompositionLocal 하나 읽어서 draw. (전형적 `composed` 사용)
- **Case B — `pressScale`**: `InteractionSource` 구독 + 애니메이션. (무거운 `composed` 사용)

손으로 쓴 코드 줄 수만 센다(주석 제외, 대략치). 생성 코드는 0으로 친다.

| | composed | Node(손) | codegen 없이 Node(손) | 생성물 |
|---|---:|---:|---:|---:|
| Case A | ~7 | **~10** | ~55 | ~46 |
| Case B | ~10 | **~35** | ~85 | ~53 |

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
    @Invalidates(None) @OnChange var interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val scaleAnim = Animatable(1f)
    private var collectJob: Job? = null

    override fun onAttach() = subscribe()
    override fun onDetach() { collectJob?.cancel(); collectJob = null }
    fun onInteractionSourceChanged() = subscribe()   // 생성된 update() 가 호출

    private fun subscribe() {
        collectJob?.cancel()
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
        val s = scaleAnim.value        // snapshot 상태 → 프레임마다 자동 redraw
        scale(s, s, center) { this@draw.drawContent() }
    }
}
```

### 판정 B — codegen + `@OnChange` 로 격차 축소

- 손 코드 10 → ~35줄. Element(~50줄) + 재구독 dedup(`boundSource` 필드, `draw()` 의 `rebind()`)
  이 사라졌다. 남은 장황함은 `Animatable` / press 카운팅 — 진짜 노드 행동 코드다.
- `@OnChange var interactionSource` → 생성된 `update()` 가 필드 갱신 후 `onInteractionSourceChanged()`
  를 호출한다. `composed` 의 `remember(key)` 재구독에 대응.
- `Animatable` / press 카운팅 보일러플레이트는 여전히 codegen 범위 밖 (노드 유틸이 있으면 더 줄겠지만 별개).

---

## 종합

| 모디파이어 부류 | codegen 효과 |
|---|---|
| **값(파라미터/CompositionLocal) 읽고 draw/measure 에서 반응** — 커스텀 모디파이어의 다수 | **결정적.** 7→10 vs 7→55. `composed` 를 벗어나게 해줌 |
| **interaction / coroutine / animation** | `@OnChange` 로 재구독 격차 축소(10→35). 남은 장황함(`Animatable`/카운팅)은 진짜 노드 코드 |
| 모든 부류 공통 | 안정 equals·inspector·`@Invalidates` 정합성 체크·ABI 표면 축소·`@SkipWhen*` 가드 |

- **값 읽기 부류**: 결정적. `fadingEdge`(composition-free 체인)는 `composed` 케이스가 아니었고
  raw 노드로 내려서 손해 — `../ARCHITECTURE.md` §7.
- **interaction 부류**: `@OnChange` 이후 근접(값 읽기만큼 결정적이진 않음).
- 파라미터 단위 invalidation 이 실제로 작동하는지(그리고 성능 이득이 왜 좁은지)는
  `../ARCHITECTURE.md` §4·§5.
