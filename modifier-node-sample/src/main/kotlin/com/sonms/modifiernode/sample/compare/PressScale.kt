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

/** BEFORE — `composed { }` 로 InteractionSource 구독 + 애니메이션. `COMPARISON.md` Case B 참고. */
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

/**
 * AFTER — 같은 효과를 `Modifier.Node` 로.
 *
 * `interactionSource` 가 바뀌면 재구독이 필요한데 생성된 `update()` 는 필드 대입만 한다 →
 * [draw] 진입 시 [rebind] 로 수동 확인 (§10 Case B, `onUpdate` 훅 도입 전까지의 우회).
 */
@ModifierNodeFactory(name = "pressScale")
internal class PressScaleNode(
    @Invalidates(Draw) var pressedScale: Float,
    @Invalidates(None) var interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

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
        val s = scaleAnim.value // snapshot 상태 → 애니메이션 프레임마다 자동 redraw
        scale(s, s, center) {
            this@draw.drawContent()
        }
    }
}
