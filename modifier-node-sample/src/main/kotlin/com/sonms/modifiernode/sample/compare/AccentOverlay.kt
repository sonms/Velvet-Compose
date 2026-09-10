package com.sonms.modifiernode.sample.compare

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.ModifierNodeFactory

val LocalAccentColor = compositionLocalOf { Color.Magenta }

/** BEFORE — `composed { }` 로 CompositionLocal 을 읽는다. `COMPARISON.md` Case A 참고. */
fun Modifier.accentOverlayComposed(alpha: Float): Modifier = composed {
    val color = LocalAccentColor.current
    drawWithContent {
        drawContent()
        drawRect(color.copy(alpha = alpha))
    }
}

/** AFTER — 같은 효과를 `Modifier.Node` 로. `draw()` 안의 `currentValueOf` 는 관찰되므로 로컬 변경 시 자동 redraw. */
@ModifierNodeFactory(name = "accentOverlay")
internal class AccentOverlayNode(
    @Invalidates(Draw) var alpha: Float,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false

    override fun ContentDrawScope.draw() {
        val color = currentValueOf(LocalAccentColor)
        drawContent()
        drawRect(color.copy(alpha = alpha))
    }
}
