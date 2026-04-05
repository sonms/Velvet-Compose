package com.sonms.wheelpicker.extension

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 페이드 엣지 효과 Modifier.
 * Modifier that applies fading edge effect.
 *
 * @param isVertical 페이드 엣지가 수직 방향인지 여부.
 *                   Whether the fading edge is vertical.
 * @param fraction 페이드 엣지의 농도.
 *                  The fraction of the fading edge.
 * @param enabled 페이드 엣지의 활성화 여부.
 *                 Whether the fading edge is enabled.
 */
internal fun Modifier.fadingEdge(
    isVertical: Boolean,
    fraction: Float,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val maskBrush = if (isVertical) {
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    fraction to Color.Black,
                    (1f - fraction) to Color.Black,
                    1f to Color.Transparent
                )
            } else {
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    fraction to Color.Black,
                    (1f - fraction) to Color.Black,
                    1f to Color.Transparent
                )
            }

            onDrawWithContent {
                drawContent()
                drawRect(brush = maskBrush, blendMode = BlendMode.DstIn)
            }
        }
}