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
import com.sonms.modifiernode.annotations.OnChange
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
 * AFTER — 같은 효과를 `Modifier.Node` 로. `interactionSource` 는 `@OnChange` — 바뀌면
 * 생성된 `update()` 가 [onInteractionSourceChanged] 를 불러 재구독한다.
 */
@ModifierNodeFactory(name = "pressScale")
internal class PressScaleNode(
    @Invalidates(Draw) var pressedScale: Float,
    @Invalidates(None) @OnChange var interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

    private val scaleAnim = Animatable(1f)
    private var collectJob: Job? = null

    override fun onAttach() = subscribe()

    override fun onDetach() {
        collectJob?.cancel()
        collectJob = null
    }

    fun onInteractionSourceChanged() = subscribe()

    private fun subscribe() {
        collectJob?.cancel()
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
        val s = scaleAnim.value // snapshot 상태 → 애니메이션 프레임마다 자동 redraw
        scale(s, s, center) {
            this@draw.drawContent()
        }
    }
}
