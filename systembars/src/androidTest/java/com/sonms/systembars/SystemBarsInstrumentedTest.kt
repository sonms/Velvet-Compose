package com.sonms.systembars

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonms.systembars.internal.SystemBarsController
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sampling accuracy, registry stacking and recomposition safety, on a real window.
 * Bar heights are injected so the tests do not depend on the device's system insets.
 */
@RunWith(AndroidJUnit4::class)
class SystemBarsInstrumentedTest {
    @get:Rule
    val rule = createComposeRule()

    private val fast = SystemBarsConfig(intervalMillis = 0L, scrimModel = null)
    private var view: View? = null

    @Composable
    private fun TestHost(
        config: SystemBarsConfig = fast,
        navigationBarColor: Int = android.graphics.Color.TRANSPARENT,
        content: @Composable () -> Unit,
    ) {
        val v = LocalView.current
        view = v
        val controller = remember(v, config) {
            // Draw behind the bars like an edge-to-edge app, so the bar strips overlap the content.
            v.activity().window.apply {
                WindowCompat.setDecorFitsSystemWindows(this, false)
                statusBarColor = android.graphics.Color.TRANSPARENT
                this.navigationBarColor = navigationBarColor
            }
            SystemBarsController(v.activity().window, v, config) { 40 }
        }
        CompositionLocalProvider(LocalSystemBarsController provides controller, content = content)
    }

    private fun statusDarkIcons(): Boolean = onMain {
        WindowCompat.getInsetsController(view!!.activity().window, view!!).isAppearanceLightStatusBars
    }

    private fun navDarkIcons(): Boolean = onMain {
        WindowCompat.getInsetsController(view!!.activity().window, view!!).isAppearanceLightNavigationBars
    }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        rule.runOnUiThread { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun waitFor(message: String, condition: () -> Boolean) {
        rule.waitUntil(timeoutMillis = 5_000) { condition() }
        assertEquals(message, true, condition())
    }

    @Test
    fun auto_followsContentColor() {
        var color by mutableStateOf(Color.White)
        rule.setContent {
            TestHost {
                Box(
                    Modifier
                        .systemBars(BarContent.Auto, BarContent.Auto)
                        .fillMaxSize()
                        .drawBehind { drawRect(color) },
                )
            }
        }
        waitFor("white content → dark status icons") { statusDarkIcons() }
        waitFor("white content → dark nav icons") { navDarkIcons() }

        color = Color.Black
        waitFor("black content → light status icons") { !statusDarkIcons() }
        waitFor("black content → light nav icons") { !navDarkIcons() }
    }

    @Test
    fun auto_samplesStatusAndNavigationSeparately() {
        rule.setContent {
            TestHost {
                Column(Modifier.systemBars(BarContent.Auto, BarContent.Auto).fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black))
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color.White))
                }
            }
        }
        // top half is black → light status icons; bottom half is white → dark nav icons
        waitFor("status over black") { !statusDarkIcons() }
        waitFor("nav over white") { navDarkIcons() }
    }

    @Test
    fun declared_backgroundAndFixedTones() {
        var content by mutableStateOf<BarContent>(BarContent.Background(Color.White))
        rule.setContent {
            TestHost { Box(Modifier.fillMaxSize().systemBars(content, content)) }
        }
        waitFor("Background(white)") { statusDarkIcons() && navDarkIcons() }
        content = BarContent.Dark
        waitFor("Dark → light icons") { !statusDarkIcons() && !navDarkIcons() }
        content = BarContent.Light
        waitFor("Light → dark icons") { statusDarkIcons() && navDarkIcons() }
    }

    @Test
    fun registry_laterWins_inheritFallsThrough_andRestoresOnLeave() {
        var inner by mutableStateOf(true)
        rule.setContent {
            TestHost {
                Box(Modifier.fillMaxSize().systemBars(BarContent.Light, BarContent.Light)) {
                    if (inner) {
                        Box(Modifier.fillMaxSize().systemBars(BarContent.Dark, BarContent.Inherit))
                    }
                }
            }
        }
        waitFor("inner Dark wins on status") { !statusDarkIcons() }
        waitFor("inner Inherit falls through to outer Light on nav") { navDarkIcons() }

        inner = false
        waitFor("status restored to outer Light") { statusDarkIcons() }
    }

    @Test
    fun sampling_doesNotRecomposeContent() {
        var color by mutableStateOf(Color.White)
        var compositions = 0
        rule.setContent {
            TestHost {
                SideEffect { compositions++ }
                Box(
                    Modifier
                        .systemBars(BarContent.Auto, BarContent.Auto)
                        .fillMaxSize()
                        .drawBehind { drawRect(color) }, // state read in draw phase only
                )
            }
        }
        waitFor("initial sample") { statusDarkIcons() }
        repeat(5) {
            color = Color.Black
            waitFor("flip to light icons") { !statusDarkIcons() }
            color = Color.White
            waitFor("flip to dark icons") { statusDarkIcons() }
        }
        rule.runOnIdle { assertEquals("content must not recompose while sampling", 1, compositions) }
    }

    @Test
    fun autoWindow_readsRenderedWindow() {
        assumeTrue("PixelCopy needs API 26+", Build.VERSION.SDK_INT >= 26)
        var color by mutableStateOf(Color.White)
        rule.setContent {
            TestHost {
                Box(
                    Modifier
                        .systemBars(BarContent.AutoWindow, BarContent.AutoWindow)
                        .fillMaxSize()
                        .drawBehind { drawRect(color) },
                )
            }
        }
        waitFor("white window → dark icons") { statusDarkIcons() }
        color = Color.Black
        waitFor("black window → light icons") { !statusDarkIcons() }
    }

    /** Regression: a new [SystemBarsConfig] used to replace the controller and orphan attached nodes. */
    @Test
    fun host_configChange_keepsAttachedNodesWorking() {
        var config by mutableStateOf(SystemBarsConfig(intervalMillis = 300L))
        var content by mutableStateOf<BarContent>(BarContent.Light)
        rule.setContent {
            view = LocalView.current
            SystemBarsHost(config) { Box(Modifier.fillMaxSize().systemBars(content, content)) }
        }
        waitFor("Light → dark icons") { statusDarkIcons() }
        config = SystemBarsConfig(intervalMillis = 50L)
        rule.waitForIdle()
        content = BarContent.Dark
        waitFor("still reacts after config change") { !statusDarkIcons() }
    }

    /** A shadowed Auto node skips sampling; when the winner leaves it must take over with a fresh sample. */
    @Test
    fun shadowedAutoNode_takesOverWithFreshSample_whenWinnerLeaves() {
        var inner by mutableStateOf(true)
        rule.setContent {
            TestHost {
                Box(Modifier.systemBars(BarContent.Auto, BarContent.Auto).fillMaxSize().drawBehind { drawRect(Color.Black) }) {
                    if (inner) {
                        Box(Modifier.systemBars(BarContent.Auto, BarContent.Auto).fillMaxSize().drawBehind { drawRect(Color.White) })
                    }
                }
            }
        }
        waitFor("inner (white) wins → dark icons") { statusDarkIcons() }
        inner = false
        waitFor("outer (black) takes over → light icons") { !statusDarkIcons() }
    }

    // ---- painting: bar height is injected as 40px by TestHost, so rows 0..39 are the status bar ----

    private fun pixelAt(x: Int, y: Int): Color {
        val map = rule.onRoot().captureToImage().toPixelMap()
        return map[x, y]
    }

    private fun assertNear(message: String, expected: Color, actual: Color, tolerance: Float = 0.06f) {
        val ok = kotlin.math.abs(expected.red - actual.red) <= tolerance &&
            kotlin.math.abs(expected.green - actual.green) <= tolerance &&
            kotlin.math.abs(expected.blue - actual.blue) <= tolerance
        assertEquals("$message (expected $expected, was $actual)", true, ok)
    }

    @Test
    fun paint_none_leavesContentVisibleBehindTheBar() {
        rule.setContent { TestHost { Box(Modifier.systemBars().fillMaxSize().drawBehind { drawRect(Color.Black) }) } }
        rule.waitForIdle()
        assertNear("no paint → content", Color.Black, pixelAt(20, 10))
    }

    @Test
    fun paint_solid_paintsOnlyTheBarArea_andDecidesTheIcons() {
        rule.setContent {
            TestHost {
                Box(
                    Modifier
                        .systemBars(statusPaint = BarPaint.Solid(Color.White))
                        .fillMaxSize()
                        .drawBehind { drawRect(Color.Black) },
                )
            }
        }
        waitFor("white paint over black content → dark icons") { statusDarkIcons() }
        assertNear("bar area is painted", Color.White, pixelAt(20, 10))
        assertNear("below the bar the content is untouched", Color.Black, pixelAt(20, 200))
    }

    @Test
    fun paint_sampled_paintsTheAverageOfTheContentBehind() {
        rule.setContent {
            TestHost {
                Box(
                    Modifier
                        .systemBars(statusPaint = BarPaint.Sampled)
                        .fillMaxSize()
                        .drawBehind { drawRect(Brush.horizontalGradient(listOf(Color.Black, Color.White))) },
                )
            }
        }
        // black → white gradient averages to mid-gray; the left edge of the bar must no longer be black
        rule.waitUntil(5_000) { pixelAt(5, 10).red > 0.3f }
        assertNear("left edge is the average, not the content", Color(0.5f, 0.5f, 0.5f), pixelAt(5, 10), 0.12f)
        assertNear("right edge is the average too", Color(0.5f, 0.5f, 0.5f), pixelAt(rule.onRoot().fetchSemanticsNode().size.width - 5, 10), 0.12f)
    }

    @Test
    fun paint_onlyTheStackWinnerPaints() {
        rule.setContent {
            TestHost {
                Box(Modifier.systemBars(BarContent.Light, statusPaint = BarPaint.Solid(Color.Red)).fillMaxSize().drawBehind { drawRect(Color.Black) }) {
                    // deeper node wins the status bar, so the outer node must not paint
                    Box(Modifier.systemBars(BarContent.Dark).fillMaxSize())
                }
            }
        }
        waitFor("inner Dark wins") { !statusDarkIcons() }
        rule.waitForIdle()
        assertNear("outer does not paint while shadowed", Color.Black, pixelAt(20, 10))
    }

    /**
     * Below API 29 the system fills the navigation bar with `navigationBarColor` (the translucent scrim from
     * `enableEdgeToEdge()`). Dark content under a light scrim needs dark icons.
     */
    @Test
    fun legacyNavigationScrim_usesWindowNavigationBarColor() {
        assumeTrue("API 29+ uses the scrim model instead", Build.VERSION.SDK_INT < 29)
        rule.setContent {
            TestHost(fast.copy(scrimModel = ScrimModel.AndroidxDefault), navigationBarColor = 0xE6FFFFFF.toInt()) {
                Box(Modifier.systemBars(BarContent.Dark, BarContent.Auto).fillMaxSize().drawBehind { drawRect(Color.Black) })
            }
        }
        waitFor("light scrim over black → dark nav icons") { navDarkIcons() }
    }

    private fun midGrayOnNavigation(config: SystemBarsConfig) {
        assumeTrue("scrim applies on API 29+", Build.VERSION.SDK_INT >= 29)
        rule.setContent {
            TestHost(config) {
                Box(
                    Modifier
                        .systemBars(BarContent.Dark, BarContent.Auto)
                        .fillMaxSize()
                        .drawBehind { drawRect(Color(0xFF808080)) },
                )
            }
        }
        val threeButton = onMain {
            (androidx.core.view.ViewCompat.getRootWindowInsets(view!!)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.tappableElement())?.bottom ?: 0) > 0
        }
        assumeTrue("needs 3-button navigation", threeButton)
    }

    /**
     * 3-button navigation on API 29+ only. Mid-gray alone reads as a dark background (light icons), but
     * under the light scrim the system adds, dark icons are the readable choice.
     */
    @Test
    fun navigationScrim_flipsMidGrayToDarkIcons() {
        midGrayOnNavigation(fast.copy(scrimModel = ScrimModel.AndroidxDefault))
        waitFor("scrim model → dark nav icons over mid-gray") { navDarkIcons() }
    }

    @Test
    fun navigationScrim_disabledKeepsLightIconsOnMidGray() {
        midGrayOnNavigation(fast)
        waitFor("no scrim model → light nav icons over mid-gray") { !navDarkIcons() }
    }
}

private fun View.activity(): Activity {
    var ctx: Context = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity for view")
}