# ARCHITECTURE — modifier-node-codegen

> 브랜치: `feat/modifier-node-codegen` — `master` 미병합. 갈래 B 진행 중 (§8·§9).
> 스택: Kotlin 2.0.21 · AGP 8.13.2 · Compose BOM 2026.04.01 (ui 1.12.0-beta02) · KSP 2.0.21-1.0.28 · KotlinPoet 1.18.1
> 모듈: `:modifier-node-annotations` · `:modifier-node-processor` · `:modifier-node-sample`(예제+테스트, 미배포)
> 라이브 데모: `:app` → "Modifier.Node" 탭 (`ModifierNodeShowcase.kt`, 노드는 `com.sonms.velvetcompose.modifiernode`)

사용법은 [`modifier-node-annotations/README.md`](modifier-node-annotations/README.md),
`composed` 와의 비교는 [`modifier-node-sample/COMPARISON.md`](modifier-node-sample/COMPARISON.md).

---

## 1. 문제

`Modifier` 확장 함수를 만들 때 흔히 `Modifier.composed { }` 를 쓴다. `composed` 는:

- 반환 Modifier 가 `equals`/`hashCode` 미구현 → modifier 체인 재사용 비교 실패 → 불필요한 재구성/무효화
- 매 재구성마다 람다 re-materialize
- composition 밖에서 사용 불가

AndroidX 권장은 `Modifier.Node` 이며 신규 코드에서 `composed` 사용을 지양한다.
`Modifier.Node` 로 옮기면 위 문제는 사라지지만 **보일러플레이트**가 생긴다:
`ModifierNodeElement` 서브클래스 + `equals`/`hashCode`/`update`/`inspectableProperties` 수동 작성.

---

## 2. 이 프로젝트가 하는 것

`@ModifierNodeFactory` 를 붙인 **concrete `Modifier.Node` 서브클래스**로부터 KSP 가
`ModifierNodeElement` 와 `Modifier` 확장 함수를 생성한다.

파라미터의 단일 원천은 Node 주 생성자의 `var` 프로퍼티 파라미터다.
(초안의 별도 `NodeBase` 상속 방식은 이름 규약/동기화 문제 때문에 버림.)
`create()` 는 생성자 호출, `update()` 는 `var` 재대입 + 무효화.

```kotlin
@ModifierNodeFactory(name = "debugTint")
internal class DebugTintNode(
    @Invalidates(Draw) var color: Color,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false   // §5

    override fun ContentDrawScope.draw() { drawContent(); drawRect(color) }
}
```

생성물 (`DebugTintElement.kt` 한 파일):

- `public fun Modifier.debugTint(color: Color): Modifier = this.then(DebugTintElement(color))`
- `internal class DebugTintElement : ModifierNodeElement<DebugTintNode>()`
  — `create` / `update` / `equals` / `hashCode` / `inspectableProperties`

사용자는 Node 의 **행동 코드(그리기/측정/포인터)만** 쓴다.

---

## 3. 어노테이션

| 어노테이션 | 대상 | 하는 일 |
|---|---|---|
| `@ModifierNodeFactory(name, visibility)` | Node 클래스 | 생성 트리거. `name` 비우면 클래스명(`-Node`)에서 유도. `visibility` 는 생성 **함수**의 가시성(기본 `Public`) — 생성 Element 는 항상 `internal` |
| `@Invalidates(vararg InvalidationScope)` | `var` 파라미터 | 변경 시 무효화 범위. 미지정 → 노드가 구현한 모든 범위. `None` → 무효화 없음. **사용 시 Node 에 `shouldAutoInvalidate = false` 강제**(§5) |
| `@SkipWhenFalse` / `@SkipWhenTrue` | `Boolean` 파라미터 | 조건 시 생성 함수가 `return this` — 노드 미attach. 여럿이면 OR (`if (!a \|\| b) return this`) |
| `@OnChange` | `var` 파라미터 | 값 변경 시 생성 `update()` 가 노드의 `fun on<Name>Changed()` 호출(재구독·effect 재시작용). 해당 함수 선언 필수 |

`InvalidationScope` 계층: `Measure` > `Placement` > `Draw` (상위 하나만 호출).
`Semantics` / `ParentData` 는 MVP 미지원, 지정 시 에러.

생성되는 `update()` (계층 접기 예):

```kotlin
override fun update(node: FixedSquareNode) {
    var redraw = false; var remeasure = false
    if (node.side != side) { node.side = side; remeasure = true }
    if (node.overlay != overlay) { node.overlay = overlay; redraw = true }
    if (remeasure) node.invalidateMeasurement() else if (redraw) node.invalidateDraw()
}
```

---

## 4. `shouldAutoInvalidate` — `@Invalidates` 가 한 줄을 요구하는 이유

> 재조사 비용을 아끼기 위한 사실 기록. `androidx.compose.ui` 1.12.0-beta02 소스 직접 확인.

Compose 는 `NodeChain.updateNode()` → `autoInvalidateUpdatedNode()` 에서 `update()` 직후
노드의 **모든** capability 를 무효화한다. 유일한 opt-out:

```kotlin
// NodeKind.kt (autoInvalidateNodeSelf)
if (phase == Updated && !node.shouldAutoInvalidate) return
```

- `Modifier.Node.shouldAutoInvalidate` (Modifier.kt): `open val ... get() = true`. **`@Deprecated` 아니다.**
  (바이트코드의 `getShouldAutoInvalidate$annotations()` 합성 메서드에 `Deprecated: true` 가 붙어 있으나
  이는 Kotlin 컴파일러 아티팩트 — `getNode$annotations()` 도 동일. override 시 경고 없음.)
- KDoc 이 직접 안내: *"You may choose to set this to `false` … you must call the appropriate
  invalidate functions manually when the modifier is updated."*
- Compose 자체 사용처: `SimpleGraphicsLayerModifier`(= `Modifier.graphicsLayer{}`),
  `PainterNode`(= `Modifier.paint`), `FocusTargetNode`.

즉 `@Invalidates` 의 파라미터 단위 제어가 실효를 가지려면 Node 가
`override val shouldAutoInvalidate: Boolean get() = false` 를 선언해야 한다.
codegen 은 Node 클래스를 수정 못 하므로 `@Invalidates` 사용 시 이 선언을 요구한다(없으면 KSP 에러).

`AutoInvalidateProbeTest` 로 동작 고정: `shouldAutoInvalidate=false` 노드는 draw 전용 파라미터
변경 시 remeasure 하지 않고, measure 파라미터 변경 시엔 `update()` 의 `invalidateMeasurement()` 로 remeasure.

---

## 5. 가치와 한계

### 실질 가치

1. **보일러플레이트 제거** — `equals`/`hashCode`/`create`/`update`/`inspectableProperties`,
   modifier 당 ~30~55줄. `GeneratedElementTest` 로 계약 고정.
2. **equals 안정성 보장** — 파라미터 불변 시 `update()` 자체가 안 불림(`Modifier.Node` 성능의 대부분).
   codegen 이 정확한 `equals` 를 생성하므로 사용자가 실수로 깨뜨릴 여지가 없다.
3. **ABI 표면 축소** — Element 는 항상 `internal`, 공개는 `Modifier.foo()` 함수뿐.
4. `@Invalidates` scope ↔ 노드 인터페이스 **컴파일 타임 검증**.
5. `@SkipWhen*` / `@OnChange` — `composed` 의 조건부 적용 / `remember(key)` 재구독을 마커로 대체.

### 파라미터 단위 invalidation 의 성능 이득은 좁다

micro-optimization 이다. Compose 조차 극소수 modifier(graphicsLayer/paint/focus)에만 적용했다.
`Modifier.Node` 성능의 대부분은 equals 스킵이고 그건 `@Invalidates` 없이도 얻는다.
`@Invalidates` 의 이득은 "파라미터가 자주 바뀌는데 그 변경이 draw 만 요구하는" 경우에 한정.
**주된 가치는 1·2·3 이다.**

### 한계

- 생성 함수에 default argument 재현 불가 (KSP 가 기본값 표현식을 못 읽음). 호출부에서 전 인자 전달.
  → `@Default(String)` 검토 완료(부록 A), 구현 보류.
- 지원 노드 인터페이스 2개: `DrawModifierNode`, `LayoutModifierNode`.
- 제네릭 Node, 다중 타입 파라미터 미검증.
- `equals` 는 참조형 파라미터의 `equals` 안정성에 의존. 함수 타입은 프로세서가 경고,
  그 밖의 불안정 타입은 사용자 책임.
- 하드 에러(non-var, 잘못된 마커, `@OnChange` 콜백 누락) 시 해당 노드 생성물을 안 만든다(2차 에러 방지).
- 프로세서 자체 단위 테스트 없음 — `:modifier-node-sample` 생성물 + Robolectric 로 간접 검증.

### 포지셔닝

`Modifier.Node` 를 직접 작성하는 사람(라이브러리 저자 등)만 대상 — 니치가 좁다.
Velvet-Compose 내부 인프라 + `Modifier.Node` 학습 목적. 1인 유지보수, SLA·API 호환성 보장 없음.

---

## 6. 비목표

- `composed` → `Node` **자동 변환기.** `composed` 블록은 임의의 `@Composable` 코드(서브 컴포지션,
  복잡한 `remember` 키, effect 의존성)를 담을 수 있어 범용 기계 변환 불가.
- **노드 행동 코드 생성.** 그리기/측정/포인터 로직은 사용자가 쓴다.
- **런타임 람다 DSL** (`Modifier.node { onDraw { } }` 류). 람다 캡처 → `equals` 불안정 → `composed` 문제 재발.
- **범용 OSS 확장.**

---

## 7. `composed` vs Node 비교 (요약)

세부는 `COMPARISON.md`. 손으로 쓴 코드 줄 수:

| 부류 | composed | Node+codegen | codegen 없이 |
|---|---:|---:|---:|
| 값(파라미터/CompositionLocal) 읽고 draw/measure 반응 | ~7 | ~10 | ~55 |
| interaction / coroutine / animation | ~10 | ~35 | ~85 |
| composition-free 체인(`graphicsLayer` 등) | — | 손해 | 손해 |

- **값 읽기 부류**: 결정적. codegen 이 보일러플레이트 벽을 없애 `composed` 를 벗어나게 해줌.
- **interaction 부류**: `@OnChange` 로 격차 축소. 남는 장황함(`Animatable`/카운팅)은 진짜 노드 코드.
- **composition-free 체인**: `graphicsLayer{}.drawWithCache{}` 처럼 이미 `Modifier.Node` 기반이고
  `equals` 도 되는 체인을 raw 노드로 내리면 레이어 lifecycle 수동 관리 + 캐싱 상실 = 순 손해.
  `fadingEdge` dogfood 가 이 케이스 (`FadingEdge.kt`).

---

## 8. 구현 상태

**갈래 B 확정** (§9). 아래 완료:

- [x] KSP 프로세서: Element + 확장 함수 생성, invalidation 계층 접기, 검증
- [x] `@ModifierNodeFactory(visibility)` + Element `internal` 강제 (`generated_element_is_internal`)
- [x] `@Invalidates` + `shouldAutoInvalidate` 강제 (§4, `AutoInvalidateProbeTest`)
- [x] `@SkipWhenFalse` / `@SkipWhenTrue` (`skip_markers_gate_application`)
- [x] `@OnChange` (`onChange_fires_only_when_param_changes`)
- [x] 예제 + dogfood(`fadingEdge`) + `composed` 비교 2케이스
- [x] `modifier-node-annotations/README.md`, `:modifier-node-sample` 정리
- [x] `:app` 라이브 데모 — `gridOverlay`(`@SkipWhenFalse`+`@Invalidates(Draw)`), `squareThumbnail`(Measure/Draw 계층 접힘)

- [x] 배포 설정 — `:modifier-node-annotations` / `:modifier-node-processor` 에
  `mavenPublishing` (`io.github.sonms`, 0.0.1), `publish.yml` 태그·테스트 태스크 분기, CI 조립 추가

**보류/미완:**

- `master` 병합 + 태그 푸시 (= 실제 Maven Central 릴리스) — 미실행
- `@Default` — 검토만(부록 A), 구현 보류
- `master` 병합 전: 커밋 squash 여부
- (범위 밖) 노드 인터페이스 확장, default 대신 오버로드 체인, IDE inspection, 프로세서 단위 테스트

---

## 9. 판단 근거 (갈래 B)

명제: **"`Modifier.Node` 보일러플레이트 제거 + equals 안정성 보장 + (opt-in) 파라미터 단위 invalidation"**.
성능은 부차 셀링포인트로만, 과장 없이 (§5).

착수 전 세운 판단 기준과 결과:

| 기준 | 결과 |
|---|---|
| 재작성이 원본보다 낫거나 동등 | 대체로 충족 — 값 읽기 명백한 이득, interaction 은 `@OnChange` 이후 근접, composition-free 는 손해(§7) |
| invalidation 계약이 손보다 명확한 이점 | 조건부 충족 — 작동하고(§4) KSP 가 강제, 테스트로 고정. 단 성능 이득 범위가 좁다(§5) |
| KSP 구현 비용이 시간 단위 | 충족 — 전체 반나절 |

대안: **A 중단**(니치·이득이 미미하다고 보면 유효했음), **C 목표 전환**("Modifier.Node 를 쉽게" — 노드
유틸/delegation/테스트, 작업량 큼). 둘 다 미채택.

---

## 부록 A — `@Default(String)` 검토 (구현 보류)

```
@Default("1000") var durationMillis: Int   →   fun Modifier.x(durationMillis: Int = 1000, ...)
```

- **구현 ~10줄**: 어노테이션 문자열을 `ParameterSpec.defaultValue("%L", expr)` 로 verbatim 방출.
  생성 함수에만, Element 생성자는 그대로.
- **되는 것**: 리터럴(`1000`/`0f`/`true`/`null`/`""`), 파라미터 타입의 멤버(`Color.Unspecified` 등).
- **안 되는 것**: 생성 파일에 import 안 된 심볼. `4.dp` 도 실패(자주 씀).
  → FQN 강제거나 유닛 allowlist(`.dp/.sp/.em` 자동 import) 필요.
- **리스크**: 오타 → 생성 파일에서 컴파일 에러(어노테이션 위치 아님).
  중간 위치 `@Default` → 뒤 파라미터 named 강제(경고만, 리오더 안 함).
- **판정**: "default 없음"보다 낫고 오버로드 체인보다 단순.
  권장 형태 = `@Default(String)` + non-blank 검증 + `.dp/.sp/.em` allowlist + 중간위치 경고 (~20줄).

## 부록 B — 조사 이력

`@Invalidates` 의 파라미터 단위 invalidation 을 두고 한 차례 **"현행 Compose 에서 불가능"** 이라
잘못 결론냈다가 정정했다. 원인: `shouldAutoInvalidate` 를 `@Deprecated` 로 오독(§4 참고).
소스를 직접 확인해 opt-out 이 정상 API 이고 Compose 자체가 쓰는 패턴임을 확인 → §4 로 정리.
교훈: deprecation 여부는 바이트코드 attribute 가 아니라 소스/`@Deprecated` 어노테이션으로 확인.
