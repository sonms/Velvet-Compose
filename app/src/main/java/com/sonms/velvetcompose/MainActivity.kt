package com.sonms.velvetcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonms.velvetcompose.ui.theme.VelvetComposeTheme
import com.sonms.wheelpicker.HorizontalWheelPicker
import com.sonms.wheelpicker.VerticalWheelPicker
import com.sonms.wheelpicker.state.rememberWheelPickerState
import com.sonms.wheelpicker.style.WheelPickerDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var timePickerType by remember {
                mutableStateOf(false)
            }

            VelvetComposeTheme(darkTheme = false) {
                Scaffold (
                    modifier = Modifier
                        .fillMaxSize(),
                ) { innerPadding ->
                    Column (
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = { timePickerType = !timePickerType },
                        ) {
                            Text(text = "Toggle Time Picker")
                        }

                        if (timePickerType) {
                            VerticalTimePickerSample()
                        } else {
                            HorizontalTimePickerSample()
                        }
                    }
                }
            }
        }
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
    Text(
        text = text,
        fontSize = if (isSelected) 20.sp else 16.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) {
            Color.Black
        } else {
            Color.Gray
        },
    )
}