package com.sonms.wheelpicker.style

import androidx.compose.runtime.Immutable

/**
 * 페이드 엣지 효과의 스타일을 정의합니다.
 * Defines the style of the fading edge effect.
 *
 * @param fraction 페이드 영역 비율 (0f ~ 0.5f) / Fade area fraction (0f ~ 0.5f)
 * @param enabled 페이드 효과 활성화 여부 / Whether the fade effect is enabled
 */
@Immutable
data class WheelPickerFadeStyle(
    val fraction: Float,
    val enabled: Boolean,
)