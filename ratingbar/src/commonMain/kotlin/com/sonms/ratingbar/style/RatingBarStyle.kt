package com.sonms.ratingbar.style

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * RatingBar의 스타일을 정의합니다.
 * Defines the style of RatingBar.
 *
 * @param filledColor 채워진 아이템 색상 / Color of filled items
 * @param emptyColor 빈 아이템 색상 / Color of empty items
 * @param itemSize 아이템 크기 / Size of each item
 * @param itemSpacing 아이템 간격 / Spacing between items
 * @param animationSpec 선택 시 애니메이션 스펙. null이면 애니메이션 비활성화
 *                      Animation spec on selection. null to disable animation
 * @param hapticFeedbackEnabled 햅틱 피드백 활성화 여부 / Whether haptic feedback is enabled
 */
@Immutable
data class RatingBarStyle(
    val filledColor: Color,
    val emptyColor: Color,
    val itemSize: Dp,
    val itemSpacing: Dp,
    val animationSpec: AnimationSpec<Float>?,
    val hapticFeedbackEnabled: Boolean,
)