# Module systembars

edge-to-edge 앱에서 상태바 / 내비게이션바 **아이콘 명암**을 화면 배경에 맞춰 바꿉니다.
배경색을 직접 지정하거나(`BarContent.Background` 등), 실제로 그려진 픽셀을 샘플링해서(`BarContent.Auto` / `AutoWindow`) 자동으로 고릅니다.

Switches the status / navigation bar **icon appearance** to match the screen behind it in edge-to-edge apps,
either from a declared color or by sampling the pixels that were actually drawn.

시작하기 / Getting started: `enableEdgeToEdge()` → 루트에 `SystemBarsHost` → 화면에 `Modifier.systemBars(...)`.

# Package com.sonms.systembars

공개 API: `Modifier.systemBars`, `SystemBarsHost`, `SystemBarsDialog`, `BarContent`, `SystemBarsConfig`, `ScrimModel`.
Public API of the module.
