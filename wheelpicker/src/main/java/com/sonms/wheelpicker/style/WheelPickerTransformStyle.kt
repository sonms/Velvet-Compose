package com.sonms.wheelpicker.style

import androidx.compose.runtime.Immutable

/**
 * 3D 변환 효과의 스타일을 정의합니다.
 * Defines the style of the 3D transform effect.
 *
 * @param rotationEnabled 3D 회전 효과 활성화 여부 / Whether 3D rotation is enabled
 * @param maxRotationDegree 최대 회전 각도 / Maximum rotation degree
 * @param scaleEnabled 크기 변화 효과 활성화 여부 / Whether scale effect is enabled
 * @param minScale 최소 크기 비율 (0f ~ 1f) / Minimum scale ratio (0f ~ 1f)
 * @param alphaEnabled 투명도 변화 효과 활성화 여부 / Whether alpha effect is enabled
 * @param minAlpha 최소 투명도 (0f ~ 1f) / Minimum alpha value (0f ~ 1f)
 */
@Immutable
data class WheelPickerTransformStyle(
    val rotationEnabled: Boolean,
    val maxRotationDegree: Float,
    val scaleEnabled: Boolean,
    val minScale: Float,
    val alphaEnabled: Boolean,
    val minAlpha: Float,
)