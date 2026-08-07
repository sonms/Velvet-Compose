package com.sonms.ratingbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.sonms.ratingbar.model.StepSize
import com.sonms.ratingbar.style.RatingBarDefaults
import com.sonms.ratingbar.style.RatingBarStyle

/**
 * 기본 별 아이콘을 사용하는 RatingBar.
 * RatingBar using default star icons.
 *
 * @param rating 현재 별점 값 (0f ~ maxRating) / Current rating value (0f ~ maxRating)
 * @param modifier Modifier
 * @param maxRating 최대 별 개수 / Maximum number of stars
 * @param stepSize 별점 단계 크기 / Step size for rating
 * @param style 스타일 설정 / Style configuration
 * @param onRatingChanged 별점 변경 콜백. null이면 읽기 전용
 *                        Callback when rating changes. null for read-only
 */
@Composable
fun RatingBar(
    rating: Float,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    stepSize: StepSize = StepSize.HALF,
    style: RatingBarStyle = RatingBarDefaults.style(),
    onRatingChanged: ((Float) -> Unit)? = null,
) {
    RatingBarImpl(
        rating = rating,
        modifier = modifier,
        maxRating = maxRating,
        stepSize = stepSize,
        style = style,
        onRatingChanged = onRatingChanged,
        itemContent = null,
    )
}

/**
 * 커스텀 아이콘을 사용하는 RatingBar (Slot API).
 * RatingBar with custom icon slot API.
 *
 * @param rating 현재 별점 값 (0f ~ maxRating) / Current rating value (0f ~ maxRating)
 * @param modifier Modifier
 * @param maxRating 최대 아이템 개수 / Maximum number of items
 * @param stepSize 별점 단계 크기 / Step size for rating
 * @param style 스타일 설정 / Style configuration
 * @param onRatingChanged 별점 변경 콜백. null이면 읽기 전용
 *                        Callback when rating changes. null for read-only
 * @param itemContent 아이템 UI 슬롯. index와 fraction(0f~1f)을 받음
 *                    Item UI slot. Receives index and fraction(0f~1f)
 */
@Composable
fun RatingBar(
    rating: Float,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    stepSize: StepSize = StepSize.HALF,
    style: RatingBarStyle = RatingBarDefaults.style(),
    onRatingChanged: ((Float) -> Unit)? = null,
    itemContent: @Composable (index: Int, fraction: Float) -> Unit,
) {
    RatingBarImpl(
        rating = rating,
        modifier = modifier,
        maxRating = maxRating,
        stepSize = stepSize,
        style = style,
        onRatingChanged = onRatingChanged,
        itemContent = itemContent,
    )
}

/**
 * RatingBar 내부 공통 구현.
 * Internal common implementation of RatingBar.
 */
@Composable
private fun RatingBarImpl(
    rating: Float,
    maxRating: Int,
    stepSize: StepSize,
    style: RatingBarStyle,
    onRatingChanged: ((Float) -> Unit)?,
    modifier: Modifier,
    itemContent: (@Composable (index: Int, fraction: Float) -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // 애니메이션 적용된 rating 값
    val animatedRating by if (style.animationSpec != null) {
        animateFloatAsState(
            targetValue = rating.coerceIn(0f, maxRating.toFloat()),
            animationSpec = style.animationSpec,
            label = "rating",
        )
    } else {
        remember(rating) { mutableFloatStateOf(rating.coerceIn(0f, maxRating.toFloat())) }
    }

    // 터치 위치로 rating 계산 (아이템 사이 spacing을 제외한 실제 아이템 폭 기준)
    fun calculateRating(x: Float, itemSizePx: Float, itemSpacingPx: Float): Float {
        val segment = itemSizePx + itemSpacingPx
        val totalWidth = maxRating * itemSizePx + (maxRating - 1) * itemSpacingPx
        val effectiveX = (if (isRtl) totalWidth - x else x).coerceIn(0f, totalWidth)

        val index = (effectiveX / segment).toInt().coerceIn(0, maxRating - 1)
        val localX = (effectiveX - index * segment).coerceAtLeast(0f)
        val fractionInItem = (localX / itemSizePx).coerceIn(0f, 1f)

        val rawRating = (index + fractionInItem).coerceIn(0f, maxRating.toFloat())
        return (rawRating / stepSize.value).let {
            kotlin.math.round(it) * stepSize.value
        }.coerceIn(stepSize.value, maxRating.toFloat())
    }

    var lastHapticRating by remember { mutableFloatStateOf(rating) }

    val density = LocalDensity.current
    val itemSizePx = with(density) { style.itemSize.toPx() }
    val itemSpacingPx = with(density) { style.itemSpacing.toPx() }

    val interactionModifier = if (onRatingChanged != null) {
        Modifier
            .pointerInput(stepSize, maxRating, itemSizePx, itemSpacingPx) {
                detectTapGestures { offset ->
                    val newRating = calculateRating(offset.x, itemSizePx, itemSpacingPx)
                    if (style.hapticFeedbackEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onRatingChanged(newRating)
                    lastHapticRating = newRating
                }
            }
            .pointerInput(stepSize, maxRating, itemSizePx, itemSpacingPx) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val newRating = calculateRating(change.position.x, itemSizePx, itemSpacingPx)
                    if (style.hapticFeedbackEnabled && newRating != lastHapticRating) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastHapticRating = newRating
                    }
                    onRatingChanged(newRating)
                }
            }
    } else Modifier

    Row(
        modifier = modifier.then(interactionModifier),
        horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxRating) { index ->
            val fraction = when {
                animatedRating >= index + 1f -> 1f
                animatedRating > index -> animatedRating - index
                else -> 0f
            }

            if (itemContent != null) {
                Box(
                    modifier = Modifier
                        .size(style.itemSize)
                ) {
                    itemContent(index, fraction)
                }
            } else {
                // 기본 별 아이콘
                StarItem(
                    fraction = fraction,
                    style = style,
                )
            }
        }
    }
}

/**
 * 기본 별 아이템.
 * Default star item with fill fraction support.
 *
 * @param fraction 채움 비율 (0f ~ 1f) / Fill fraction (0f ~ 1f)
 * @param style 스타일 / Style
 */
@Composable
private fun StarItem(
    fraction: Float,
    style: RatingBarStyle,
) {
    Box(
        modifier = Modifier
            .size(style.itemSize)
            .drawWithCache {
                val path = createStarPath(size)
                onDrawBehind {
                    // 빈 별 (배경)
                    clipPath(path) {
                        drawRect(color = style.emptyColor)
                    }
                    // 채워진 별 (fraction만큼)
                    clipRect(right = size.width * fraction) {
                        clipPath(path) {
                            drawRect(color = style.filledColor)
                        }
                    }
                }
            }
    )
}

/**
 * 별 모양 Path를 생성합니다.
 * Creates a star-shaped Path.
 */
private fun createStarPath(size: androidx.compose.ui.geometry.Size): Path {
    val path = Path()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outerRadius = size.width / 2f
    val innerRadius = outerRadius * 0.4f
    val numPoints = 5
    val angleOffset = -Math.PI / 2

    for (i in 0 until numPoints * 2) {
        val angle = angleOffset + i * Math.PI / numPoints
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val x = cx + (radius * kotlin.math.cos(angle)).toFloat()
        val y = cy + (radius * kotlin.math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}