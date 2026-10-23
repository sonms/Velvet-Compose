package com.sonms.systembars.internal

import androidx.compose.ui.graphics.Color
import com.sonms.systembars.BarContent
import com.sonms.systembars.BarPaint
import org.junit.Assert.assertEquals
import org.junit.Test

class BarSlotTest {
    private val red = Color.Red

    @Test
    fun solidPaintOverAuto_isTheBackground_soNoSamplingIsNeeded() {
        val slot = BarSlot(BarContent.Auto, BarPaint.Solid(red))
        assertEquals(BarContent.Background(red), slot.effectiveContent)
    }

    @Test
    fun declaredContent_isNotOverriddenByPaint() {
        assertEquals(BarContent.Light, BarSlot(BarContent.Light, BarPaint.Solid(red)).effectiveContent)
        assertEquals(BarContent.AutoWindow, BarSlot(BarContent.AutoWindow, BarPaint.Solid(red)).effectiveContent)
    }

    @Test
    fun defaultPaintIsNone_andChangesNothing() {
        val slot = BarSlot(BarContent.Auto)
        assertEquals(BarPaint.None, slot.effectivePaint)
        assertEquals(BarContent.Auto, slot.effectiveContent)
    }

    @Test
    fun sampledPaint_onlyAppliesWithAuto() {
        assertEquals(BarPaint.Sampled, BarSlot(BarContent.Auto, BarPaint.Sampled).effectivePaint)
        assertEquals(BarPaint.None, BarSlot(BarContent.AutoWindow, BarPaint.Sampled).effectivePaint)
        assertEquals(BarPaint.None, BarSlot(BarContent.Dark, BarPaint.Sampled).effectivePaint)
    }

    @Test
    fun solidPaint_appliesRegardlessOfContent() {
        assertEquals(BarPaint.Solid(red), BarSlot(BarContent.AutoWindow, BarPaint.Solid(red)).effectivePaint)
    }
}
