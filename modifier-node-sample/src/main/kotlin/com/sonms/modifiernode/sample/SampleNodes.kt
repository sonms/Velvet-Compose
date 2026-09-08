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
 * 예제 1 — draw 전용 노드.
 * 생성물: `DebugTintElement`, `fun Modifier.debugTint(color: Color): Modifier`
 */
@ModifierNodeFactory(name = "debugTint")
internal class DebugTintNode(
    @Invalidates(Draw) var color: Color,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(color)
    }
}

/**
 * 예제 2 — draw + layout 노드. update 의 remeasure/redraw 분기를 검증하기 위한 케이스.
 * 생성물: `FixedSquareElement`, `fun Modifier.fixedSquare(side: Dp, overlay: Color): Modifier`
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
