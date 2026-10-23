package com.sonms.systembars

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 자체 [SystemBarsHost] 를 가진 [Dialog] 입니다. 다이얼로그는 별도 윈도우라서 Activity 의 레지스트리를 보지 못합니다.
 * 바 뒤까지 그리려면 [properties] 에 `decorFitsSystemWindows = false` 를 넘기세요.
 *
 * A [Dialog] with its own [SystemBarsHost], since dialogs are separate windows and do not see the
 * Activity's registry. Pass `decorFitsSystemWindows = false` in [properties] to draw behind the bars.
 *
 * @param onDismissRequest 닫힘 요청 콜백 / Called when the dialog should be dismissed
 * @param properties 다이얼로그 속성 / Dialog properties
 * @param config 간격 / 임계값 / 스크림 설정 / Interval, thresholds and scrim settings
 * @param content 다이얼로그 내용 / Dialog content
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
