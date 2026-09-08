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

/**
 * Dogfooding — `wheelpicker` 의 `Modifier.fadingEdge` 를 `Modifier.Node` 로 재작성.
 *
 * 원본은 `graphicsLayer { compositingStrategy = Offscreen }.drawWithCache { ... }` 조합이었다.
 * Node 판에서는 오프스크린 버퍼를 직접 [GraphicsLayer] 로 관리한다:
 *  - [onAttach] / [onDetach] 에서 레이어 생성/반납 (composition 밖 lifecycle)
 *  - [draw] 에서 컨텐츠를 레이어에 record 하고 같은 버퍼에 DstIn 마스크를 얹는다
 *
 * 결정 게이트 메모:
 *  - `enabled=false` 는 원본에서 "modifier 미적용(early return)" 이었다. 현재 codegen 은
 *    `this.then(Element(...))` 를 무조건 생성하므로 **조건부 적용을 표현할 수 없다**.
 *    여기서는 노드가 파라미터를 보고 no-op 하는 방식으로 우회했고, 그 대가로 disabled
 *    상태에서도 노드/레이어가 attach 된다.
 *  - 그리기 로직(브러시 계산, record, DstIn, drawLayer)은 전부 손으로 작성했다.
 *    codegen 이 걷어낸 건 Element/equals/hashCode/update/inspector 뿐이다.
 */
@ModifierNodeFactory(name = "fadingEdge")
internal class FadingEdgeNode(
    @Invalidates(Draw) var isVertical: Boolean,
    @Invalidates(Draw) var fraction: Float,
    @Invalidates(Draw) var enabled: Boolean,
) : Modifier.Node(), DrawModifierNode {

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
        if (!enabled || gl == null) {
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
