package com.sonms.systembars

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import com.sonms.systembars.internal.BarSlot
import com.sonms.systembars.internal.SystemBarsController
import com.sonms.systembars.internal.averageArgb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Declares how the system bars should look while this content is on screen. Requires a
 * [SystemBarsHost] above it; without one the modifier is inert.
 *
 * Deeper usages win over shallower ones, and among equal depth the most recently attached wins;
 * [BarContent.Inherit] falls through to the entry below.
 *
 * **Order matters for [BarContent.Auto]:** the modifier samples what is drawn *inside* it, so put it
 * before the modifiers that draw the background: `Modifier.systemBars().fillMaxSize().background(c)`.
 */
public fun Modifier.systemBars(
    status: BarContent = BarContent.Auto,
    navigation: BarContent = BarContent.Auto,
): Modifier = this.then(SystemBarsElement(status, navigation))

private data class SystemBarsElement(
    val status: BarContent,
    val navigation: BarContent,
) : ModifierNodeElement<SystemBarsNode>() {
    override fun create() = SystemBarsNode(status, navigation)

    override fun update(node: SystemBarsNode) {
        node.updateContent(status, navigation)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "systemBars"
        properties["status"] = status
        properties["navigation"] = navigation
    }
}

private class SystemBarsNode(
    status: BarContent,
    navigation: BarContent,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, GlobalPositionAwareModifierNode {

    private val statusSlot = BarSlot(status)
    private val navigationSlot = BarSlot(navigation)
    private var controller: SystemBarsController? = null
    private var disposeHandle: (() -> Unit)? = null
    private var updateHandle: (() -> Unit)? = null
    private var setPriority: ((Int) -> Unit)? = null
    private var depthKnown = false

    private var sampleBitmap: ImageBitmap? = null
    private var sampleCanvas: Canvas? = null
    private var windowOffset = Offset.Zero
    private var lastSampleNanos = 0L
    private var trailingScheduled = false
    private val pixels = IntArray(SystemBarsController.SAMPLE_W * SystemBarsController.SAMPLE_H)

    // Not an auto-invalidating node: we manage our own redraw.
    override val shouldAutoInvalidate: Boolean get() = false

    override fun onAttach() {
        val c = currentValueOf(LocalSystemBarsController)
        if (c == null) {
            Log.w("SystemBars", "Modifier.systemBars used without SystemBarsHost — ignored.")
            return
        }
        controller = c
        val h = c.register(statusSlot, navigationSlot)
        disposeHandle = h::dispose
        setPriority = h::setPriority
        depthKnown = false
        updateHandle = { h.update(statusSlot, navigationSlot) }
    }

    override fun onDetach() {
        disposeHandle?.invoke()
        disposeHandle = null
        updateHandle = null
        setPriority = null
        controller = null
        sampleBitmap = null
        sampleCanvas = null
    }

    fun updateContent(status: BarContent, navigation: BarContent) {
        if (statusSlot.content == status && navigationSlot.content == navigation) return
        statusSlot.content = status
        navigationSlot.content = navigation
        updateHandle?.invoke()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        windowOffset = coordinates.positionInWindow()
        if (!depthKnown) {
            // Deeper usages must win over their ancestors, but nested nodes attach child-first, so the
            // registration order alone is wrong. Layout depth is only reachable via coordinates.
            depthKnown = true
            var depth = 0
            var parent = coordinates.parentLayoutCoordinates
            while (parent != null) { depth++; parent = parent.parentLayoutCoordinates }
            setPriority?.invoke(depth)
        }
    }

    /**
     * Content is drawn normally every frame. Only when a sample is due is it drawn a second time into a
     * tiny software bitmap (canvas swapped for the duration), so there is no per-frame double rendering
     * and no GPU read-back. Hardware-only effects (blur, RenderEffect) are not reproduced in the sample.
     */
    override fun ContentDrawScope.draw() {
        drawContent()
        val c = controller ?: return
        if (statusSlot.content == BarContent.AutoWindow || navigationSlot.content == BarContent.AutoWindow) {
            c.requestWindowSample()
        }
        if (statusSlot.content != BarContent.Auto && navigationSlot.content != BarContent.Auto) return

        val wait = c.config.intervalMillis * 1_000_000L - (System.nanoTime() - lastSampleNanos)
        if (wait > 0) {
            // Trailing sample so the last frame of a scroll is still observed.
            if (!trailingScheduled) {
                trailingScheduled = true
                coroutineScope.launch {
                    delay(wait / 1_000_000L + 1)
                    trailingScheduled = false
                    invalidateDraw()
                }
            }
            return
        }
        lastSampleNanos = System.nanoTime()

        var changed = false
        if (statusSlot.content == BarContent.Auto) changed = sampleBar(statusSlot, top = true, c.statusBarHeight()) || changed
        if (navigationSlot.content == BarContent.Auto) changed = sampleBar(navigationSlot, top = false, c.navigationBarHeight()) || changed
        if (changed) coroutineScope.launch { c.apply() }
    }

    /** Draws the part of the content under the bar into the sample bitmap and stores its mean color. */
    private fun ContentDrawScope.sampleBar(slot: BarSlot, top: Boolean, barHeightPx: Int): Boolean {
        if (barHeightPx <= 0 || size.width <= 0f) return false
        val windowHeight = currentValueOf(LocalView).height.toFloat()
        val barTop = if (top) 0f else windowHeight - barHeightPx
        val localTop = (barTop - windowOffset.y).coerceIn(0f, size.height)
        val localBottom = (barTop + barHeightPx - windowOffset.y).coerceIn(0f, size.height)
        val rectHeight = localBottom - localTop
        if (rectHeight <= 0f) return false

        val bitmap = sampleBitmap ?: ImageBitmap(SystemBarsController.SAMPLE_W, SystemBarsController.SAMPLE_H)
            .also { sampleBitmap = it; sampleCanvas = Canvas(it) }
        val canvas = sampleCanvas!!
        val w = SystemBarsController.SAMPLE_W.toFloat()
        val h = SystemBarsController.SAMPLE_H.toFloat()

        val outer = this
        val savedCanvas = drawContext.canvas
        val savedSize = drawContext.size
        drawContext.canvas = canvas
        drawContext.size = Size(w, h)
        try {
            drawRect(androidx.compose.ui.graphics.Color.Transparent, size = Size(w, h), blendMode = BlendMode.Clear)
            scale(w / outer.size.width, h / rectHeight, pivot = Offset.Zero) {
                translate(0f, -localTop) { outer.drawContent() }
            }
        } finally {
            drawContext.canvas = savedCanvas
            drawContext.size = savedSize
        }
        bitmap.readPixels(pixels)
        val avg = averageArgb(pixels) ?: return false
        val changed = slot.sampleArgb != avg
        slot.sampleArgb = avg
        return changed
    }
}
