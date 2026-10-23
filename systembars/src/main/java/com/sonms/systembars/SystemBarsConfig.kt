package com.sonms.systembars

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 3버튼 내비게이션 뒤에 시스템이 깔아 주는 반투명 스크림의 근사값입니다.
 * 실제 값은 플랫폼 내부 값이라 기기에 따라 다르면 직접 지정하세요.
 *
 * Approximation of the translucent scrim the system draws behind 3-button navigation.
 * The real values are platform internals, so supply your own if a device differs.
 *
 * @param lightScrim 어두운 아이콘일 때 깔리는 밝은 스크림 / Light scrim shown under dark icons
 * @param darkScrim 밝은 아이콘일 때 깔리는 어두운 스크림 / Dark scrim shown under light icons
 */
@Immutable
public class ScrimModel(public val lightScrim: Color, public val darkScrim: Color) {
    /** [ScrimModel] 기본값 모음 / Default [ScrimModel] values. */
    public companion object {
        /** androidx `SystemBarStyle` 기본값과 같습니다. / Matches androidx `SystemBarStyle` defaults. */
        public val AndroidxDefault: ScrimModel =
            ScrimModel(lightScrim = Color(0xE6FFFFFF), darkScrim = Color(0x801B1B1B))
    }

    /** 두 스크림 색이 모두 같으면 같은 모델입니다. / Equal when both scrim colors are equal. */
    override fun equals(other: Any?): Boolean =
        other is ScrimModel && lightScrim == other.lightScrim && darkScrim == other.darkScrim

    /** [equals] 와 일관된 해시 / Hash consistent with [equals]. */
    override fun hashCode(): Int = 31 * lightScrim.hashCode() + darkScrim.hashCode()
}

/**
 * [SystemBarsHost] 의 동작을 조정합니다.
 * Tuning for [SystemBarsHost].
 *
 * @param intervalMillis 샘플 사이의 최소 간격(ms). 낮출수록 반응이 빠르지만 스크롤 중 프레임 비용이 늘어납니다.
 * / Minimum time between pixel samples; lower reacts faster but costs more frame time while scrolling.
 * @param lowThreshold 이 휘도보다 낮아지면 밝은 배경에서 어두운 배경으로 전환합니다 (0..1).
 * / Luminance below which a light background flips to a dark one.
 * @param highThreshold 이 휘도보다 높아지면 어두운 배경에서 밝은 배경으로 전환합니다 (0..1). 두 임계값 사이에서는 직전 값을 유지합니다(히스테리시스).
 * / Luminance above which a dark background flips to a light one; between the two the previous value is kept.
 * @param contrastEnforced `Window.isNavigationBarContrastEnforced` 값 (API 29+).
 * / Value for `Window.isNavigationBarContrastEnforced` (API 29+).
 * @param scrimModel 3버튼 내비바 보정에 쓰는 스크림. `null` 이면 보정을 끕니다. [contrastEnforced] 가 true 일 때만 적용됩니다.
 * / Scrim used to correct the 3-button navigation bar; `null` disables it. Applies only when [contrastEnforced].
 */
@Immutable
public data class SystemBarsConfig(
    val intervalMillis: Long = 300L,
    val lowThreshold: Float = 0.45f,
    val highThreshold: Float = 0.55f,
    val contrastEnforced: Boolean = true,
    val scrimModel: ScrimModel? = ScrimModel.AndroidxDefault,
) {
    init {
        require(lowThreshold <= highThreshold) { "lowThreshold must be <= highThreshold" }
    }
}
