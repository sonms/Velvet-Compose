package com.sonms.systembars

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * How a system bar (status or navigation) decides its icon appearance.
 *
 * [Light] / [Dark] / [Background] describe the **background** behind the bar:
 * a light background gets dark icons and vice versa.
 */
@Immutable
public sealed interface BarContent {
    /** Sample the Compose content drawn inside the modifier (re-drawn into a tiny software bitmap). */
    public data object Auto : BarContent

    /**
     * Sample the real window via `PixelCopy` (API 26+; no-op below). Use it when the area behind
     * the bar is drawn outside Compose (`SurfaceView`, maps, video).
     */
    public data object AutoWindow : BarContent

    /** The background behind the bar is [color] (declared or extracted, e.g. from an image palette). */
    public data class Background(val color: Color) : BarContent

    /** The background is light → dark icons. */
    public data object Light : BarContent

    /** The background is dark → light icons. */
    public data object Dark : BarContent

    /** Use the entry below this one in the stack. */
    public data object Inherit : BarContent
}
