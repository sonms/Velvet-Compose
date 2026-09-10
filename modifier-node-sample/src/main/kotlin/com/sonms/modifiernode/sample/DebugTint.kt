package com.sonms.modifiernode.sample

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.ModifierNodeFactory

/**
 * 컨텐츠 위에 색을 덧칠하는 draw 전용 노드. 가장 단순한 생성 예.
 * Draw-only node that tints the content — the simplest codegen example.
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
