package com.sonms.wheelpicker.style

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WheelPicker의 기본 스타일 값을 제공합니다.
 * Provides default style values for WheelPicker.
 */
object WheelPickerDefaults {
    /**
     * 기본 WheelPickerStyle을 반환합니다.
     * Returns the default WheelPickerStyle.
     *
     * @param selector 선택 영역 스타일 / Selector style
     * @param fade 페이드 엣지 스타일 / Fade style
     * @param transform 3D 변환 스타일 / Transform style
     */
    @Composable
    fun style(
        selector: WheelPickerSelectorStyle = selectorStyle(),
        fade: WheelPickerFadeStyle = fadeStyle(),
        transform: WheelPickerTransformStyle = transformStyle(),
    ): WheelPickerStyle = WheelPickerStyle(
        selector = selector,
        fade = fade,
        transform = transform,
    )

    /**
     * 기본 선택 영역 스타일을 반환합니다.
     * Returns the default selector style.
     *
     * @param background 선택 영역 배경 색상 / Background color
     * @param shape 선택 영역 모양 / Shape
     * @param dividerColor 구분선 색상 / Divider color
     * @param dividerThickness 구분선 두께 / Divider thickness
     * @param showDivider 구분선 표시 여부 / Whether to show divider
     */
    @Composable
    fun selectorStyle(
        background: Color = MaterialTheme.colorScheme.surfaceVariant,
        shape: Shape = RoundedCornerShape(8.dp),
        dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
        dividerThickness: Dp = 1.dp,
        showDivider: Boolean = true,
    ): WheelPickerSelectorStyle = WheelPickerSelectorStyle(
        background = background,
        shape = shape,
        dividerColor = dividerColor,
        dividerThickness = dividerThickness,
        showDivider = showDivider,
    )

    /**
     * 기본 페이드 엣지 스타일을 반환합니다.
     * Returns the default fade edge style.
     *
     * @param color 페이드 색상 / Fade color
     * @param fraction 페이드 영역 비율 / Fade fraction
     * @param enabled 페이드 효과 활성화 여부 / Whether fade is enabled
     */
    @Composable
    fun fadeStyle(
        fraction: Float = 0.3f,
        enabled: Boolean = true,
    ): WheelPickerFadeStyle = WheelPickerFadeStyle(
        fraction = fraction,
        enabled = enabled,
    )

    /**
     * 기본 3D 변환 효과 스타일을 반환합니다.
     * Returns the default 3D transform style.
     *
     * @param rotationEnabled 3D 회전 활성화 여부 / Whether rotation is enabled
     * @param maxRotationDegree 최대 회전 각도 / Maximum rotation degree
     * @param scaleEnabled 크기 변화 활성화 여부 / Whether scale is enabled
     * @param minScale 최소 크기 비율 / Minimum scale
     * @param alphaEnabled 투명도 변화 활성화 여부 / Whether alpha is enabled
     * @param minAlpha 최소 투명도 / Minimum alpha
     */
    @Composable
    fun transformStyle(
        rotationEnabled: Boolean = true,
        maxRotationDegree: Float = 20f,
        scaleEnabled: Boolean = true,
        minScale: Float = 0.85f,
        alphaEnabled: Boolean = true,
        minAlpha: Float = 0.7f,
    ): WheelPickerTransformStyle = WheelPickerTransformStyle(
        rotationEnabled = rotationEnabled,
        maxRotationDegree = maxRotationDegree,
        scaleEnabled = scaleEnabled,
        minScale = minScale,
        alphaEnabled = alphaEnabled,
        minAlpha = minAlpha,
    )
}