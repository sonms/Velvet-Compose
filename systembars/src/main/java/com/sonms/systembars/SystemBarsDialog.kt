package com.sonms.systembars

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A [Dialog] with its own [SystemBarsHost], since dialogs are separate windows and do not see the
 * Activity's registry. Pass `decorFitsSystemWindows = false` in [properties] for an edge-to-edge dialog.
 */
@Composable
public fun SystemBarsDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    config: SystemBarsConfig = SystemBarsConfig(),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        SystemBarsHost(config = config, content = content)
    }
}
