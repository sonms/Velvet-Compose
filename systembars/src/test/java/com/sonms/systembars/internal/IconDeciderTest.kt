package com.sonms.systembars.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconDeciderTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val lightScrim = 0xE6FFFFFF.toInt()
    private val darkScrim = 0x801B1B1B.toInt()

    private fun decider() = IconDecider(lowThreshold = 0.45f, highThreshold = 0.55f)

    @Test
    fun noScrim_lightBackground_wantsDarkIcons() {
        assertTrue(decider().decide(white, previousDarkIcons = null, scrim = null))
    }

    @Test
    fun noScrim_darkBackground_wantsLightIcons() {
        assertFalse(decider().decide(black, previousDarkIcons = null, scrim = null))
    }

    @Test
    fun hysteresis_staysInsideBand() {
        // luminance ~0.50 → inside 0.45..0.55: keep whatever was chosen before
        val mid = 0xFFbcbcbc.toInt() // relative luminance ≈ 0.50
        assertTrue(decider().decide(mid, previousDarkIcons = true, scrim = null))
        assertFalse(decider().decide(mid, previousDarkIcons = false, scrim = null))
    }

    @Test
    fun hysteresis_flipsOutsideBand() {
        assertFalse(decider().decide(0xFF202020.toInt(), previousDarkIcons = true, scrim = null))
        assertTrue(decider().decide(0xFFF0F0F0.toInt(), previousDarkIcons = false, scrim = null))
    }

    @Test
    fun scrim_picksHigherContrastCandidate() {
        val scrim = Scrim(light = lightScrim, dark = darkScrim)
        // dark content: dark icons over a near-white scrim vs white icons over a dark scrim
        // → dark scrim keeps the content dark, white icons read better
        assertFalse(decider().decide(black, previousDarkIcons = null, scrim = scrim))
        // white content: dark icons over near-white scrim are the clear winner
        assertTrue(decider().decide(white, previousDarkIcons = null, scrim = scrim))
    }

    @Test
    fun scrim_requiresMarginToSwitch() {
        // Both candidates ≈ equal contrast: the previous choice must be kept.
        val scrim = Scrim(light = 0x00FFFFFF, dark = 0x00000000) // fully transparent scrims
        val mid = 0xFF777777.toInt()
        val first = decider().decide(mid, previousDarkIcons = null, scrim = scrim)
        assertTrue(decider().decide(mid, previousDarkIcons = first, scrim = scrim) == first)
        assertTrue(decider().decide(mid, previousDarkIcons = !first, scrim = scrim) == !first)
    }
}
