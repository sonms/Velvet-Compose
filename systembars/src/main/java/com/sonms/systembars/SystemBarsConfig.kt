package com.sonms.systembars

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Approximation of the translucent scrim the system draws behind 3-button navigation
 * ([lightScrim] under dark icons, [darkScrim] under light icons). The real values are
 * platform internals, so supply your own if a device differs.
 */
@Immutable
public class ScrimModel(public val lightScrim: Color, public val darkScrim: Color) {
    public companion object {
        /** Matches androidx `SystemBarStyle` defaults. */
        public val AndroidxDefault: ScrimModel =
            ScrimModel(lightScrim = Color(0xE6FFFFFF), darkScrim = Color(0x801B1B1B))
    }

    override fun equals(other: Any?): Boolean =
        other is ScrimModel && lightScrim == other.lightScrim && darkScrim == other.darkScrim

    override fun hashCode(): Int = 31 * lightScrim.hashCode() + darkScrim.hashCode()
}

/**
 * Tuning for [SystemBarsHost].
 *
 * @param intervalMillis minimum time between pixel samples.
 * @param lowThreshold luminance below which a light background flips to a dark one.
 * @param highThreshold luminance above which a dark background flips to a light one.
 * @param contrastEnforced value for `Window.isNavigationBarContrastEnforced` (API 29+).
 * @param scrimModel scrim used for the 3-button navigation bar when [contrastEnforced]; `null` disables it.
 */
@Immutable
public data class SystemBarsConfig(
    val intervalMillis: Long = 100L,
    val lowThreshold: Float = 0.45f,
    val highThreshold: Float = 0.55f,
    val contrastEnforced: Boolean = true,
    val scrimModel: ScrimModel? = ScrimModel.AndroidxDefault,
) {
    init {
        require(lowThreshold <= highThreshold) { "lowThreshold must be <= highThreshold" }
    }
}
