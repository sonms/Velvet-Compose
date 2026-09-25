package com.sonms.velvetcompose.modifiernode

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.InvalidationScope.Measure
import com.sonms.modifiernode.annotations.ModifierNodeFactory

/**
 * 컨텐츠를 정사각형 크기로 강제하고 테두리를 그리는 layout + draw 노드.
 * Layout + draw node that clamps content to a square and strokes a border.
 *
 * 계층 접힘 시연 / demonstrates update() folding:
 *  - `side` 변경  → `@Invalidates(Measure)` → `invalidateMeasurement()` (remeasure, redraw 포함)
 *  - `border` 변경 → `@Invalidates(Draw)`   → `invalidateDraw()` 만
 *  - 둘 다 바뀌면 상위 하나(Measure)만 호출된다.
 */
@ModifierNodeFactory(name = "squareThumbnail")
internal class SquareThumbnailNode(
    @Invalidates(Measure) var side: Dp,
    @Invalidates(Draw) var border: Color,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val px = side.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(px, px))
        return layout(px, px) { placeable.place(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(color = border, style = Stroke(width = 4f))
    }
}
