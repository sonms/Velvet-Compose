package com.sonms.velvetcompose

import android.graphics.Color as AndroidColor
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.sonms.systembars.BarContent
import com.sonms.systembars.SystemBarsDialog
import com.sonms.systembars.systemBars

/**
 * `:systembars` 라이브 데모. 상태바/내비바 아이콘이 화면 배경에 따라 바뀌는지 눈으로 확인한다.
 * 바 뒤까지 그려야 하므로 `Scaffold` 의 innerPadding 없이 전체 화면을 쓴다.
 *
 * Live demo for `:systembars` — watch the status/navigation bar icons follow the screen behind them.
 */
private enum class BarsDemo(val label: String) {
    SCROLL("Auto 스크롤"),
    DECLARED("지정/추출 색"),
    DIALOG("Dialog"),
    SURFACE("PixelCopy"),
}

private val bandColors = listOf(
    Color.White, Color(0xFF101010), Color(0xFF1E88E5), Color(0xFFFFEB3B), Color(0xFFE91E63), Color(0xFFF5F5F5),
)

@Composable
fun SystemBarsShowcase(onBack: () -> Unit) {
    var demo by remember { mutableStateOf(BarsDemo.SCROLL) }

    Box(Modifier.fillMaxSize()) {
        when (demo) {
            BarsDemo.SCROLL -> ScrollDemo()
            BarsDemo.DECLARED -> DeclaredDemo()
            BarsDemo.DIALOG -> DialogDemo()
            BarsDemo.SURFACE -> SurfaceDemo()
        }

        // 컨트롤은 바 영역을 피해 안쪽에 둔다 (statusBarsPadding / navigationBarsPadding).
        Column(Modifier.statusBarsPadding().padding(horizontal = 8.dp)) {
            TextButton(onClick = onBack) { Text("← 돌아가기") }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BarsDemo.entries.forEach { d ->
                FilterChip(selected = demo == d, onClick = { demo = d }, label = { Text(d.label) })
            }
        }
    }
}

/** 흰/검정/파랑/노랑/그라데이션 띠를 스크롤하면서 `Auto` 가 아이콘을 뒤집는지 본다. */
@Composable
private fun ScrollDemo() {
    LazyColumn(Modifier.systemBars(BarContent.Auto, BarContent.Auto).fillMaxSize()) {
        itemsIndexed(List(30) { it }) { i, _ ->
            val c = bandColors[i % bandColors.size]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (i % 7 == 6) 220.dp else 140.dp)
                    .background(if (i % 7 == 6) Brush.verticalGradient(listOf(Color.White, Color.Black)) else Brush.linearGradient(listOf(c, c))),
                contentAlignment = Alignment.Center,
            ) {
                Text("#$i", color = if (c.luminanceIsLow()) Color.White else Color.Black)
            }
        }
    }
}

/** `Background(color)` 직접 지정. "추출 색"은 이미지 팔레트에서 뽑은 값을 같은 자리에 넘기면 된다. */
@Composable
private fun DeclaredDemo() {
    var color by remember { mutableStateOf(Color(0xFF1E88E5)) }
    Box(
        Modifier
            .systemBars(BarContent.Background(color), BarContent.Background(color))
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            bandColors.forEach { c ->
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(c)
                        .clickable { color = c },
                )
            }
        }
    }
}

/** 다이얼로그는 별도 윈도우라 자체 호스트를 쓰는 `SystemBarsDialog` 로 연다. */
@Composable
private fun DialogDemo() {
    var show by remember { mutableStateOf(false) }
    Box(
        // systemBars 를 background 보다 앞에 둬야 Auto 가 배경까지 샘플링한다.
        Modifier.systemBars(BarContent.Auto, BarContent.Auto).fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = { show = true }) { Text("다이얼로그 열기") }
    }
    if (show) {
        SystemBarsDialog(
            onDismissRequest = { show = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(
                Modifier
                    .systemBars(BarContent.Dark, BarContent.Dark)
                    .fillMaxSize()
                    .background(Color(0xFF101010))
                    .clickable { show = false },
                contentAlignment = Alignment.Center,
            ) { Text("어두운 다이얼로그 (탭하면 닫힘)", color = Color.White) }
        }
    }
}

/** `SurfaceView` 는 Compose 레이어가 못 읽으므로 `AutoWindow`(PixelCopy). 탭하면 흰/검정이 바뀐다. */
@Composable
private fun SurfaceDemo() {
    var dark by remember { mutableStateOf(false) }
    val argb = (if (dark) Color.Black else Color.White).toArgb()
    Box(
        Modifier
            .systemBars(BarContent.AutoWindow, BarContent.AutoWindow)
            .fillMaxSize()
            .clickable { dark = !dark },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) = Unit
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) = paint(h, tag as? Int ?: AndroidColor.WHITE)
                        override fun surfaceDestroyed(h: SurfaceHolder) = Unit
                    })
                }
            },
            update = { sv ->
                sv.tag = argb
                if (sv.holder.surface.isValid) paint(sv.holder, argb)
            },
        )
        Text("SurfaceView — 탭하면 색 전환", Modifier.align(Alignment.Center), color = if (dark) Color.White else Color.Black)
    }
}

private fun paint(holder: SurfaceHolder, argb: Int) {
    val canvas = holder.lockCanvas() ?: return
    try { canvas.drawColor(argb) } finally { holder.unlockCanvasAndPost(canvas) }
}

private fun Color.luminanceIsLow(): Boolean = (red * 0.2126f + green * 0.7152f + blue * 0.0722f) < 0.4f
