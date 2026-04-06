# 🎡 Velvet-Compose WheelPicker
<div style="text-align: right">

[English](README.md) | [한국어](README_KO.md)

</div>

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/wheelpicker.svg)](https://central.sonatype.com/artifact/io.github.sonms/wheelpicker)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

iOS 스타일의 3D 휠 피커를 Jetpack Compose 앱에서 사용할 수 있도록 해주는 라이브러리입니다.  
무한 스크롤, 3D 그래픽 레이어, 완전한 스타일 커스터마이징을 지원합니다.

---

## 📸 미리보기

| Vertical                                               | Horizontal                                               |
|--------------------------------------------------------|----------------------------------------------------------|
| <video src="assets/vertical_preview.mp4" width="200"/> | <video src="assets/horizontal_preview.mp4" width="200"/> |
---

## 🚀 시작하기

### Gradle

`build.gradle.kts`에 의존성을 추가하세요:

```kotlin
dependencies {
    implementation("io.github.sonms:wheelpicker:0.0.1")
}
```

---

## 📖 사용법

### 기본 사용법

```kotlin
val items = remember { (1..12).map { it.toString().padStart(2, '0') } }
val state = rememberWheelPickerState(initialIndex = 0)

VerticalWheelPicker(
    items = items,
    state = state,
    visibleItemCount = 5,
    infinite = true,
    onItemSelected = { index, item ->
        // 선택된 아이템 처리
    },
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

---

## 🎨 커스터마이징

### 스타일

`WheelPickerDefaults.style()`로 전체 스타일을 커스터마이징할 수 있습니다:

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

셀렉터 영역을 커스터마이징하는 방법은 두 가지입니다:

**1. 외부에서 직접 Box를 그려 selector를 커스터마이징**

여러 피커에 걸쳐 하나로 보이는 셀렉터를 만들고 싶을 때 사용합니다:

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

`WheelPickerState`를 사용하여 피커를 외부에서 제어할 수 있습니다:

```kotlin
val state = rememberWheelPickerState(initialIndex = 0)

// 현재 선택된 인덱스 읽기
val currentIndex = state.currentIndex

// 애니메이션 없이 이동
LaunchedEffect(Unit) {
    state.scrollToIndex(3)
}

// 애니메이션과 함께 이동
LaunchedEffect(Unit) {
    state.animateScrollToIndex(3)
}
```

---

## 📋 API 레퍼런스

### `VerticalWheelPicker`

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

### `HorizontalWheelPicker`

`VerticalWheelPicker`와 동일하나 `itemHeight` 대신 `itemWidth`를 사용합니다.

### `WheelPickerState`

| 프로퍼티 / 함수 | 설명 |
|---|---|
| `currentIndex` | 현재 선택된 아이템 인덱스 |
| `isScrollInProgress` | 스크롤 진행 중 여부 |
| `scrollToIndex(index)` | 애니메이션 없이 인덱스로 이동 |
| `animateScrollToIndex(index)` | 애니메이션과 함께 인덱스로 이동 |

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