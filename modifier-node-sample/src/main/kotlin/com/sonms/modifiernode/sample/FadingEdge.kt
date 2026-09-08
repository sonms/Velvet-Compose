package com.sonms.modifiernode.sample

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.requireGraphicsContext
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.ModifierNodeFactory
import com.sonms.modifiernode.annotations.SkipWhenFalse

/**
 * 가장자리를 서서히 투명하게 만드는 페이드 엣지 효과.
 * Fading edge effect that fades the edges to transparent.
 *
 * 오프스크린 [GraphicsLayer] 에 컨텐츠를 그린 뒤 같은 버퍼에 `DstIn` 그라디언트 마스크를 얹는다.
 * Draws the content into an offscreen [GraphicsLayer], then applies a `DstIn` gradient mask to it.
 */
@ModifierNodeFactory(name = "fadingEdge")
internal class FadingEdgeNode(
    @Invalidates(Draw) var isVertical: Boolean,
    @Invalidates(Draw) var fraction: Float,
    @SkipWhenFalse var enabled: Boolean,
) : Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

    private var layer: GraphicsLayer? = null

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
    }

    override fun ContentDrawScope.draw() {
        val gl = layer
        if (gl == null) {
            drawContent()
            return
        }

        val f = fraction
        val maskBrush = if (isVertical) {
            Brush.verticalGradient(
                0f to Color.Transparent,
                f to Color.Black,
                (1f - f) to Color.Black,
                1f to Color.Transparent,
            )
        } else {
            Brush.horizontalGradient(
                0f to Color.Transparent,
                f to Color.Black,
                (1f - f) to Color.Black,
                1f to Color.Transparent,
            )
        }

        gl.record {
            this@draw.drawContent()
            drawRect(brush = maskBrush, blendMode = BlendMode.DstIn)
        }
        drawLayer(gl)
    }
}
