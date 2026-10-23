package com.sonms.systembars

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 시스템 바(상태바 / 내비게이션바)의 아이콘 명암을 무엇으로 정할지 나타냅니다.
 * Describes how a system bar (status or navigation) decides its icon appearance.
 *
 * [Light] / [Dark] / [Background] 는 아이콘이 아니라 바 **뒤 배경**의 톤입니다.
 * 밝은 배경이면 어두운 아이콘, 어두운 배경이면 밝은 아이콘이 됩니다.
 *
 * [Light] / [Dark] / [Background] describe the **background** behind the bar, not the icons:
 * a light background gets dark icons and vice versa.
 */
@Immutable
public sealed interface BarContent {
    /**
     * `Modifier.systemBars` 안쪽에 그려진 Compose 내용을 작은 소프트웨어 비트맵에 다시 그려 샘플링합니다.
     * modifier 안쪽에 그려진 것만 읽으므로 `systemBars()` 를 `background()` 보다 앞에 두어야 합니다.
     *
     * Samples the Compose content drawn inside the modifier by re-drawing it into a tiny software bitmap.
     * Only what is drawn *inside* the modifier is read, so place `systemBars()` before `background()`.
     */
    public data object Auto : BarContent

    /**
     * 실제 윈도우를 `PixelCopy` 로 샘플링합니다 (API 26 이상, 그 미만에서는 동작하지 않음).
     * `SurfaceView` / 지도 / 비디오처럼 Compose 밖에서 그려진 영역일 때 사용합니다.
     * 샘플에 시스템 스크림이 이미 포함되므로 스크림 보정은 적용하지 않습니다.
     *
     * Samples the real window via `PixelCopy` (API 26+; no-op below). Use it when the area behind the bar
     * is drawn outside Compose (`SurfaceView`, maps, video). The sample already contains the system scrim,
     * so no scrim correction is applied.
     */
    public data object AutoWindow : BarContent

    /**
     * 바 뒤 배경이 [color] 라고 선언합니다 (직접 지정한 색이나 이미지 팔레트에서 추출한 색).
     * The background behind the bar is [color] (declared, or extracted e.g. from an image palette).
     *
     * @param color 바 뒤 배경색 / Background color behind the bar
     */
    public data class Background(val color: Color) : BarContent

    /**
     * 배경이 밝다고 선언합니다 → 어두운 아이콘.
     * The background is light → dark icons.
     */
    public data object Light : BarContent

    /**
     * 배경이 어둡다고 선언합니다 → 밝은 아이콘.
     * The background is dark → light icons.
     */
    public data object Dark : BarContent

    /**
     * 판단하지 않고 스택에서 바로 아래 항목을 따릅니다. 상태바 / 내비바마다 따로 적용됩니다.
     * Defers to the entry below this one in the stack, per bar.
     */
    public data object Inherit : BarContent
}
