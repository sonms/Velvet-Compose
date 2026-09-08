# modifier-node-sample

`modifier-node-codegen` 의 예제 + 통합 테스트 모듈. **배포하지 않는다.**
This is the examples + integration test module for `modifier-node-codegen`. **Not published.**

## 구성

| 파일 | 내용 |
|---|---|
| `DebugTint.kt` | 가장 단순한 예 — draw 전용 노드 |
| `FixedSquare.kt` | draw + layout 노드 — `update()` 계층 접기 |
| `FadingEdge.kt` | dogfood. `graphicsLayer` 오프스크린 + `DstIn` 마스크. composition-free 체인을 raw 노드로 내렸을 때의 손익을 보여주는 케이스 (`../ARCHITECTURE.md` §9) |
| `compare/AccentOverlay.kt` | `composed` vs Node — CompositionLocal 읽기 (Case A) |
| `compare/PressScale.kt` | `composed` vs Node — InteractionSource + 애니메이션 (Case B) |
| `COMPARISON.md` | 위 두 케이스의 before/after 분석 |

## 테스트

- `GeneratedElementTest` — equals/hashCode/create/update 계약, Element `internal` 가시성 (순수 JUnit)
- `InvalidationContractTest` — equals 스킵, `@SkipWhen*` 가드, `@OnChange` 콜백 (Robolectric)
- `AutoInvalidateProbeTest` — `shouldAutoInvalidate = false` opt-out 동작 통제 실험 (Robolectric)
