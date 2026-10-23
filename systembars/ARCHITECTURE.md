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
| 샘플링 | 기본: `Modifier.Node` 가 콘텐츠를 `GraphicsLayer` 에 기록 → 바 높이 띠만 다운샘플해 휘도 계산. 폴백: `PixelCopy` (API 26 미만 비활성) |
| 시점 | 스로틀(기본 ~10Hz) + 히스테리시스(기본 0.45 / 0.55). 간격·임계값 모두 파라미터 |
| API | `Modifier.systemBars(status, navigation)` , 각각 `BarContent` = `Auto` \| `Background(Color)` \| `Light` \| `Dark` \| `Inherit` |
| 중재 | 루트 `SystemBarsHost` + 스택형 레지스트리. 나중에 등록된 항목이 이기고, 해제되면 이전 항목으로 복귀. `Inherit` 는 아래 항목을 따른다 |
| 다이얼로그 | `Dialog` 헬퍼 제공 (별도 윈도우). 컴포넌트 래퍼는 범위 밖 |
| 3버튼 내비 | `tappableElement` 인셋으로 감지. `contrastEnforced=true` 기본. `scrimModel`(기본: androidx 근사값, `null` 이면 비활성) |
| edge-to-edge | 사용자가 `enableEdgeToEdge()` 호출. 미호출 시 디버그 경고 로그 |
| 후속 | Navigation 연동은 `:systembars-navigation` 어댑터 모듈 (어댑터 → 코어 단방향) |

### 스크림 순환 의존 해결

시스템은 아이콘 명암에 따라 스크림 색을 고르므로 "스크림이 깔린 결과"로 아이콘을 고르면 순환이다.
두 후보(어두운 아이콘+밝은 스크림, 밝은 아이콘+어두운 스크림)를 각각 합성해 **대비율이 큰 쪽**을 고른다.
스크림 색/알파는 플랫폼 내부 값이므로 근사값이며 `ScrimModel` 로 교체 가능.
전환 깜빡임 방지를 위해 현재 선택보다 `SWITCH_MARGIN` 배 이상 좋아질 때만 바꾼다.

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
