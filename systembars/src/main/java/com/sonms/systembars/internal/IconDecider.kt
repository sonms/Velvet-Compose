package com.sonms.systembars.internal

/** System scrim colors (ARGB) for light-appearance and dark-appearance bars. */
internal class Scrim(val light: Int, val dark: Int)

/**
 * Decides the icon appearance for a bar from its background color.
 *
 * Returns `true` for **dark icons** (light background), `false` for light icons.
 *
 * Without a scrim, a luminance hysteresis band avoids flicker near the midpoint.
 * With a scrim, the system picks the scrim from our choice, so both candidates are
 * composited and the one with the better icon contrast wins; it only switches away from
 * [previousDarkIcons] when it improves contrast by [SWITCH_MARGIN].
 */
internal class IconDecider(
    private val lowThreshold: Float,
    private val highThreshold: Float,
) {
    fun decide(contentArgb: Int, previousDarkIcons: Boolean?, scrim: Scrim?): Boolean =
        if (scrim == null) decidePlain(contentArgb, previousDarkIcons)
        else decideWithScrim(contentArgb, previousDarkIcons, scrim)

    private fun decidePlain(content: Int, previous: Boolean?): Boolean {
        val l = Luma.relativeLuminance(content)
        return when {
            previous == null -> l >= (lowThreshold + highThreshold) / 2f
            previous && l < lowThreshold -> false
            !previous && l > highThreshold -> true
            else -> previous
        }
    }

    private fun decideWithScrim(content: Int, previous: Boolean?, scrim: Scrim): Boolean {
        val darkIconsBg = Luma.relativeLuminance(Luma.composite(scrim.light, content))
        val lightIconsBg = Luma.relativeLuminance(Luma.composite(scrim.dark, content))
        val darkIconsContrast = Luma.contrastRatio(darkIconsBg, 0f)
        val lightIconsContrast = Luma.contrastRatio(lightIconsBg, 1f)
        return when (previous) {
            null -> darkIconsContrast >= lightIconsContrast
            true -> !(lightIconsContrast > darkIconsContrast * SWITCH_MARGIN)
            false -> darkIconsContrast > lightIconsContrast * SWITCH_MARGIN
        }
    }

    companion object {
        const val SWITCH_MARGIN = 1.1f
    }
}
