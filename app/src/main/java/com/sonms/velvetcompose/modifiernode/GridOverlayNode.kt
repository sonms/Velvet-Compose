package com.sonms.velvetcompose.modifiernode

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.InvalidationScope.None
import com.sonms.modifiernode.annotations.ModifierNodeFactory
import com.sonms.modifiernode.annotations.SkipWhenFalse

/**
 * 컨텐츠 위에 일정 간격 격자선을 덧그리는 draw 전용 노드.
 * Draw-only node that paints a grid over the content.
 *
 * 코드젠 산출물 / what codegen produces (see build/generated/ksp/.../GridOverlayElement.kt):
 *  - `fun Modifier.gridOverlay(visible: Boolean, step: Dp, lineColor: Color): Modifier`
 *  - `GridOverlayElement` — 안정 타입 파라미터 기반 equals/hashCode + 계층 접힌 update()
 *
 * `visible == false` 면 노드를 아예 attach 하지 않는다 (`@SkipWhenFalse` → 생성 함수 앞 `if (!visible) return this`).
 * `step` / `lineColor` 변경은 redraw 만 유발한다 (`@Invalidates(Draw)`).
 */
@ModifierNodeFactory(name = "gridOverlay")
internal class GridOverlayNode(
    @SkipWhenFalse @Invalidates(None) var visible: Boolean,
    @Invalidates(Draw) var step: Dp,
    @Invalidates(Draw) var lineColor: Color,
) : Modifier.Node(), DrawModifierNode {

    // @Invalidates 를 쓰려면 자동 무효화를 꺼야 한다 (§4).
    override val shouldAutoInvalidate: Boolean get() = false

    override fun ContentDrawScope.draw() {
        drawContent()
        val stepPx = step.toPx().coerceAtLeast(1f)

        var x = stepPx
        while (x < size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height))
            x += stepPx
        }
        var y = stepPx
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y))
            y += stepPx
        }
    }
}
