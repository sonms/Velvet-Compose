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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
 * 이 콘텐츠가 화면에 있는 동안 시스템 바가 어떻게 보일지 선언합니다.
 * 위쪽에 [SystemBarsHost] 가 있어야 하며, 없으면 아무 일도 하지 않습니다(디버그 로그만 남깁니다).
 *
 * 여러 곳에서 쓰면 **더 깊은 노드가 이기고, 깊이가 같으면 나중에 붙은 쪽이 이깁니다.**
 * 노드가 사라지면 이전 항목으로 복귀하고, [BarContent.Inherit] 는 아래 항목을 따릅니다.
 *
 * **[BarContent.Auto] 는 순서가 중요합니다.** modifier 안쪽에 그려진 것만 샘플링하므로
 * 배경을 그리는 modifier 보다 앞에 두세요: `Modifier.systemBars().fillMaxSize().background(color)`.
 *
 * Declares how the system bars should look while this content is on screen. Requires a
 * [SystemBarsHost] above it; without one the modifier is inert (a debug log is printed).
 *
 * Deeper usages win over shallower ones, and among equal depth the most recently attached wins;
 * [BarContent.Inherit] falls through to the entry below.
 *
 * **Order matters for [BarContent.Auto]:** the modifier samples what is drawn *inside* it, so put it
 * before the modifiers that draw the background: `Modifier.systemBars().fillMaxSize().background(color)`.
 *
 * @param status 상태바 설정 / How the status bar decides its icons
 * @param navigation 내비게이션바 설정 / How the navigation bar decides its icons
 * @param statusPaint 상태바 영역을 칠할지(기본 [BarPaint.None], 투명) / Whether to paint the status bar area
 * @param navigationPaint 내비게이션바 영역을 칠할지(기본 [BarPaint.None], 투명) / Whether to paint the navigation bar area
 */
public fun Modifier.systemBars(
    status: BarContent = BarContent.Auto,
    navigation: BarContent = BarContent.Auto,
    statusPaint: BarPaint = BarPaint.None,
    navigationPaint: BarPaint = BarPaint.None,
): Modifier = this.then(SystemBarsElement(status, navigation, statusPaint, navigationPaint))

private data class SystemBarsElement(
    val status: BarContent,
    val navigation: BarContent,
    val statusPaint: BarPaint,
    val navigationPaint: BarPaint,
) : ModifierNodeElement<SystemBarsNode>() {
    override fun create() = SystemBarsNode(status, navigation, statusPaint, navigationPaint)

    override fun update(node: SystemBarsNode) {
        node.updateContent(status, navigation, statusPaint, navigationPaint)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "systemBars"
        properties["status"] = status
        properties["navigation"] = navigation
        properties["statusPaint"] = statusPaint
        properties["navigationPaint"] = navigationPaint
    }
}

private class SystemBarsNode(
    status: BarContent,
    navigation: BarContent,
    statusPaint: BarPaint,
    navigationPaint: BarPaint,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, GlobalPositionAwareModifierNode {

    private val statusSlot = BarSlot(status, statusPaint)
    private val navigationSlot = BarSlot(navigation, navigationPaint)
    private var controller: SystemBarsController? = null
    private var disposeHandle: (() -> Unit)? = null
    private var updateHandle: (() -> Unit)? = null
    private var setPriority: ((Int) -> Unit)? = null
    private var depthKnown = false
    private var removeChangeListener: (() -> Unit)? = null
    private var wasSampling = false

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
        removeChangeListener = c.addChangeListener(::onRegistryChanged)
        // A parent's draw block does not re-run when only its children redraw (e.g. scrolling), so
        // watch frames at the window level and ask for a re-draw of this node when a sample is due.
        val view = currentValueOf(LocalView)
        drawObserverView = view
        if (view.viewTreeObserver.isAlive) view.viewTreeObserver.addOnDrawListener(onFrame)
    }

    override fun onDetach() {
        removeChangeListener?.invoke()
        removeChangeListener = null
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

    /** Only the node that currently decides a bar is worth sampling for; shadowed ones skip the work. */
    private fun wantsStatus(c: SystemBarsController) = needsSample(statusSlot) && c.isWinner(statusSlot, top = true)
    private fun wantsNavigation(c: SystemBarsController) = needsSample(navigationSlot) && c.isWinner(navigationSlot, top = false)

    /** A slot needs pixels when its icons come from sampling, or when it paints the sampled color. */
    private fun needsSample(slot: BarSlot) = slot.effectiveContent == BarContent.Auto || slot.effectivePaint == BarPaint.Sampled

    /** A node that just started winning has a stale (or no) sample: take a fresh one right away. */
    private fun onRegistryChanged() {
        val c = controller ?: return
        // Winner changed: the new winner has to paint, the old one has to stop.
        if (paintsAnything()) coroutineScope.launch { invalidateDraw() }
        val now = wantsStatus(c) || wantsNavigation(c)
        if (now && !wasSampling) {
            lastSampleNanos = 0L
            requestOwnRedraw()
        }
        wasSampling = now
    }

    private fun paintsAnything() =
        statusSlot.effectivePaint != BarPaint.None || navigationSlot.effectivePaint != BarPaint.None

    fun updateContent(status: BarContent, navigation: BarContent, statusPaint: BarPaint, navigationPaint: BarPaint) {
        if (statusSlot.content == status && navigationSlot.content == navigation &&
            statusSlot.paint == statusPaint && navigationSlot.paint == navigationPaint
        ) return
        val hadPaint = paintsAnything()
        statusSlot.content = status
        navigationSlot.content = navigation
        statusSlot.paint = statusPaint
        navigationSlot.paint = navigationPaint
        updateHandle?.invoke()
        if (hadPaint || paintsAnything()) invalidateDraw()
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
        sampleIfDue(c)
        // Paint after sampling so a fresh sample is used right away; the sample itself never includes the paint.
        paintBar(top = true, statusSlot, c)
        paintBar(top = false, navigationSlot, c)
    }

    private fun ContentDrawScope.sampleIfDue(c: SystemBarsController) {
        if (statusSlot.content == BarContent.AutoWindow || navigationSlot.content == BarContent.AutoWindow) {
            c.requestWindowSample()
        }
        if (!wantsStatus(c) && !wantsNavigation(c)) return

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

    /** Paints the part of the bar that overlaps this node, only while this node decides that bar. */
    private fun ContentDrawScope.paintBar(top: Boolean, slot: BarSlot, c: SystemBarsController) {
        val argb = when (val p = slot.effectivePaint) {
            BarPaint.None -> return
            BarPaint.Sampled -> slot.sampleArgb ?: return
            is BarPaint.Solid -> p.color.toArgb()
        }
        if (!c.isWinner(slot, top)) return
        val barHeight = if (top) c.statusBarHeight() else c.navigationBarHeight()
        val span = localSpanUnderBar(top, barHeight, currentValueOf(LocalView).height.toFloat()) ?: return
        drawRect(Color(argb), topLeft = Offset(0f, span.first), size = Size(size.width, span.second - span.first))
    }

    /** Window-level frame callback: request a re-draw of this node when a sample is due. */
    private fun requestSampleIfDue() {
        // The frame produced by our own re-draw request must not trigger another one (endless loop).
        if (ownRedraws > 0) { ownRedraws--; return }
        val c = controller ?: return
        if (!wantsStatus(c) && !wantsNavigation(c)) return
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
        val wantStatus = wantsStatus(c)
        val wantNav = wantsNavigation(c)
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
            drawRect(Color.Transparent, size = Size(NODE_W.toFloat(), NODE_H.toFloat()), blendMode = BlendMode.Clear)
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

    /** Local y range (top, bottom) of the bar inside this node, or null if the bar misses the node. */
    private fun ContentDrawScope.localSpanUnderBar(top: Boolean, barHeightPx: Int, windowHeight: Float): Pair<Float, Float>? {
        if (barHeightPx <= 0) return null
        val barTop = if (top) 0f else windowHeight - barHeightPx
        val localTop = (barTop - windowOffset.y).coerceIn(0f, size.height)
        val localBottom = (barTop + barHeightPx - windowOffset.y).coerceIn(0f, size.height)
        return if (localBottom - localTop <= 0f) null else localTop to localBottom
    }

    /** Bitmap rows (inclusive start, exclusive end) covered by the bar, or null if the bar misses this node. */
    private fun ContentDrawScope.rowsUnderBar(top: Boolean, barHeightPx: Int, windowHeight: Float): IntRange? {
        val (localTop, localBottom) = localSpanUnderBar(top, barHeightPx, windowHeight) ?: return null
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
