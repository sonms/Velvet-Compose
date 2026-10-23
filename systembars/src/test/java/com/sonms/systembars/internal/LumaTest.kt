package com.sonms.systembars.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LumaTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun luminance_ofWhiteAndBlack() {
        assertEquals(1f, Luma.relativeLuminance(white), 1e-4f)
        assertEquals(0f, Luma.relativeLuminance(black), 1e-4f)
    }

    @Test
    fun luminance_ofMidGray_isBelowHalfBecauseOfGamma() {
        val gray = 0xFF808080.toInt()
        assertEquals(0.2159f, Luma.relativeLuminance(gray), 1e-3f)
    }

    @Test
    fun contrastRatio_blackOnWhite_is21() {
        assertEquals(21f, Luma.contrastRatio(1f, 0f), 1e-3f)
        assertEquals(21f, Luma.contrastRatio(0f, 1f), 1e-3f)
    }

    @Test
    fun composite_opaqueForegroundWins() {
        assertEquals(white, Luma.composite(fg = white, bg = black))
    }

    @Test
    fun composite_halfAlphaBlendsChannels() {
        val halfWhite = 0x80FFFFFF.toInt()
        val result = Luma.composite(fg = halfWhite, bg = black)
        assertEquals(0xFF, (result ushr 24) and 0xFF)
        assertTrue(((result ushr 16) and 0xFF) in 127..129)
    }
}
