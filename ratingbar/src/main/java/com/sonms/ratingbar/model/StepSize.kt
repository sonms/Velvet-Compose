package com.sonms.ratingbar.model

/**
 * 별점 단계 크기를 정의합니다.
 * Defines the step size for rating selection.
 *
 * @property value 실제 단계 값
 * Actual step value
 */
enum class StepSize(
    val value: Float
) {
    /**
     * 1.0 단위로 선택
     * Select in increments of 1.0
     */
    FULL(1.0f),

    /**
     * 0.5 단위로 선택
     * Select in increments of 0.5
     */
    HALF(0.5f),
}