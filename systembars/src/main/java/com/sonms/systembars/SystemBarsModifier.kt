package com.sonms.systembars

import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import com.sonms.systembars.internal.BarSlot
import com.sonms.systembars.internal.SystemBarsController
import com.sonms.systembars.internal.averageArgb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Declares how the system bars should look while this content is on screen. Requires a
 * [SystemBarsHost] above it; without one the modifier is inert.
 *
 * Later (deeper / newer) usages win over earlier ones; [BarContent.Inherit] falls through.
 * Use [BarContent.Auto] on the node that actually draws behind the bar (typically the screen root).
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

    private var contentLayer: GraphicsLayer? = null
    private var stripLayer: GraphicsLayer? = null
    private var windowOffset = Offset.Zero
    private var lastSampleNanos = 0L
    private var sampling = false
    private var trailingScheduled = false

    // Not an auto-invalidating node: we manage our own redraw (content changes need a re-record).
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
        updateHandle = { h.update(statusSlot, navigationSlot) }
    }

    override fun onDetach() {
        disposeHandle?.invoke()
        disposeHandle = null
        updateHandle = null
        controller = null
        val gc = requireGraphicsContext()
        contentLayer?.let(gc::releaseGraphicsLayer)
        stripLayer?.let(gc::releaseGraphicsLayer)
        contentLayer = null
        stripLayer = null
    }

    fun updateContent(status: BarContent, navigation: BarContent) {
        if (statusSlot.content == status && navigationSlot.content == navigation) return
        statusSlot.content = status
        navigationSlot.content = navigation
        updateHandle?.invoke()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        windowOffset = coordinates.positionInWindow()
    }

    override fun ContentDrawScope.draw() {
        val c = controller
        val usesLayer = c != null &&
            (statusSlot.content == BarContent.Auto || navigationSlot.content == BarContent.Auto)
        if (usesLayer) {
            val gc = requireGraphicsContext()
            val layer = contentLayer ?: gc.createGraphicsLayer().also { contentLayer = it }
            layer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                this@draw.drawContent()
            }
            drawLayer(layer)
            requestSample(layer, size.width, size.height)
        } else {
            drawContent()
            if (c != null &&
                (statusSlot.content == BarContent.AutoWindow || navigationSlot.content == BarContent.AutoWindow)
            ) {
                c.requestWindowSample()
            }
        }
    }

    private fun ContentDrawScope.requestSample(layer: GraphicsLayer, width: Float, height: Float) {
        val c = controller ?: return
        val now = System.nanoTime()
        val intervalNanos = c.config.intervalMillis * 1_000_000L
        val wait = intervalNanos - (now - lastSampleNanos)
        if (sampling) return
        if (wait > 0) {
            // Trailing sample so the last frame of a scroll is still observed.
            if (!trailingScheduled) {
                trailingScheduled = true
                coroutineScope.launch {
                    delay(wait / 1_000_000L + 1)
                    trailingScheduled = false
                    contentLayer?.let { sample(it, width, height) }
                }
            }
            return
        }
        coroutineScope.launch { sample(layer, width, height) }
    }

    private suspend fun sample(layer: GraphicsLayer, width: Float, height: Float) {
        val c = controller ?: return
        if (sampling) return
        sampling = true
        lastSampleNanos = System.nanoTime()
        try {
            if (statusSlot.content == BarContent.Auto) {
                sampleBar(layer, width, height, statusSlot, top = true, c.statusBarHeight())
            }
            if (navigationSlot.content == BarContent.Auto) {
                sampleBar(layer, width, height, navigationSlot, top = false, c.navigationBarHeight())
            }
            c.apply()
        } finally {
            sampling = false
        }
    }

    /** Renders the part of [layer] that sits under the bar into a tiny layer and averages its pixels. */
    private suspend fun sampleBar(
        layer: GraphicsLayer,
        width: Float,
        height: Float,
        slot: BarSlot,
        top: Boolean,
        barHeightPx: Int,
    ) {
        if (barHeightPx <= 0) return
        val view = currentValueOf(LocalView)
        val windowHeight = view.height.toFloat()
        // Bar rect in node-local coordinates (window coords minus node origin), clipped to the node.
        val barTop = if (top) 0f else windowHeight - barHeightPx
        val localTop = (barTop - windowOffset.y).coerceIn(0f, height)
        val localBottom = (barTop + barHeightPx - windowOffset.y).coerceIn(0f, height)
        val rectHeight = localBottom - localTop
        if (rectHeight <= 0f || width <= 0f) return

        val gc = requireGraphicsContext()
        val strip = stripLayer ?: gc.createGraphicsLayer().also { stripLayer = it }
        val density = androidx.compose.ui.unit.Density(1f)
        strip.record(
            density,
            androidx.compose.ui.unit.LayoutDirection.Ltr,
            IntSize(SystemBarsController.SAMPLE_W, SystemBarsController.SAMPLE_H),
        ) {
            scale(
                SystemBarsController.SAMPLE_W / width,
                SystemBarsController.SAMPLE_H / rectHeight,
                pivot = Offset.Zero,
            ) {
                translate(0f, -localTop) { drawLayer(layer) }
            }
        }
        val bitmap = strip.toImageBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        averageArgb(pixels)?.let { slot.sampleArgb = it }
    }
}
