package com.sonms.velvetcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * :modifier-node-processor 는 KSP 기반 Android/JVM 전용 코드젠이라 iOS 타겟에는 붙지 않는다.
 * The KSP codegen behind ModifierNodeShowcase only runs on the Android/JVM target.
 */
@Composable
actual fun ModifierNodeShowcaseHost() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Modifier.Node showcase is Android-only (KSP codegen).")
    }
}
