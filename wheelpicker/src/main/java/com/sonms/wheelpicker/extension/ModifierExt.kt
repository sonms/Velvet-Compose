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
 * @param color 페이드 엣지의 색상.
 *               The color of the fading edge.
 * @param fraction 페이드 엣지의 농도.
 *                  The fraction of the fading edge.
 * @param enabled 페이드 엣지의 활성화 여부.
 *                 Whether the fading edge is enabled.
 */
internal fun Modifier.fadingEdge(
    isVertical: Boolean,
    color: Color,
    fraction: Float,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val startBrush: Brush
            val endBrush: Brush

            if (isVertical) {
                startBrush = Brush.verticalGradient(
                    colors = listOf(color, Color.Transparent),
                    startY = 0f,
                    endY = size.height * fraction,
                )
                endBrush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, color),
                    startY = size.height * (1f - fraction),
                    endY = size.height,
                )
            } else {
                startBrush = Brush.horizontalGradient(
                    colors = listOf(color, Color.Transparent),
                    startX = 0f,
                    endX = size.width * fraction,
                )
                endBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color),
                    startX = size.width * (1f - fraction),
                    endX = size.width,
                )
            }

            onDrawWithContent {
                drawContent()
                drawRect(brush = startBrush, blendMode = BlendMode.DstIn)
                drawRect(brush = endBrush, blendMode = BlendMode.DstIn)
            }
        }
}