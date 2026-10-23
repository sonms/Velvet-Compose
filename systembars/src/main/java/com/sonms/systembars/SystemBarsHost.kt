package com.sonms.systembars

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import com.sonms.systembars.internal.SystemBarsController

internal val LocalSystemBarsController = compositionLocalOf<SystemBarsController?> { null }

/**
 * Root of the system-bars registry for one window. Place it once under `setContent`
 * (after calling `enableEdgeToEdge()` in the Activity). Screens then use [Modifier.systemBars].
 */
@Composable
public fun SystemBarsHost(
    config: SystemBarsConfig = SystemBarsConfig(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val window = remember(view) { view.findWindow() }
    if (window == null) {
        content()
        return
    }
    val controller = remember(window, view, config) { SystemBarsController(window, view, config) }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    CompositionLocalProvider(LocalSystemBarsController provides controller, content = content)
}

private fun View.findWindow(): Window? {
    ((parent as? DialogWindowProvider) ?: (this as? DialogWindowProvider))?.let { return it.window }
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx.window
        ctx = ctx.baseContext
    }
    return null
}
