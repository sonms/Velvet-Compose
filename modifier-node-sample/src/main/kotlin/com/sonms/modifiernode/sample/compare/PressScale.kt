package com.sonms.modifiernode.sample.compare

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.InvalidationScope.None
import com.sonms.modifiernode.annotations.ModifierNodeFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// BEFORE — composed { }: InteractionSource 구독 + 애니메이션
// ─────────────────────────────────────────────────────────────────────────────

/**
 * press 되는 동안 축소 스케일 애니메이션.
 *
 * `composed` 가 여기서 해주는 것:
 *  - `collectIsPressedAsState()` — InteractionSource 구독을 composition lifecycle 에 묶음
 *  - `animateFloatAsState` — 애니메이션 상태를 remember
 *  - `interactionSource` 가 바뀌면 `collectIsPressedAsState` 가 알아서 재구독 (remember 키)
 *
 * 대가: equals 불안정, 매 재구성 re-materialize, composition 밖 사용 불가.
 */
fun Modifier.pressScaleComposed(
    interactionSource: InteractionSource,
    pressedScale: Float,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AFTER — Modifier.Node + codegen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * codegen 이 걷어내는 것: Element/equals/hashCode/update/inspector.
 *
 * 손으로 남는 것 — 그리고 이게 §COMPARISON 의 핵심:
 *  - `coroutineScope` 에서 `interactionSource.interactions` 수집 (press 카운팅)
 *  - `Animatable` 을 직접 들고 `launch { animateTo() }`
 *  - `onDetach` 에서 job 정리
 *  - ⚠️ `interactionSource` 파라미터가 바뀌면 재구독이 필요한데, 현재 codegen 의
 *    `update` 는 `node.interactionSource = interactionSource` 대입만 한다.
 *    "파라미터 변경 시 노드 커스텀 로직 실행" 훅이 없다 → 여기서는 수동 처리.
 */
@ModifierNodeFactory(name = "pressScale")
internal class PressScaleNode(
    @Invalidates(Draw) var pressedScale: Float,
    @Invalidates(None) var interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    private val scaleAnim = Animatable(1f)
    private var collectJob: Job? = null
    private var boundSource: InteractionSource? = null

    override fun onAttach() {
        rebind()
    }

    override fun onDetach() {
        collectJob?.cancel()
        collectJob = null
        boundSource = null
    }

    /** codegen 의 update 가 대입만 하므로, 재구독은 draw 진입 시 수동 확인. */
    private fun rebind() {
        if (boundSource === interactionSource) return
        collectJob?.cancel()
        boundSource = interactionSource
        collectJob = coroutineScope.launch {
            val presses = ArrayList<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                }
                val target = if (presses.isNotEmpty()) pressedScale else 1f
                launch { scaleAnim.animateTo(target) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        rebind()
        val s = scaleAnim.value // Animatable.value 는 snapshot 상태 → 프레임마다 자동 redraw
        scale(s, s, center) {
            this@draw.drawContent()
        }
    }
}
