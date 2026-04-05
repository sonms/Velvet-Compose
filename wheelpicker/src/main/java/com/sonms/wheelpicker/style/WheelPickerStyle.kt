package com.sonms.wheelpicker.style

import androidx.compose.runtime.Immutable

/**
 * WheelPicker 전체 스타일을 정의합니다.
 * Defines the overall style of WheelPicker.
 *
 * @param selector 선택 영역 스타일 / Selector area style
 * @param fade 페이드 엣지 스타일 / Fade edge style
 * @param transform 3D 변환 효과 스타일 / 3D transform effect style
 */
@Immutable
data class WheelPickerStyle(
    val selector: WheelPickerSelectorStyle,
    val fade: WheelPickerFadeStyle,
    val transform: WheelPickerTransformStyle,
)