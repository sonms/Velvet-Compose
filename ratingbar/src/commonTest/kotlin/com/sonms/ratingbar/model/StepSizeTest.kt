package com.sonms.ratingbar.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * commonTest — runs on every target (Android unit test host JVM + iOS simulator),
 * proving StepSize's values behave consistently across platforms.
 */
class StepSizeTest {
    @Test
    fun full_isOneWholeStep() {
        assertEquals(1.0f, StepSize.FULL.value)
    }

    @Test
    fun half_isOneHalfStep() {
        assertEquals(0.5f, StepSize.HALF.value)
    }
}
