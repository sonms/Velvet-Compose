# ARCHITECTURE — systembars

> 브랜치: `feat/systembars`. 모듈: `:systembars` (Android 전용, minSdk 24, `io.github.sonms:systembars`).
> 목적: edge-to-edge 앱에서 상태바/내비바 **아이콘 명암**을 화면 배경에 맞춰 자동으로 바꾼다.

---

## 1. 문제

`enableEdgeToEdge()` 는 바를 투명하게 만들 뿐, 화면마다 다른 배경 위에서 아이콘 명암을 맞춰 주지 않는다.

- `SystemBarStyle.auto()` : 시스템 다크모드만 본다. 화면별 배경은 모른다.
- Accompanist `SystemUiController` : deprecated.
- 블로그/gist 스니펫 : 재사용 가능한 라이브러리가 아니다.

## 2. 결정 사항

| 영역 | 결정 |
|---|---|
| 감지 | 직접 지정(색/추출 색) + 자동 픽셀 샘플링 둘 다 |
| 샘플링 | 기본: `Modifier.Node` 가 평소엔 `drawContent()` 만 하고, 샘플 시점에만 노드 캔버스를 32×8 소프트웨어 `ImageBitmap` 으로 잠깐 바꿔 바 영역을 한 번 더 그린다(동기, GPU read-back 없음). 폴백: `PixelCopy` (API 26 미만 비활성) |
| 시점 | 스로틀(기본 ~10Hz) + 히스테리시스(기본 0.45 / 0.55). 간격·임계값 모두 파라미터 |
| API | `Modifier.systemBars(status, navigation)` , 각각 `BarContent` = `Auto` \| `AutoWindow` \| `Background(Color)` \| `Light` \| `Dark` \| `Inherit` |
| 중재 | 루트 `SystemBarsHost` + 스택형 레지스트리. 나중에 등록된 항목이 이기고, 해제되면 이전 항목으로 복귀. `Inherit` 는 아래 항목을 따른다 |
| 다이얼로그 | `Dialog` 헬퍼 제공 (별도 윈도우). 컴포넌트 래퍼는 범위 밖 |
| 3버튼 내비 | `tappableElement` 인셋으로 감지. `contrastEnforced=true` 기본. `scrimModel`(기본: androidx 근사값, `null` 이면 비활성) |
| edge-to-edge | 사용자가 `enableEdgeToEdge()` 호출. 미호출 시 디버그 경고 로그 |
| 후속 | Navigation 연동은 `:systembars-navigation` 어댑터 모듈 (어댑터 → 코어 단방향) |

> `Auto` 는 노드 안쪽에서 그려진 Compose 콘텐츠만 읽는다(**`systemBars()` 를 `background()` 보다 앞에 둘 것**, blur/RenderEffect 같은 하드웨어 전용 효과는 반영되지 않음). `AutoWindow` 는 `PixelCopy`(API 26+, `SurfaceView`/지도/비디오용).
> Compose 밖에서 그려진 영역은 자동으로 알 수 없어서 폴백은 **명시적 옵트인** 이다. `AutoWindow` 의 샘플에는 시스템 스크림이 이미 포함되므로 스크림 모델은 적용하지 않는다.
> `Light`/`Dark`/`Background` 는 바 **뒤 배경**의 톤이다 (Light 배경 → 어두운 아이콘).

### 스크림 순환 의존 해결

시스템은 아이콘 명암에 따라 스크림 색을 고르므로 "스크림이 깔린 결과"로 아이콘을 고르면 순환이다.
두 후보(어두운 아이콘+밝은 스크림, 밝은 아이콘+어두운 스크림)를 각각 합성해 **대비율이 큰 쪽**을 고른다.
스크림 색/알파는 플랫폼 내부 값이므로 근사값이며 `ScrimModel` 로 교체 가능.
전환 깜빡임 방지를 위해 현재 선택보다 `SWITCH_MARGIN` 배 이상 좋아질 때만 바꾼다.

### API 28 실기기에서 확인된 것 (SM-G950N)

- `GraphicsLayer.record { drawContent() }` 는 `drawContent()` 가 레이어로 들어가지 않았고, 레이어를 다른 레이어에 축소해서 그리면 빈 이미지가 나왔다. → `GraphicsLayer` 경로를 버리고 캔버스 교체 방식으로 변경.
- 중첩된 노드는 **자식이 부모보다 먼저 attach** 된다. 등록 순서만으로는 깊은 쪽이 이길 수 없어서, 첫 레이아웃 뒤 `parentLayoutCoordinates` 로 깊이를 구해 우선순위로 쓴다(깊이 → 등록 순).
- `PixelCopy` 는 draw 중 요청하면 직전 프레임을 복사한다. 샘플 뒤 50ms+ 후 한 번 더 확인한다. 앱이 `enableEdgeToEdge()` 를 안 하면 시스템이 그린 바 배경이 샘플에 섞인다.
- 라이브러리의 androidTest APK 는 targetSdk 가 minSdk(24) 로 잡혀 기기가 "이전 버전용 앱" 으로 차단할 수 있어 `testOptions.targetSdk = 36` 으로 올렸다.

## 3. 리컴포지션 안전

- 샘플링 결과(`isLight`)는 Compose `State` 로 읽지 않는다. 호스트가 윈도우 컨트롤러에 직접 적용한다.
- modifier 는 `Modifier.Node` (`composed` 미사용).
- 검증: ① Compose 컴파일러 메트릭(`build/compose_metrics`)에서 공개 컴포저블이 `restartable skippable`, 파라미터 타입이 `stable` ② 계측 테스트의 컴포지션 횟수 프로브 ③ 데모에서 Layout Inspector.

## 4. 레이어

```
internal/ (순수 Kotlin, JVM 단위 테스트)
  Luma            휘도 / 대비율 / 알파 합성
  IconDecider     히스테리시스 + 스크림 대비 선택
  BarRegistry     스택형 레지스트리 + Inherit 해석
public
  BarContent, ScrimModel
  Modifier.systemBars, SystemBarsHost, Dialog 헬퍼   (이후 단계)
```

## 5. 테스트

1. JVM 단위: 휘도, 히스테리시스, 스크림 대비, 레지스트리
2. 계측(Compose): `GraphicsLayer` 샘플링, 가짜 인셋 주입, 컴포지션 횟수
3. `:app` 데모 탭: 스크롤/그라데이션/이미지/다이얼로그/`SurfaceView`(PixelCopy)
