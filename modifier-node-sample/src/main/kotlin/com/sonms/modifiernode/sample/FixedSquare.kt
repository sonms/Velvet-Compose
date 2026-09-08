package com.sonms.modifiernode.sample

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
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
 * 정사각형 크기로 강제하고 그 위에 색을 덧칠하는 draw + layout 노드.
 * `side` 변경은 remeasure, `overlay` 변경은 redraw 만 — 생성된 `update()` 의 계층 접기 예.
 *
 * Draw + layout node that forces a square size and tints over it.
 * Changing `side` remeasures; changing `overlay` only redraws — shows the generated `update()` folding.
 */
@ModifierNodeFactory(name = "fixedSquare")
internal class FixedSquareNode(
    @Invalidates(Measure) var side: Dp,
    @Invalidates(Draw) var overlay: Color,
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val px = side.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(px, px))
        return layout(px, px) { placeable.place(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(overlay)
    }
}
