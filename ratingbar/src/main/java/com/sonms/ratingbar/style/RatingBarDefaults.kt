package com.sonms.ratingbar.style

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RatingBar의 기본 스타일 값을 제공합니다.
 * Provides default style values for RatingBar.
 */
object RatingBarDefaults {

    /**
     * 기본 RatingBarStyle을 반환합니다.
     * Returns the default RatingBarStyle.
     *
     * @param filledColor 채워진 아이템 색상 / Color of filled items
     * @param emptyColor 빈 아이템 색상 / Color of empty items
     * @param itemSize 아이템 크기 / Size of each item
     * @param itemSpacing 아이템 간격 / Spacing between items
     * @param animationSpec 선택 시 애니메이션 스펙. null이면 비활성화
     *                      Animation spec on selection. null to disable
     * @param hapticFeedbackEnabled 햅틱 피드백 활성화 여부 / Whether haptic feedback is enabled
     */
    @Composable
    fun style(
        filledColor: Color = MaterialTheme.colorScheme.primary,
        emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        itemSize: Dp = 32.dp,
        itemSpacing: Dp = 4.dp,
        animationSpec: AnimationSpec<Float>? = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        hapticFeedbackEnabled: Boolean = true,
    ): RatingBarStyle = RatingBarStyle(
        filledColor = filledColor,
        emptyColor = emptyColor,
        itemSize = itemSize,
        itemSpacing = itemSpacing,
        animationSpec = animationSpec,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
    )
}