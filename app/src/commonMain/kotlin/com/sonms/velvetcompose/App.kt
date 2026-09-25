package com.sonms.velvetcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonms.ratingbar.RatingBar
import com.sonms.velvetcompose.ui.theme.VelvetComposeTheme
import com.sonms.wheelpicker.HorizontalWheelPicker
import com.sonms.wheelpicker.VerticalWheelPicker
import com.sonms.wheelpicker.state.rememberWheelPickerState
import com.sonms.wheelpicker.style.WheelPickerDefaults

enum class SampleType {
    VERTICAL_TIME, HORIZONTAL_TIME, RATING_BAR, MODIFIER_NODE
}

/**
 * ModifierNodeShowcase 는 :modifier-node-processor (KSP) 코드젠에 의존하는 Android 전용 데모라
 * 플랫폼별 actual 로 갈아끼운다. iOS 쪽은 안내 텍스트만 보여준다.
 *
 * ModifierNodeShowcase depends on Android-only KSP codegen, so it's swapped in per platform —
 * iOS just shows a placeholder.
 */
@Composable
expect fun ModifierNodeShowcaseHost()

@Composable
fun App() {
    var selectedSample by remember { mutableStateOf(SampleType.VERTICAL_TIME) }

    VelvetComposeTheme(darkTheme = false) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 버튼들은 항상 한 줄로 그려진다 — weight()+Spacer로 남는 공간을 나누던 이전 방식은
                    // 플랫폼별 폰트 메트릭 차이로 폭이 좁아지면 텍스트가 세로로 wrap되는 문제가 있었다.
                    // Buttons always render on one line — the previous weight()+Spacer split let
                    // platform font-metric differences squeeze a button's width enough to wrap its
                    // text vertically (worst on iOS's system font).
                    Button(onClick = {
                        selectedSample = if (selectedSample == SampleType.VERTICAL_TIME)
                            SampleType.HORIZONTAL_TIME
                        else
                            SampleType.VERTICAL_TIME
                    }) {
                        Text(text = "Toggle Time Picker")
                    }

                    Button(onClick = { selectedSample = SampleType.RATING_BAR }) {
                        Text(text = "Toggle Rating Star")
                    }

                    Button(onClick = { selectedSample = SampleType.MODIFIER_NODE }) {
                        Text(text = "Modifier.Node")
                    }
                }

                when (selectedSample) {
                    SampleType.VERTICAL_TIME -> VerticalTimePickerSample()
                    SampleType.HORIZONTAL_TIME -> HorizontalTimePickerSample()
                    SampleType.RATING_BAR -> VelvetRatingBarSample()
                    SampleType.MODIFIER_NODE -> ModifierNodeShowcaseHost()
                }
            }
        }
    }
}

@Composable
fun VelvetRatingBarSample() {
    var initRating by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        RatingBar(
            rating = initRating,
            maxRating = 5,
            onRatingChanged = { rating ->
                initRating = rating
            },
        )
    }
}

@Composable
fun VerticalTimePickerSample() {
    val amPmItems = remember { listOf("AM", "PM") }
    val hourItems = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }

    val amPmState = rememberWheelPickerState(initialIndex = 0)
    val hourState = rememberWheelPickerState(initialIndex = 0)
    val minuteState = rememberWheelPickerState(initialIndex = 0)

    val itemHeight = 32.dp
    val visibleItemCount = 10

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // selector
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
            // AM / PM
            VerticalWheelPicker(
                items = amPmItems,
                state = amPmState,
                modifier = Modifier.width(80.dp),
                itemHeight = itemHeight,
                visibleItemCount = 2,
                infinite = false,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                    fade = WheelPickerDefaults.fadeStyle(
                        fraction = 0.3f,
                    ),
                ),
            ) { item, isSelected ->
                TimePickerItem(text = item, isSelected = isSelected)
            }

            // 시 - Hour
            VerticalWheelPicker(
                items = hourItems,
                state = hourState,
                modifier = Modifier.width(80.dp),
                itemHeight = itemHeight,
                visibleItemCount = visibleItemCount,
                infinite = true,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                    fade = WheelPickerDefaults.fadeStyle(
                        fraction = 1f / visibleItemCount,
                    ),
                ),
            ) { item, isSelected ->
                TimePickerItem(text = item, isSelected = isSelected)
            }

            // 분 - Minute
            VerticalWheelPicker(
                items = minuteItems,
                state = minuteState,
                modifier = Modifier.width(80.dp),
                itemHeight = itemHeight,
                visibleItemCount = visibleItemCount,
                infinite = true,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = true,
                    ),
                    fade = WheelPickerDefaults.fadeStyle(
                        fraction = 1f / visibleItemCount,
                    ),
                ),
            ) { item, isSelected ->
                TimePickerItem(text = item, isSelected = isSelected)
            }
        }
    }
}

@Composable
fun HorizontalTimePickerSample() {
    val amPmItems = remember { listOf("AM", "PM") }
    val hourItems = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }

    val amPmState = rememberWheelPickerState(initialIndex = 0)
    val hourState = rememberWheelPickerState(initialIndex = 0)
    val minuteState = rememberWheelPickerState(initialIndex = 0)

    val itemWidth = 48.dp
    val visibleItemCount = 5

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // selector
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
                .width(itemWidth)
                .background(
                    color = Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // AM / PM
            HorizontalWheelPicker(
                items = amPmItems,
                state = amPmState,
                modifier = Modifier.height(48.dp),
                itemWidth = itemWidth,
                visibleItemCount = visibleItemCount,
                infinite = false,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                ),
            ) { item, isSelected ->
                TimePickerItem(text = item, isSelected = isSelected)
            }

            // 시 - Hour
            HorizontalWheelPicker(
                items = hourItems,
                state = hourState,
                modifier = Modifier.height(48.dp),
                itemWidth = itemWidth,
                visibleItemCount = visibleItemCount,
                infinite = true,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                ),
            ) { item, isSelected ->
                TimePickerItem(text = item, isSelected = isSelected)
            }

            // 분 - Minute
            HorizontalWheelPicker(
                items = minuteItems,
                state = minuteState,
                modifier = Modifier.height(48.dp),
                itemWidth = itemWidth,
                visibleItemCount = visibleItemCount,
                infinite = true,
                style = WheelPickerDefaults.style(
                    selector = WheelPickerDefaults.selectorStyle(
                        background = Color.Transparent,
                        showDivider = false,
                    ),
                ),
            ) { item, isSelected ->
                TimePickerItem(text = item, isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun TimePickerItem(
    text: String,
    isSelected: Boolean,
) {
    Box(
        modifier = Modifier
            .styleable {
                contentColor(if (isSelected) Color.Black else Color.Gray)
            },
    ) {
        Text(
            text = text,
            fontSize = if (isSelected) 20.sp else 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
