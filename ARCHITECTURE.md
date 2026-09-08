# ARCHITECTURE — Modifier.Node codegen (실험 브랜치)

> 브랜치: `feat/modifier-node-codegen`
> 상태: MVP 검증 단계. 아래 "결정 게이트"를 통과하기 전까지 `master` 병합 없음.

---

## 1. 문제

Compose에서 `Modifier` 확장 함수를 만들 때 지금까지 `Modifier.composed { }` 를 써 왔다.
`composed` 의 구조적 문제:

- 반환 Modifier가 `equals`/`hashCode` 를 구현하지 않음 → modifier 재사용 비교가 항상 실패 → 불필요한 재구성/무효화
- 매 재구성마다 람다 re-materialize
- composition 밖에서 사용 불가, `Modifier.Companion` 정적 팩토리로 만들 수 없음
- AndroidX가 `Modifier.Node` 로 전면 이관 중이며 `composed` 는 deprecate 예고 상태

`Modifier.Node` 로 옮기면 위 문제가 해결되지만 **보일러플레이트**가 생긴다:
`ModifierNodeElement` 서브클래스 + `Modifier.Node` 서브클래스 +
`equals` / `hashCode` / `update` / `InspectorInfo` 수동 작성.

---

## 2. 이 프로젝트가 하는 것

`@ModifierNodeFactory` 어노테이션이 붙은 **`Modifier.Node` 서브클래스**로부터
`ModifierNodeElement` 레이어와 공개 확장 함수를 KSP로 생성한다.

> 설계 변경 (초안 대비): 별도 `NodeBase` 를 생성하고 사용자가 상속하는 대신,
> **사용자가 concrete Node 클래스를 직접 작성하고 거기에 어노테이션을 단다.**
> 파라미터의 단일 원천이 Node의 주 생성자(`var` 프로퍼티 파라미터)가 되어
> 이름 규약/동기화 문제가 사라진다. `create` 는 생성자 호출, `update` 는 `var` 재대입.

```kotlin
@ModifierNodeFactory(name = "shimmer")
internal class ShimmerNode(
    @Invalidates(Draw) var color: Color,
    @Invalidates(Measure) var thickness: Dp,
    var durationMillis: Int,
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() { /* color, thickness 사용 */ }
}
```

생성물 (`ShimmerElement.kt` 한 파일):

- `internal fun Modifier.shimmer(color, thickness, durationMillis): Modifier = this.then(ShimmerElement(...))`
- `internal class ShimmerElement : ModifierNodeElement<ShimmerNode>()` — `create` / `update` / `equals` / `hashCode` / `inspectableProperties`

사용자가 직접 작성하는 것:

- Node 클래스의 그리기/측정/포인터 **행동 코드만**

---

## 3. 만드는 이유 (가치 명제)

InspectorInfo/`update` 자동화만 놓고 보면 절약량은 파일당 10~20줄이며
data class Element 로도 대충 흉내 낼 수 있다. **그 자체는 프로젝트의 이유가 아니다.**

실제 이유는 두 가지다:

### 3.1 컴파일 타임에 검증되는 invalidation 계약

- `@Invalidates(Measure)` 인데 노드가 `LayoutModifierNode` 를 구현하지 않으면 KSP 에러
- 파라미터가 바뀌었을 때 어떤 invalidate가 호출되는지 생성 코드 + 테스트 헬퍼로 고정
- 손으로 짤 때 흔한 두 실수를 원천 차단:
  - 과잉: 무엇이 바뀌든 `invalidateMeasurement()`
  - 누락: 필드 대입만 하고 invalidate 안 함

생성되는 `update` 형태:

```kotlin
override fun update(node: ShimmerNode) {
    var redraw = false; var remeasure = false
    if (node.color != color) { node.color = color; redraw = true }
    if (node.thickness != thickness) { node.thickness = thickness; remeasure = true }
    if (node.durationMillis != durationMillis) node.durationMillis = durationMillis
    if (remeasure) node.invalidateMeasurement()   // 계층상 redraw 포함
    else if (redraw) node.invalidateDraw()
}
```

invalidation 계층: `Measure` > `Placement` > `Draw` (상위 하나만 호출),
`Semantics` / `ParentData` 는 직교(별도 호출).

무지정 파라미터 기본 정책: **안전 우선** — 노드가 구현한 모든 capability를 invalidate.
`@Invalidates(...)` 로 좁히고, `@Invalidates(None)` 으로 완전히 뺀다
(create 시점에만 쓰거나 `observeReads` 로 반응적으로 읽는 값).

### 3.2 바이너리 호환성 (라이브러리 저자용)

- Element/Node 클래스를 `internal` 로 감추고 `Modifier.foo(...)` 함수만 공개
  → 내부 리팩터가 ABI를 깨지 않음, `.api` diff가 예측 가능
- (opt-in) `@ModifierNodeFactory(binaryCompat = true)`:
  Kotlin default argument 대신 명시적 오버로드 체인 생성
  → 파라미터 추가 = 심볼 추가, 기존 심볼 유지

---

## 4. 비목표 (Non-goals)

- **`composed` → `Node` 자동 변환기.** `composed` 블록은 임의의 `@Composable` 코드를
  담을 수 있어(서브 컴포지션, 복잡한 `remember` 키, effect 의존성) 범용 기계 변환이 불가능하다.
- **노드 행동 코드 생성.** 그리기/측정/포인터 로직은 사용자가 직접 쓴다.
  codegen은 Element 레이어 + 필드 선언까지만.
- **런타임 람다 DSL** (`Modifier.node { onDraw { } }` 류). 람다 캡처 → `equals` 불안정 →
  `composed` 문제 재발.
- **범용 OSS로의 확장.** 아래 "포지셔닝" 참고.

---

## 5. 포지셔닝 / 솔직한 한계

- **니치가 좁다.** `Modifier.Node` 를 직접 작성하는 사람은 라이브러리 저자 / 디자인시스템
  팀 정도이며, 그중 이 codegen을 dependency로 받을 사람은 더 적다.
  → 이 프로젝트는 **Velvet-Compose 내부 인프라 + `Modifier.Node` 학습 + 포트폴리오**가 목적이다.
  공개는 하되 "많은 사용자" 목표는 없다.
- **절약량이 크지 않다.** trivial한 부분(InspectorInfo/update 대입)만 보면 파일당 10~20줄.
  가치는 3.1 / 3.2 에 있고 거기서 안 나오면 접는다.
- **`Modifier.Node` API 안정성.** 코어(`ModifierNodeElement`, `Modifier.Node`,
  `DrawModifierNode`, `LayoutModifierNode`, `invalidateDraw/Measurement/Placement`)는
  현행 Compose에서 stable. MVP 착수 시 실제 사용 API 목록을 뽑아 각각
  stable/experimental 태그를 확인하고, experimental이 섞이면 그 부분만 opt-in으로 격리한다.
- **1인 유지보수.** SLA 없음. 이슈 생기면 대응, API 호환성 보장 안 함. README에 명시.

---

## 6. MVP 스코프

- `@ModifierNodeFactory(name)` (Node 클래스에 부착) + `@Invalidates(vararg Scope)` (생성자 파라미터에 부착)
  (`Scope = Measure | Placement | Draw | Semantics | ParentData | None`)
- KSP 프로세서:
  - `<Name>Element` 생성: `create` / `update` / `equals` / `hashCode` / `inspectableProperties`
  - 공개 확장 함수 생성
  - 검증: `@Invalidates` scope ↔ 노드가 구현한 인터페이스 정합성, `var` 파라미터 강제
- 지원 노드 인터페이스 **2개만**: `DrawModifierNode`, `LayoutModifierNode`
- invalidation 계층 접기 로직 (`Measure` > `Placement` > `Draw`)
- 테스트 헬퍼 1개: "파라미터 P를 바꾸면 `invalidateDraw` 만 호출되고 `invalidateMeasurement` 는
  호출되지 않는다" 를 검증  ← **아직 미작성**
- 모듈: `:modifier-node-annotations`, `:modifier-node-processor`, `:modifier-node-sample`
- **Dogfooding: `:modifier-node-sample` 안에서 `fadingEdge` 재작성.**
  ratingbar / wheelpicker 컴포넌트 본체에 이 codegen dependency를 연결하지 않는다
  (의존성이 실험 모듈에 몰리는 것 방지). `fadingEdge` 는 sample 모듈 격리 예제로만 사용.
  ※ `fadingEdge` 는 `graphicsLayer { compositingStrategy = Offscreen }` 를 필요로 하므로
    순수 `DrawModifierNode` 로는 완전 재현 불가 — GraphicsLayer 위임이 필요하고 이는
    결정 게이트에서 "노드 행동 코드는 codegen이 안 도와준다" 를 실증하는 좋은 케이스.

### 진행 상태 (2026-09-08)

- [x] 모듈 3개 스캐폴딩, 카탈로그/settings 배선
- [x] 어노테이션 정의
- [x] 프로세서: Element + 확장 함수 생성, invalidation 계층 접기, `var`/scope 검증
- [x] `:modifier-node-sample` 예제 2개(`debugTint` draw 전용, `fixedSquare` draw+layout) 빌드/코드젠 확인
- [ ] invalidation 계약 테스트 헬퍼
- [ ] `fadingEdge` 재작성
- [ ] 기본값(default argument) 처리 — 현재 미지원, 호출부에서 전 인자 전달 필요

### 한계 (MVP 구현상)

- 생성 함수에 default argument 재현 불가 (KSP가 기본값 표현식을 못 읽음). 전 인자 필수.
- 제네릭 Node, 다중 타입 파라미터 미검증.
- `equals`/`hashCode` 는 참조형 파라미터의 `equals` 안정성에 의존 (람다/불안정 타입 넣으면 그대로 깨짐 — 이는 사용자 책임).

### 범위 밖 (MVP 이후)

- 노드 인터페이스 확장 (`PointerInputModifierNode`, `SemanticsModifierNode`, `GlobalPositionAwareModifierNode` ...)
- `binaryCompat = true` 오버로드 체인
- Element/Node `internal` 은닉 옵션
- IDE inspection / quick-fix

---

## 7. 결정 게이트

MVP 완료 후 아래를 근거로 **계속 / 중단**을 판단한다.

**계속 조건 (아래를 모두 만족):**

1. 재작성한 `fadingEdge` 가 원본보다 읽기 쉽거나 최소한 동등하다 (보일러플레이트가
   실제로 사라졌고, 노드 행동 코드는 그대로 명확하다).
2. `@Invalidates` invalidation 계약이 **손으로 짤 때보다 명확한 이점**을 준다:
   - scope ↔ 인터페이스 불일치를 실제로 컴파일 타임에 잡아낸다
   - 테스트 헬퍼로 "이 파라미터는 redraw만" 을 고정할 수 있고, 그게 회귀 방지에 유용하다고 느껴진다
3. KSP 프로세서 구현/디버깅 비용이 감당 가능한 수준이었다 (며칠 단위가 아니라 시간 단위).

**중단 판단 (아래 중 하나라도):**

- 재작성 결과가 원본과 비슷하거나 더 장황하다.
- invalidation 계약이 "있으면 좋지만 없어도 그만" 수준이고, 검증 에러가 실전에서 안 걸린다.
- KSP 쪽에서 예상 못 한 제약(타입 해석, 제네릭 노드, cross-module 가시성)으로 스코프가 계속 샌다.

중단 시: `Modifier.Node` / KSP 를 저비용으로 학습한 것으로 간주하고 브랜치를 아카이브한다.

---

## 8. 참고

- 프로젝트: Kotlin 2.0.21, AGP 8.13.2, Compose BOM 2026.04.01
- KSP 버전은 Kotlin 2.0.21 에 정렬 (`2.0.21-1.0.x`)
- 첫 dogfooding 대상: `wheelpicker/src/main/java/com/sonms/wheelpicker/extension/ModifierExt.kt`
