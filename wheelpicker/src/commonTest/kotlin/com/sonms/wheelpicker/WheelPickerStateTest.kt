package com.sonms.wheelpicker

import com.sonms.wheelpicker.state.WheelPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * commonTest — runs on every target (Android unit test host JVM + iOS simulator),
 * proving WheelPickerState's public API behaves consistently across platforms.
 */
class WheelPickerStateTest {
    @Test
    fun initialIndex_isExposedAsGiven() {
        val state = WheelPickerState(initialIndex = 3)
        assertEquals(3, state.initialIndex)
    }

    @Test
    fun currentIndex_isZero_beforePagerStateIsAttached() {
        val state = WheelPickerState()
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun isScrollInProgress_isFalse_beforePagerStateIsAttached() {
        val state = WheelPickerState()
        assertFalse(state.isScrollInProgress)
    }
}
