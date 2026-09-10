<div style="text-align: right">

[English](README.md) | [한국어](README_KO.md)

</div>

# 🎡 Velvet-Compose

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

Smooth & beautiful Jetpack Compose component library inspired by iOS.  
Velvet provides silky-smooth UI components with full customization support.

---

## 📦 Components

| Component | Maven Central | Description |
|---|---|---|
| 🎡 **WheelPicker** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/wheelpicker.svg)](https://central.sonatype.com/artifact/io.github.sonms/wheelpicker) | iOS-style 3D wheel picker with infinite scroll |
| ⭐ **RatingBar** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/ratingbar.svg)](https://central.sonatype.com/artifact/io.github.sonms/ratingbar) | Customizable rating bar with spring animation & haptic feedback |

## 🧩 Tooling

| Module | Maven Central | Description |
|---|---|---|
| 🧩 **Modifier.Node Codegen** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.sonms/modifier-node-processor.svg)](https://central.sonatype.com/artifact/io.github.sonms/modifier-node-processor) | KSP processor that generates `ModifierNodeElement` + `Modifier` extension from a `Modifier.Node` |

---

## 🚀 Getting Started

### Gradle

```kotlin
dependencies {
    implementation("io.github.sonms:wheelpicker:0.0.2")
    implementation("io.github.sonms:ratingbar:0.0.2")
}
```

---

## 🎡 WheelPicker

iOS-style 3D wheel picker for Jetpack Compose.  
Supports infinite scrolling, 3D graphic layers, and full style customization.

### 📸 Preview

| Vertical | Horizontal |
|---|---|
| <video src="assets/vertical_preview.mp4" width="200"/> | <video src="assets/horizontal_preview.mp4" width="200"/> |

### Basic Usage

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

### Time Picker Sample (AM/PM + Hour + Minute)

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
        // Custom selector spanning all three pickers
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

### Customization

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

### Selector Customization

**1. Draw a custom Box externally**

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

**2. Use the built-in selector via `WheelPickerDefaults.selectorStyle`**

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

### State Control

```kotlin
val state = rememberWheelPickerState(initialIndex = 0)

val currentIndex = state.currentIndex

LaunchedEffect(Unit) {
    state.scrollToIndex(3)
    state.animateScrollToIndex(3)
}
```

### API Reference

#### `VerticalWheelPicker`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `items` | `List<T>` | required | List of items to display |
| `modifier` | `Modifier` | `Modifier` | Modifier |
| `state` | `WheelPickerState` | `rememberWheelPickerState()` | State holder |
| `itemHeight` | `Dp` | `48.dp` | Height of each item |
| `visibleItemCount` | `Int` | `5` | Number of visible items (odd recommended) |
| `infinite` | `Boolean` | `true` | Enable infinite scrolling |
| `style` | `WheelPickerStyle` | `WheelPickerDefaults.style()` | Style configuration |
| `onItemSelected` | `(Int, T) -> Unit` | `{}` | Callback when item is settled |
| `itemContent` | `@Composable (T, Boolean) -> Unit` | required | Item UI slot |

#### `HorizontalWheelPicker`

Same as `VerticalWheelPicker` but with `itemWidth` instead of `itemHeight`.

#### `WheelPickerState`

| Property / Function | Description |
|---|---|
| `currentIndex` | Currently selected item index |
| `isScrollInProgress` | Whether scrolling is in progress |
| `scrollToIndex(index)` | Scroll to index without animation |
| `animateScrollToIndex(index)` | Scroll to index with animation |

---

## ⭐ RatingBar

Highly customizable RatingBar for Jetpack Compose.  
Features spring animation, haptic feedback, half-step support, and custom icon slot API.

### 📸 Preview

| Default Star | Custom Icon |
|---|---|
| <video src="assets/ratingbar_star_preview.mp4" width="200"/> | <video src="assets/ratingbar_custom_preview.mp4" width="200"/> |

### Basic Usage

```kotlin
// Read-only
RatingBar(
    rating = 3.5f,
)

// Interactive
var rating by remember { mutableStateOf(3.5f) }
RatingBar(
    rating = rating,
    onRatingChanged = { rating = it },
)
```

### Custom Icon (Slot API)

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

### Customization

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
        // Spring animation (null to disable)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        // Haptic feedback on/off
        hapticFeedbackEnabled = true,
    ),
    onRatingChanged = { rating = it },
)
```

### API Reference

#### `RatingBar`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `rating` | `Float` | required | Current rating value (0f ~ maxRating) |
| `modifier` | `Modifier` | `Modifier` | Modifier |
| `maxRating` | `Int` | `5` | Maximum number of items |
| `stepSize` | `StepSize` | `StepSize.HALF` | Step size (FULL or HALF) |
| `style` | `RatingBarStyle` | `RatingBarDefaults.style()` | Style configuration |
| `onRatingChanged` | `((Float) -> Unit)?` | `null` | Callback when rating changes. null for read-only |
| `itemContent` | `@Composable (Int, Float) -> Unit` | - | Custom icon slot (optional) |

#### `StepSize`

| Value | Description |
|---|---|
| `StepSize.FULL` | Select in increments of 1.0 |
| `StepSize.HALF` | Select in increments of 0.5 |

---

## 🧩 Modifier.Node Codegen

Write the `Modifier.Node` — KSP generates the `ModifierNodeElement` and the `Modifier` extension
function for you. It removes the boilerplate wall that keeps people on `composed { }`.

> Developer tooling for people who write `Modifier.Node` by hand (library authors).
> Single maintainer, no SLA, no API compatibility guarantee.
> Design notes: [`ARCHITECTURE.md`](ARCHITECTURE.md) · vs `composed`: [`COMPARISON.md`](modifier-node-sample/COMPARISON.md)

### Setup

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    // Annotations are SOURCE retention, so compileOnly is enough — they never reach your APK.
    compileOnly("io.github.sonms:modifier-node-annotations:0.0.1")
    ksp("io.github.sonms:modifier-node-processor:0.0.1")
}
```

**Both coordinates are required.** `ksp(...)` only populates the processor classpath, so the
annotations have to be on your compile classpath separately — the same shape as Room or Moshi.

### Usage

Write the node as usual and annotate it. Parameters must be `var` property parameters of the
primary constructor.

```kotlin
@ModifierNodeFactory(name = "debugTint")
internal class DebugTintNode(
    @Invalidates(Draw) var color: Color,
) : Modifier.Node(), DrawModifierNode {

    // Required whenever you use @Invalidates — see below.
    override val shouldAutoInvalidate: Boolean get() = false

    override fun ContentDrawScope.draw() {
        drawContent()
        drawRect(color)
    }
}
```

Generated for you (`DebugTintElement.kt`):

```kotlin
public fun Modifier.debugTint(color: Color): Modifier = this.then(DebugTintElement(color))

internal class DebugTintElement(private val color: Color) : ModifierNodeElement<DebugTintNode>() {
    override fun create() = DebugTintNode(color)
    override fun update(node: DebugTintNode) {
        if (node.color != color) { node.color = color; node.invalidateDraw() }
    }
    override fun InspectorInfo.inspectableProperties() { /* name + properties */ }
    override fun equals(other: Any?): Boolean { /* per-field */ }
    override fun hashCode(): Int { /* per-field */ }
}
```

Then call it like any other modifier:

```kotlin
Box(modifier = Modifier.debugTint(Color.Red.copy(alpha = 0.2f)))
```

### Annotations

| Annotation | Target | What it does |
|---|---|---|
| `@ModifierNodeFactory(name, visibility)` | Node class | Triggers generation. `name` defaults to the class name minus `Node`. `visibility` applies to the extension function — the Element is always `internal` |
| `@Invalidates(vararg InvalidationScope)` | `var` parameter | Which scopes to invalidate when this parameter changes. Defaults to every scope the node implements. `None` means no invalidation |
| `@SkipWhenFalse` / `@SkipWhenTrue` | `Boolean` parameter | The generated function returns `this` when the condition fails — the node is never attached. Multiple markers are OR-joined |
| `@OnChange` | `var` parameter | The generated `update()` calls the node's `fun on<Name>Changed()` when the value changes. Use it to re-subscribe a coroutine or restart an effect |

`InvalidationScope` is `Measure`, `Placement`, `Draw`, `Semantics`, `ParentData` or `None`.
The generated `update()` folds the hierarchy — `Measure > Placement > Draw`, only the highest one fires.

### `shouldAutoInvalidate = false`

Compose auto-invalidates every capability of a node right after `update()`. Without this override,
per-parameter `@Invalidates` control is a no-op, so the processor requires the declaration:

```kotlin
override val shouldAutoInvalidate: Boolean get() = false
```

It is a supported, documented API — Compose's own `graphicsLayer` and `paint` modifiers use the same pattern.

### Limitations

- Default arguments are not reproduced on the generated function (KSP cannot read default expressions) — pass every argument at the call site.
- Supported node interfaces are `DrawModifierNode` and `LayoutModifierNode` only.
- Generic nodes and multiple type parameters are unverified.
- `equals` relies on your parameter types having stable `equals`. Function-type parameters produce a warning; other unstable types are on you.

---

## 📄 License

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