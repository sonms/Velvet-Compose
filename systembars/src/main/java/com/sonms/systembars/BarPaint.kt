package com.sonms.systembars

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 시스템 바 영역을 노드가 직접 칠할지, 무엇으로 칠할지 나타냅니다. 기본은 [None] (칠하지 않음, 투명).
 * Whether the node paints the system bar area itself, and with what. The default is [None] (transparent).
 *
 * 칠하기는 `Window.statusBarColor` 같은 윈도우 속성이 아니라, 노드가 콘텐츠 위 바 영역에 사각형을 한 번 더 그리는 방식입니다.
 * 그래서 모든 API 레벨에서 같게 동작하고, 색을 애니메이션할 수 있습니다. 바 영역과 겹치는 노드 범위에서만 칠해지며,
 * 해당 바를 **결정하는(스택에서 이기는) 노드만** 칠합니다.
 *
 * Painting draws a rectangle over the content in the bar area (not a window property like `Window.statusBarColor`),
 * so it behaves the same on every API level and the color can be animated. It only paints where the node overlaps
 * the bar, and only the node that currently decides that bar (the stack winner) paints.
 */
@Immutable
public sealed interface BarPaint {
    /** 칠하지 않습니다. 바 뒤에는 콘텐츠가 그대로 비칩니다. / Nothing is painted; the content shows through. */
    public data object None : BarPaint

    /**
     * 샘플링한 바 뒤 콘텐츠의 평균색으로 단색 칠합니다. [BarContent.Auto] 와 함께 쓸 때만 적용됩니다
     * (`AutoWindow` 는 칠한 색이 다시 샘플에 섞여 갱신되지 않으므로 무시됩니다).
     * 색은 샘플 간격([SystemBarsConfig.intervalMillis])마다 갱신됩니다.
     *
     * Paints a solid band of the sampled average color behind the bar. Only applies together with [BarContent.Auto]
     * (ignored for `AutoWindow`, whose window sample would include the paint itself and never update).
     * The color refreshes every [SystemBarsConfig.intervalMillis].
     */
    public data object Sampled : BarPaint

    /**
     * 지정한 [color] 로 칠합니다. [BarContent.Auto] 와 함께 쓰면 샘플링 없이 이 색으로 아이콘 명암을 정합니다
     * (샘플링 비용이 없고, `animateColorAsState` 로 부드럽게 바꿀 수 있습니다). 불투명한 색을 권장합니다.
     *
     * Paints the bar with [color]. With [BarContent.Auto] the icons are chosen from this color without any sampling
     * (no sampling cost; animate it with `animateColorAsState`). An opaque color is recommended.
     *
     * @param color 바를 칠할 색 / Color to paint the bar with
     */
    public data class Solid(val color: Color) : BarPaint
}
