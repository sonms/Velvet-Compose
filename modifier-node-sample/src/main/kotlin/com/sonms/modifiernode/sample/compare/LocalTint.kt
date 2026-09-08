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

/** 비교용 CompositionLocal. */
val LocalAccentColor = compositionLocalOf { Color.Magenta }

// ─────────────────────────────────────────────────────────────────────────────
// BEFORE — composed { } 로 CompositionLocal 을 읽는 전형적 패턴
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `LocalAccentColor` 를 읽어 반투명 오버레이를 얹는다.
 *
 * 문제:
 *  - 반환 Modifier 가 `equals`/`hashCode` 미구현 → 부모가 재구성될 때마다 modifier 체인
 *    비교 실패 → 하위 재구성/무효화
 *  - `composed` 람다가 매 재구성마다 re-materialize
 *  - composition 밖에서 사용 불가
 */
fun Modifier.accentOverlayComposed(alpha: Float): Modifier = composed {
    val color = LocalAccentColor.current
    drawWithContent {
        drawContent()
        drawRect(color.copy(alpha = alpha))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AFTER — Modifier.Node + codegen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 손으로 쓰는 건 노드 행동 코드뿐. `currentValueOf(LocalAccentColor)` 를 `draw()` 에서
 * 읽으면 snapshot 관찰 대상이라 로컬 값이 바뀌면 자동 redraw.
 *
 * 생성물(`AccentOverlayElement.kt`):
 *  - `fun Modifier.accentOverlay(alpha: Float): Modifier`
 *  - `AccentOverlayElement` : create/update/equals/hashCode/inspectableProperties
 */
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
