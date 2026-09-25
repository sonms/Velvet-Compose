package com.sonms.wheelpicker.style

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * 선택 영역의 스타일을 정의합니다.
 * Defines the style of the selected item area.
 *
 * @param background 선택 영역 배경 색상 / Background color of the selected area
 * @param shape 선택 영역 모양 / Shape of the selected area
 *              stable한 구현체 사용을 권장합니다 (예: RoundedCornerShape)
 *              It is recommended to use stable implementations (e.g. RoundedCornerShape)
 * @param dividerColor 선택 영역 위아래 구분선 색상 / Divider color above and below the selected area
 * @param dividerThickness 구분선 두께 / Divider thickness
 * @param showDivider 구분선 표시 여부 / Whether to show dividers
 */
@Immutable
data class WheelPickerSelectorStyle(
    val background: Color,
    val shape: Shape,
    val dividerColor: Color,
    val dividerThickness: Dp,
    val showDivider: Boolean,
)