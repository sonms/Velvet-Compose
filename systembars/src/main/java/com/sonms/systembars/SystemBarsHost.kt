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
 * 한 윈도우의 시스템 바 레지스트리 루트입니다. `setContent` 바로 아래에 한 번만 두세요.
 * Activity 에서 `enableEdgeToEdge()` 를 먼저 호출해야 하며(이 라이브러리는 호출하지 않습니다),
 * 이후 화면에서 [systemBars] 를 사용합니다. Dialog 는 별도 윈도우이므로 [SystemBarsDialog] 를 쓰세요.
 *
 * Root of the system-bars registry for one window. Place it once under `setContent`, after calling
 * `enableEdgeToEdge()` in the Activity (the library does not do that for you). Screens then use
 * [systemBars]. Dialogs are separate windows, use [SystemBarsDialog].
 *
 * @param config 간격 / 임계값 / 스크림 설정 / Interval, thresholds and scrim settings
 * @param content 레지스트리를 사용할 콘텐츠 / Content that can use the registry
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
