package com.sonms.systembars

import android.os.Trace
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
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
    private var ownRedraws = 0
    private var drawObserverView: View? = null
    private val onFrame = ViewTreeObserver.OnDrawListener { requestSampleIfDue() }
    private val pixels = IntArray(16 * 160)

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
        // A parent's draw block does not re-run when only its children redraw (e.g. scrolling), so
        // watch frames at the window level and ask for a re-draw of this node when a sample is due.
        val view = currentValueOf(LocalView)
        drawObserverView = view
        if (view.viewTreeObserver.isAlive) view.viewTreeObserver.addOnDrawListener(onFrame)
    }

    override fun onDetach() {
        disposeHandle?.invoke()
        drawObserverView?.let { v -> if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnDrawListener(onFrame) }
        drawObserverView = null
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
            scheduleTrailing(wait)
            return
        }
        lastSampleNanos = System.nanoTime()

        // Visible in Perfetto/systrace as "SystemBars.sample" (enable app tracing for the package).
        Trace.beginSection("SystemBars.sample")
        var changed = false
        try {
            changed = sampleBars(c)
        } finally {
            Trace.endSection()
        }
        if (changed) coroutineScope.launch { c.apply() }
    }

    /** Window-level frame callback: request a re-draw of this node when a sample is due. */
    private fun requestSampleIfDue() {
        // The frame produced by our own re-draw request must not trigger another one (endless loop).
        if (ownRedraws > 0) { ownRedraws--; return }
        val c = controller ?: return
        if (statusSlot.content != BarContent.Auto && navigationSlot.content != BarContent.Auto) return
        val wait = c.config.intervalMillis * 1_000_000L - (System.nanoTime() - lastSampleNanos)
        if (wait > 0) scheduleTrailing(wait) else requestOwnRedraw()
    }

    /** Not inside the draw traversal: invalidate on the next main-thread turn. */
    private fun requestOwnRedraw() {
        ownRedraws++
        coroutineScope.launch { invalidateDraw() }
    }

    /** Trailing sample so the last frame of a scroll is still observed. */
    private fun scheduleTrailing(waitNanos: Long) {
        if (trailingScheduled) return
        trailingScheduled = true
        coroutineScope.launch {
            delay(waitNanos / 1_000_000L + 1)
            trailingScheduled = false
            ownRedraws++
            invalidateDraw()
        }
    }

    /**
     * Draws the node's content once, scaled down to a [NODE_W]×[NODE_H] software bitmap, then reads the rows
     * that sit under each bar. One content pass serves both bars (the cost is dominated by replaying the draw
     * commands, not by pixel count).
     */
    private fun ContentDrawScope.sampleBars(c: SystemBarsController): Boolean {
        val wantStatus = statusSlot.content == BarContent.Auto
        val wantNav = navigationSlot.content == BarContent.Auto
        if (size.width <= 0f || size.height <= 0f) return false
        val windowHeight = currentValueOf(LocalView).height.toFloat()
        val statusRows = if (wantStatus) rowsUnderBar(top = true, c.statusBarHeight(), windowHeight) else null
        val navRows = if (wantNav) rowsUnderBar(top = false, c.navigationBarHeight(), windowHeight) else null
        if (statusRows == null && navRows == null) return false

        val bitmap = sampleBitmap ?: ImageBitmap(NODE_W, NODE_H).also { sampleBitmap = it; sampleCanvas = Canvas(it) }
        val canvas = sampleCanvas!!
        val outer = this
        val nodeWidth = size.width // read before drawContext.size is swapped below
        val nodeHeight = size.height
        val savedCanvas = drawContext.canvas
        val savedSize = drawContext.size
        drawContext.canvas = canvas
        drawContext.size = Size(NODE_W.toFloat(), NODE_H.toFloat())
        try {
            drawRect(androidx.compose.ui.graphics.Color.Transparent, size = Size(NODE_W.toFloat(), NODE_H.toFloat()), blendMode = BlendMode.Clear)
            scale(NODE_W / nodeWidth, NODE_H / nodeHeight, pivot = Offset.Zero) { outer.drawContent() }
        } finally {
            drawContext.canvas = savedCanvas
            drawContext.size = savedSize
        }

        var changed = false
        if (statusRows != null) changed = readRows(bitmap, statusRows, statusSlot) || changed
        if (navRows != null) changed = readRows(bitmap, navRows, navigationSlot) || changed
        return changed
    }

    /** Bitmap rows (inclusive start, exclusive end) covered by the bar, or null if the bar misses this node. */
    private fun ContentDrawScope.rowsUnderBar(top: Boolean, barHeightPx: Int, windowHeight: Float): IntRange? {
        if (barHeightPx <= 0) return null
        val barTop = if (top) 0f else windowHeight - barHeightPx
        val localTop = (barTop - windowOffset.y).coerceIn(0f, size.height)
        val localBottom = (barTop + barHeightPx - windowOffset.y).coerceIn(0f, size.height)
        if (localBottom - localTop <= 0f) return null
        val r0 = (localTop / size.height * NODE_H).toInt().coerceIn(0, NODE_H - 1)
        val r1 = kotlin.math.ceil(localBottom / size.height * NODE_H).toInt().coerceIn(r0 + 1, NODE_H)
        return r0 until r1
    }

    private fun readRows(bitmap: ImageBitmap, rows: IntRange, slot: BarSlot): Boolean {
        val height = rows.last - rows.first + 1
        bitmap.readPixels(pixels, startX = 0, startY = rows.first, width = NODE_W, height = height)
        val avg = averageArgb(pixels, NODE_W * height) ?: return false
        val changed = slot.sampleArgb != avg
        slot.sampleArgb = avg
        return changed
    }

    private companion object {
        const val NODE_W = 16
        const val NODE_H = 160
    }

}
