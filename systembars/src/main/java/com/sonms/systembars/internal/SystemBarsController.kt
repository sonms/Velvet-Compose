package com.sonms.systembars.internal

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.sonms.systembars.BarContent
import com.sonms.systembars.SystemBarsConfig

/** Mutable per-bar entry owned by a modifier node. [sampleArgb] is the last sampled background. */
internal class BarSlot(var content: BarContent, var sampleArgb: Int? = null)

/** Applies the winning [BarSlot]s to one window. Main thread only; never exposes Compose state. */
internal class SystemBarsController(
    private val window: Window,
    private val view: View,
    val config: SystemBarsConfig,
) {
    private val registry = BarRegistry<BarSlot> { it.content == BarContent.Inherit }
    private val decider = IconDecider(config.lowThreshold, config.highThreshold)
    private var statusDarkIcons: Boolean? = null
    private var navDarkIcons: Boolean? = null
    private var lastWindowSample = 0L
    private var windowSampling = false
    private var disposed = false

    init {
        registry.onChanged = ::apply
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = config.contrastEnforced
        warnIfNotEdgeToEdge()
    }

    fun register(status: BarSlot, navigation: BarSlot) = registry.register(status, navigation)

    fun dispose() {
        disposed = true
        registry.onChanged = null
    }

    /** Bar heights in px from the current root insets (status top, navigation bottom). */
    fun statusBarHeight(): Int =
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0

    fun navigationBarHeight(): Int =
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0

    fun apply() {
        if (disposed) return
        val controller = WindowCompat.getInsetsController(window, view)
        val status = registry.resolveStatus()
        val nav = registry.resolveNavigation()

        darkIconsFor(status, statusDarkIcons, scrim = null)?.let {
            statusDarkIcons = it
            controller.isAppearanceLightStatusBars = it
        }
        darkIconsFor(nav, navDarkIcons, navigationScrim(nav))?.let {
            navDarkIcons = it
            controller.isAppearanceLightNavigationBars = it
        }
    }

    private fun darkIconsFor(slot: BarSlot?, previous: Boolean?, scrim: Scrim?): Boolean? {
        val content = slot?.content ?: return null
        return when (content) {
            BarContent.Auto, BarContent.AutoWindow ->
                slot.sampleArgb?.let { decider.decide(it, previous, scrim) }
            is BarContent.Background -> decider.decide(content.color.toArgb(), null, scrim)
            BarContent.Light -> true
            BarContent.Dark -> false
            BarContent.Inherit -> null
        }
    }

    /** Scrim applies only to 3-button navigation (tappable inset) and never to window samples (already include it). */
    private fun navigationScrim(nav: BarSlot?): Scrim? {
        val model = config.scrimModel ?: return null
        if (!config.contrastEnforced || Build.VERSION.SDK_INT < 29) return null
        if (nav?.content == BarContent.AutoWindow) return null
        val tappable = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.tappableElement())?.bottom ?: 0
        if (tappable <= 0) return null
        return Scrim(model.lightScrim.toArgb(), model.darkScrim.toArgb())
    }

    /** PixelCopy fallback; throttled by [SystemBarsConfig.intervalMillis]. No-op below API 26. */
    fun requestWindowSample() {
        if (Build.VERSION.SDK_INT < 26 || disposed || windowSampling) return
        val wantsStatus = registry.resolveStatus()?.content == BarContent.AutoWindow
        val wantsNav = registry.resolveNavigation()?.content == BarContent.AutoWindow
        if (!wantsStatus && !wantsNav) return
        val now = SystemClock.uptimeMillis()
        if (now - lastWindowSample < config.intervalMillis) return
        lastWindowSample = now

        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return
        val jobs = buildList {
            if (wantsStatus) add(registry.resolveStatus()!! to Rect(0, 0, width, statusBarHeight()))
            if (wantsNav) add(registry.resolveNavigation()!! to Rect(0, height - navigationBarHeight(), width, height))
        }.filter { !it.second.isEmpty }
        if (jobs.isEmpty()) return
        windowSampling = true
        copyNext(jobs, 0)
    }

    private fun copyNext(jobs: List<Pair<BarSlot, Rect>>, index: Int) {
        if (index >= jobs.size) {
            windowSampling = false
            apply()
            return
        }
        val (slot, rect) = jobs[index]
        val bitmap = Bitmap.createBitmap(SAMPLE_W, SAMPLE_H, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(window, rect, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) slot.sampleArgb = averageArgb(bitmap)
                bitmap.recycle()
                copyNext(jobs, index + 1)
            }, Handler(Looper.getMainLooper()))
        } catch (e: IllegalArgumentException) {
            bitmap.recycle()
            windowSampling = false
        }
    }

    private fun warnIfNotEdgeToEdge() {
        val debuggable = view.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable && (window.statusBarColor ushr 24) != 0) {
            Log.w(TAG, "Status bar is not transparent — call enableEdgeToEdge() in Activity.onCreate before setContent.")
        }
    }

    companion object {
        const val SAMPLE_W = 32
        const val SAMPLE_H = 8
        private const val TAG = "SystemBars"
    }
}

/** Mean opaque-pixel color of [bitmap]; `null` if the bitmap is (almost) fully transparent. */
internal fun averageArgb(bitmap: Bitmap): Int? {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return averageArgb(pixels)
}

internal fun averageArgb(pixels: IntArray): Int? {
    var r = 0L; var g = 0L; var b = 0L; var n = 0
    for (p in pixels) {
        if ((p ushr 24) < 0x80) continue
        r += (p ushr 16) and 0xFF; g += (p ushr 8) and 0xFF; b += p and 0xFF; n++
    }
    if (n * 10 < pixels.size) return null
    return (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
}
