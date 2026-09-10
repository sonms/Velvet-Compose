package com.sonms.velvetcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonms.velvetcompose.modifiernode.gridOverlay
import com.sonms.velvetcompose.modifiernode.squareThumbnail

/**
 * `:modifier-node-processor` (KSP) 사용 예. 노드 정의는 `com.sonms.velvetcompose.modifiernode` 패키지에,
 * 아래에서 쓰는 `Modifier.gridOverlay(...)` / `Modifier.squareThumbnail(...)` 는 코드젠 산출물이다.
 *
 * A usage sample for the KSP codegen. The nodes live in `com.sonms.velvetcompose.modifiernode`;
 * `Modifier.gridOverlay(...)` and `Modifier.squareThumbnail(...)` used below are generated.
 */
private val lineColors = listOf(Color.White, Color.Black, Color.Cyan, Color.Yellow)
private val borderColors = listOf(Color.Magenta, Color.Green, Color(0xFFFF9800), Color.Transparent)

@Composable
fun ModifierNodeShowcase() {
    var gridVisible by remember { mutableStateOf(true) }
    var step by remember { mutableFloatStateOf(24f) }
    var side by remember { mutableFloatStateOf(200f) }
    var lineColorIndex by remember { mutableIntStateOf(0) }
    var borderColorIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                // 두 modifier 모두 코드젠으로 만들어진 확장 함수.
                // squareThumbnail: side 변경 → remeasure / border 변경 → redraw (계층 접힘)
                // gridOverlay: gridVisible=false 면 노드 미attach (@SkipWhenFalse)
                .squareThumbnail(
                    side = side.dp,
                    border = borderColors[borderColorIndex],
                )
                .gridOverlay(
                    visible = gridVisible,
                    step = step.dp,
                    lineColor = lineColors[lineColorIndex],
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF6A1B9A), Color(0xFF283593)),
                    ),
                ),
        )

        Text("grid step: ${step.toInt()}dp", style = MaterialTheme.typography.labelLarge)
        Slider(value = step, onValueChange = { step = it }, valueRange = 8f..48f)

        Text("square side: ${side.toInt()}dp", style = MaterialTheme.typography.labelLarge)
        Slider(value = side, onValueChange = { side = it }, valueRange = 120f..260f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("show grid", style = MaterialTheme.typography.labelLarge)
            Switch(checked = gridVisible, onCheckedChange = { gridVisible = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = { lineColorIndex = (lineColorIndex + 1) % lineColors.size },
            ) { Text("line color") }
            FilledTonalButton(
                onClick = { borderColorIndex = (borderColorIndex + 1) % borderColors.size },
            ) { Text("border color") }
        }
    }
}
