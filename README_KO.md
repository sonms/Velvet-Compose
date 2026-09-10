<div style="text-align: right">

[English](README.md) | [한국어](README_KO.md)

</div>

# 🎡 Velvet-Compose

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

iOS 감성의 부드럽고 아름다운 Jetpack Compose 컴포넌트 라이브러리입니다.  
완전한 커스터마이징을 지원하는 실크처럼 부드러운 UI 컴포넌트를 제공합니다.

---

## 📦 컴포넌트

| 컴포넌트 | Maven Central | 설명 |
|---|---|---|
| 🎡 **WheelPicker** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/wheelpicker.svg)](https://central.sonatype.com/artifact/io.github.sonms/wheelpicker) | 무한 스크롤을 지원하는 iOS 스타일 3D 휠 피커 |
| ⭐ **RatingBar** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/ratingbar.svg)](https://central.sonatype.com/artifact/io.github.sonms/ratingbar) | 스프링 애니메이션과 햅틱 피드백을 지원하는 커스터마이징 가능한 별점 바 |

## 🧩 도구

| 모듈 | Maven Central | 설명 |
|---|---|---|
| 🧩 **Modifier.Node Codegen** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/modifier-node-processor.svg)](https://central.sonatype.com/artifact/io.github.sonms/modifier-node-processor) | `Modifier.Node` 에서 `ModifierNodeElement` 와 `Modifier` 확장 함수를 생성하는 KSP 프로세서 |

---

## 🚀 시작하기

### Gradle

```kotlin
dependencies {
    implementation("io.github.sonms:wheelpicker:0.0.2")
    implementation("io.github.sonms:ratingbar:0.0.2")
}
```

---

## 🎡 WheelPicker

Jetpack Compose를 위한 iOS 스타일 3D 휠 피커입니다.  
무한 스크롤, 3D 그래픽 레이어, 완전한 스타일 커스터마이징을 지원합니다.

### 📸 미리보기

| Vertical | Horizontal |
|---|---|
| <video src="assets/vertical_preview.mp4" width="200"/> | <video src="assets/horizontal_preview.mp4" width="200"/> |

### 기본 사용법

```kotlin
val items = remember { (1..12).map { it.toString().padStart(2, '0') } }
val state = rememberWheelPickerState(initialIndex = 0)

VerticalWheelPicker(
    items = items,
    state = state,
    visibleItemCount = 5,
    infinite = true,
    onItemSelected = { index, item -> },
) { item, isSelected ->
    Text(
        text = item,
        fontSize = if (isSelected) 20.sp else 16.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.Black else Color.Gray,
    )
}
```

### 타임피커 샘플 (AM/PM + 시 + 분)

```kotlin
@Composable
fun TimePickerSample() {
    val amPmItems = remember { listOf("AM", "PM") }
    val hourItems = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }

    val amPmState = rememberWheelPickerState(initialIndex = 0)
    val hourState = rememberWheelPickerState(initialIndex = 0)
    val minuteState = rememberWheelPickerState(initialIndex = 0)

    val itemHeight = 48.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // 3개 피커에 걸쳐 하나로 보이는 커스텀 셀렉터
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(itemHeight)
                .background(
                    color = Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                )
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerticalWheelPicker(
                items = amPmItems,
                state = amPmState,
                modifier = Modifier.width(80.dp),
                itemHeight = itemHeight,
                visibleItemCount = 5,
                infinite = false,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                ),
            ) { item, isSelected ->
                Text(
                    text = item,
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    color = if (isSelected) Color.Black else Color.Gray,
                )
            }

            VerticalWheelPicker(
                items = hourItems,
                state = hourState,
                modifier = Modifier.width(80.dp),
                itemHeight = itemHeight,
                visibleItemCount = 5,
                infinite = true,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                ),
            ) { item, isSelected ->
                Text(
                    text = item,
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    color = if (isSelected) Color.Black else Color.Gray,
                )
            }

            VerticalWheelPicker(
                items = minuteItems,
                state = minuteState,
                modifier = Modifier.width(80.dp),
                itemHeight = itemHeight,
                visibleItemCount = 5,
                infinite = true,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                ),
            ) { item, isSelected ->
                Text(
                    text = item,
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    color = if (isSelected) Color.Black else Color.Gray,
                )
            }
        }
    }
}
```

### 커스터마이징

```kotlin
VerticalWheelPicker(
    items = items,
    style = WheelPickerDefaults.style(
        selector = WheelPickerDefaults.selectorStyle(
            background = Color.Gray.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            showDivider = true,
            dividerColor = Color.Gray,
            dividerThickness = 1.dp,
        ),
        fade = WheelPickerDefaults.fadeStyle(
            fraction = 0.3f,
            enabled = true,
        ),
        transform = WheelPickerDefaults.transformStyle(
            rotationEnabled = true,
            maxRotationDegree = 30f,
            scaleEnabled = true,
            minScale = 0.85f,
            alphaEnabled = true,
            minAlpha = 0.7f,
        ),
    ),
) { item, isSelected -> }
```

### 셀렉터 커스터마이징

**1. 외부에서 직접 Box를 그려 커스터마이징**

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(itemHeight)
        .background(
            color = Color.Gray.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
        )
)
```

**2. `WheelPickerDefaults.selectorStyle`을 통해 내장 selector 사용**

```kotlin
VerticalWheelPicker(
    style = WheelPickerDefaults.style(
        selector = WheelPickerDefaults.selectorStyle(
            background = Color.Gray.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            showDivider = true,
            dividerColor = Color.Gray,
            dividerThickness = 1.dp,
        ),
    ),
) { item, isSelected -> }
```

### 상태 제어

```kotlin
val state = rememberWheelPickerState(initialIndex = 0)

val currentIndex = state.currentIndex

LaunchedEffect(Unit) {
    state.scrollToIndex(3)
    state.animateScrollToIndex(3)
}
```

### API 레퍼런스

#### `VerticalWheelPicker`

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `items` | `List<T>` | 필수 | 표시할 아이템 목록 |
| `modifier` | `Modifier` | `Modifier` | Modifier |
| `state` | `WheelPickerState` | `rememberWheelPickerState()` | 상태 홀더 |
| `itemHeight` | `Dp` | `48.dp` | 각 아이템의 높이 |
| `visibleItemCount` | `Int` | `5` | 화면에 보이는 아이템 수 (홀수 권장) |
| `infinite` | `Boolean` | `true` | 무한 스크롤 활성화 여부 |
| `style` | `WheelPickerStyle` | `WheelPickerDefaults.style()` | 스타일 설정 |
| `onItemSelected` | `(Int, T) -> Unit` | `{}` | 아이템 선택 완료 콜백 |
| `itemContent` | `@Composable (T, Boolean) -> Unit` | 필수 | 아이템 UI 슬롯 |

#### `HorizontalWheelPicker`

`VerticalWheelPicker`와 동일하나 `itemHeight` 대신 `itemWidth`를 사용합니다.

#### `WheelPickerState`

| 프로퍼티 / 함수 | 설명 |
|---|---|
| `currentIndex` | 현재 선택된 아이템 인덱스 |
| `isScrollInProgress` | 스크롤 진행 중 여부 |
| `scrollToIndex(index)` | 애니메이션 없이 인덱스로 이동 |
| `animateScrollToIndex(index)` | 애니메이션과 함께 인덱스로 이동 |

---

## ⭐ RatingBar

Jetpack Compose를 위한 고도로 커스터마이징 가능한 별점 바입니다.  
스프링 애니메이션, 햅틱 피드백, 반개 지원, 커스텀 아이콘 슬롯 API를 제공합니다.

### 📸 미리보기

| 기본 별 | 커스텀 아이콘 |
|---|---|
| <video src="assets/ratingbar_star_preview.mp4" width="200"/> | <video src="assets/ratingbar_custom_preview.mp4" width="200"/> |

### 기본 사용법

```kotlin
// 읽기 전용
RatingBar(
    rating = 3.5f,
)

// 인터랙티브
var rating by remember { mutableStateOf(3.5f) }
RatingBar(
    rating = rating,
    onRatingChanged = { rating = it },
)
```

### 커스텀 아이콘 (슬롯 API)

```kotlin
RatingBar(
    rating = 3.5f,
    onRatingChanged = { rating = it },
) { index, fraction ->
    Icon(
        imageVector = if (fraction > 0f) Icons.Filled.Favorite
                      else Icons.Outlined.FavoriteBorder,
        tint = if (fraction > 0f) Color.Red else Color.Gray,
        contentDescription = null,
    )
}
```

### 커스터마이징

```kotlin
RatingBar(
    rating = 3.5f,
    maxRating = 5,
    stepSize = StepSize.HALF,
    style = RatingBarDefaults.style(
        filledColor = Color.Yellow,
        emptyColor = Color.Gray,
        itemSize = 32.dp,
        itemSpacing = 4.dp,
        // 스프링 애니메이션 (null로 끄기 가능)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        // 햅틱 피드백 on/off
        hapticFeedbackEnabled = true,
    ),
    onRatingChanged = { rating = it },
)
```

### API 레퍼런스

#### `RatingBar`

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `rating` | `Float` | 필수 | 현재 별점 값 (0f ~ maxRating) |
| `modifier` | `Modifier` | `Modifier` | Modifier |
| `maxRating` | `Int` | `5` | 최대 아이템 개수 |
| `stepSize` | `StepSize` | `StepSize.HALF` | 단계 크기 (FULL 또는 HALF) |
| `style` | `RatingBarStyle` | `RatingBarDefaults.style()` | 스타일 설정 |
| `onRatingChanged` | `((Float) -> Unit)?` | `null` | 별점 변경 콜백. null이면 읽기 전용 |
| `itemContent` | `@Composable (Int, Float) -> Unit` | - | 커스텀 아이콘 슬롯 (선택) |

#### `StepSize`

| 값 | 설명 |
|---|---|
| `StepSize.FULL` | 1.0 단위로 선택 |
| `StepSize.HALF` | 0.5 단위로 선택 |

---

## 🧩 Modifier.Node Codegen

`Modifier.Node` 만 작성하면 `ModifierNodeElement` 와 `Modifier` 확장 함수는 KSP 가 생성한다.
`composed { }` 에 머물게 만드는 보일러플레이트 벽을 없앤다.

> `Modifier.Node` 를 직접 작성하는 사람(라이브러리 저자 등)을 위한 도구다.
> 1인 유지보수 · SLA 없음 · API 호환성 보장 없음.
> 설계 배경: [`ARCHITECTURE.md`](ARCHITECTURE.md) · `composed` 와의 비교: [`COMPARISON.md`](modifier-node-sample/COMPARISON.md)

### 설정

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    // 어노테이션은 전부 SOURCE retention 이라 compileOnly 로 충분하다 — APK 에 들어가지 않는다.
    compileOnly("io.github.sonms:modifier-node-annotations:0.0.1")
    ksp("io.github.sonms:modifier-node-processor:0.0.1")
}
```

**두 좌표 모두 필요하다.** `ksp(...)` 는 프로세서 클래스패스만 채우기 때문에 어노테이션은
별도로 컴파일 클래스패스에 올려야 한다. Room 이나 Moshi 와 같은 형태다.

### 사용법

노드를 평소대로 작성하고 어노테이션을 붙인다. 파라미터는 주 생성자의 `var` 프로퍼티 파라미터여야 한다.

```kotlin
@ModifierNodeFactory(name = "debugTint")
internal class DebugTintNode(
    @Invalidates(Draw) var color: Color,
) : Modifier.Node(), DrawModifierNode {

    // @Invalidates 를 쓰면 필수 — 아래 설명 참고.
    override val shouldAutoInvalidate: Boolean get() = false

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(color)
    }
}
```

생성물 (`DebugTintElement.kt`):

```kotlin
public fun Modifier.debugTint(color: Color): Modifier = this.then(DebugTintElement(color))

internal class DebugTintElement(private val color: Color) : ModifierNodeElement<DebugTintNode>() {
    override fun create() = DebugTintNode(color)
    override fun update(node: DebugTintNode) {
        if (node.color != color) { node.color = color; node.invalidateDraw() }
    }
    override fun InspectorInfo.inspectableProperties() { /* name + properties */ }
    override fun equals(other: Any?): Boolean { /* 필드 단위 */ }
    override fun hashCode(): Int { /* 필드 단위 */ }
}
```

이후엔 여느 modifier 처럼 쓴다:

```kotlin
Box(modifier = Modifier.debugTint(Color.Red.copy(alpha = 0.2f)))
```

### 어노테이션

| 어노테이션 | 대상 | 하는 일 |
|---|---|---|
| `@ModifierNodeFactory(name, visibility)` | Node 클래스 | 생성 트리거. `name` 을 비우면 클래스명에서 `Node` 접미사를 떼어 유도한다. `visibility` 는 확장 함수의 가시성이며, Element 는 항상 `internal` |
| `@Invalidates(vararg InvalidationScope)` | `var` 파라미터 | 이 파라미터가 바뀔 때 무효화할 범위. 미지정 시 노드가 구현한 모든 범위. `None` 은 무효화 없음 |
| `@SkipWhenFalse` / `@SkipWhenTrue` | `Boolean` 파라미터 | 조건 불충족 시 생성 함수가 `this` 를 반환한다 — 노드를 아예 attach 하지 않는다. 여럿이면 OR |
| `@OnChange` | `var` 파라미터 | 값이 바뀌면 생성된 `update()` 가 노드의 `fun on<Name>Changed()` 를 호출한다. 코루틴 재구독이나 effect 재시작에 쓴다 |

`InvalidationScope` 는 `Measure`, `Placement`, `Draw`, `Semantics`, `ParentData`, `None`.
생성된 `update()` 는 계층을 접는다 — `Measure > Placement > Draw` 중 가장 상위 하나만 호출된다.

### `shouldAutoInvalidate = false`

Compose 는 `update()` 직후 노드의 모든 capability 를 자동 무효화한다. 이 override 가 없으면
`@Invalidates` 의 파라미터 단위 제어가 no-op 이 되므로, 프로세서가 선언을 강제한다:

```kotlin
override val shouldAutoInvalidate: Boolean get() = false
```

지원되는 문서화된 API 이며, Compose 자체 `graphicsLayer` / `paint` 모디파이어가 같은 패턴을 쓴다.

### 한계

- 생성 함수에 default argument 를 재현하지 못한다 (KSP 가 기본값 표현식을 못 읽는다) — 호출부에서 전 인자를 넘겨야 한다.
- 지원하는 노드 인터페이스는 `DrawModifierNode` 와 `LayoutModifierNode` 뿐이다.
- 제네릭 Node 와 다중 타입 파라미터는 검증되지 않았다.
- `equals` 는 파라미터 타입의 `equals` 안정성에 의존한다. 함수 타입은 경고하지만 그 밖의 불안정 타입은 사용자 책임이다.

---

## 📄 라이센스

```
Copyright 2026 sonms

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```