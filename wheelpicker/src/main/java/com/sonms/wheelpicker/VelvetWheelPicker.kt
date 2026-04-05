package com.sonms.wheelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonms.wheelpicker.extension.fadingEdge
import com.sonms.wheelpicker.state.WheelPickerState
import com.sonms.wheelpicker.state.rememberWheelPickerState
import com.sonms.wheelpicker.style.WheelPickerDefaults
import com.sonms.wheelpicker.style.WheelPickerStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.absoluteValue

/**
 * WheelPicker 방향 정의.
 * Defines the orientation of WheelPicker.
 */
internal enum class WheelPickerOrientation { Vertical, Horizontal }

/**
 * Vertical WheelPicker의 공개 API.
 * Public API for Vertical WheelPicker.
 *
 * @param items 표시할 아이템 목록 / List of items to display
 * @param modifier Modifier
 * @param state WheelPicker 상태 / WheelPicker state
 * @param itemHeight 각 아이템 높이 / Height of each item
 * @param visibleItemCount 화면에 보이는 아이템 수 (홀수 권장) / Visible item count (odd recommended)
 * @param infinite 무한 스크롤 여부 / Whether infinite scroll is enabled
 * @param style 스타일 설정 / Style configuration
 * @param onItemSelected 아이템 선택 콜백 / Callback when item is selected
 * @param itemContent 아이템 UI 슬롯 / Item UI slot
 */
@Composable
fun <T> VerticalWheelPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    itemHeight: Dp = 48.dp,
    visibleItemCount: Int = 5,
    infinite: Boolean = true,
    style: WheelPickerStyle = WheelPickerDefaults.style(),
    onItemSelected: (index: Int, item: T) -> Unit = { _, _ -> },
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    WheelPickerImpl(
        items = items,
        modifier = modifier,
        state = state,
        itemSize = itemHeight,
        visibleItemCount = visibleItemCount,
        infinite = infinite,
        style = style,
        onItemSelected = onItemSelected,
        orientation = WheelPickerOrientation.Vertical,
        itemContent = itemContent,
    )
}

/**
 * Horizontal WheelPicker의 공개 API.
 * Public API for Horizontal WheelPicker.
 *
 * @param items 표시할 아이템 목록 / List of items to display
 * @param modifier Modifier
 * @param state WheelPicker 상태 / WheelPicker state
 * @param itemWidth 각 아이템 너비 / Width of each item
 * @param visibleItemCount 화면에 보이는 아이템 수 (홀수 권장) / Visible item count (odd recommended)
 * @param infinite 무한 스크롤 여부 / Whether infinite scroll is enabled
 * @param style 스타일 설정 / Style configuration
 * @param onItemSelected 아이템 선택 콜백 / Callback when item is selected
 * @param itemContent 아이템 UI 슬롯 / Item UI slot
 */
@Composable
fun <T> HorizontalWheelPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    itemWidth: Dp = 48.dp,
    visibleItemCount: Int = 5,
    infinite: Boolean = true,
    style: WheelPickerStyle = WheelPickerDefaults.style(),
    onItemSelected: (index: Int, item: T) -> Unit = { _, _ -> },
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    WheelPickerImpl(
        items = items,
        modifier = modifier,
        state = state,
        itemSize = itemWidth,
        visibleItemCount = visibleItemCount,
        infinite = infinite,
        style = style,
        onItemSelected = onItemSelected,
        orientation = WheelPickerOrientation.Horizontal,
        itemContent = itemContent,
    )
}

/**
 * WheelPicker 내부 공통 구현.
 * Internal common implementation of WheelPicker.
 */
@Composable
internal fun <T> WheelPickerImpl(
    items: List<T>,
    modifier: Modifier,
    state: WheelPickerState,
    itemSize: Dp,
    visibleItemCount: Int,
    infinite: Boolean,
    style: WheelPickerStyle,
    onItemSelected: (index: Int, item: T) -> Unit,
    orientation: WheelPickerOrientation,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    if (items.isEmpty()) return

    val immutableItems = remember(items) {
        items as? ImmutableList<T> ?: items.toImmutableList()
    }

    val itemCount = immutableItems.size
    val pickerSize = itemSize * visibleItemCount
    val paddingSize = (pickerSize - itemSize) / 2

    val pageCount = if (infinite) Int.MAX_VALUE else itemCount
    val initialPage = if (infinite) {
        val center = Int.MAX_VALUE / 2
        center - (center % itemCount) + state.initialIndex
    } else {
        state.initialIndex
    }

    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    SideEffect {
        state.pagerState = pagerState
        state.itemCount = itemCount
        state.isInfinite = infinite
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val actualIndex = page % itemCount
            onItemSelected(actualIndex, immutableItems[actualIndex])
        }
    }

    Box(
        modifier = modifier
            .then(
                if (orientation == WheelPickerOrientation.Vertical)
                    Modifier.height(pickerSize)
                else
                    Modifier.width(pickerSize)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (orientation == WheelPickerOrientation.Vertical)
                        Modifier.fillMaxWidth().height(itemSize)
                    else
                        Modifier.width(itemSize).height(itemSize)
                )
                .background(
                    color = style.selector.background,
                    shape = style.selector.shape,
                )
        )

        if (style.selector.showDivider) {
            if (orientation == WheelPickerOrientation.Vertical) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .offset(y = -(itemSize / 2)),
                    thickness = style.selector.dividerThickness,
                    color = style.selector.dividerColor,
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .offset(y = itemSize / 2),
                    thickness = style.selector.dividerThickness,
                    color = style.selector.dividerColor,
                )
            } else {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .offset(x = -(itemSize / 2)),
                    thickness = style.selector.dividerThickness,
                    color = style.selector.dividerColor,
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .offset(x = itemSize / 2),
                    thickness = style.selector.dividerThickness,
                    color = style.selector.dividerColor,
                )
            }
        }

        val pagerModifier = Modifier
            .fillMaxSize()
            .fadingEdge(
                isVertical = orientation == WheelPickerOrientation.Vertical,
                fraction = style.fade.fraction,
                enabled = style.fade.enabled,
            )

        val contentPadding = if (orientation == WheelPickerOrientation.Vertical)
            PaddingValues(vertical = paddingSize)
        else
            PaddingValues(horizontal = paddingSize)

        when (orientation) {
            WheelPickerOrientation.Vertical -> VerticalPager(
                state = pagerState,
                contentPadding = contentPadding,
                modifier = pagerModifier,
                pageSpacing = 0.dp,
            ) { page ->
                val actualIndex = page % itemCount
                val isSelected = pagerState.settledPage == page
                WheelPickerItem(
                    page = page,
                    pagerState = pagerState,
                    itemSize = itemSize,
                    orientation = orientation,
                    style = style,
                ) {
                    itemContent(immutableItems[actualIndex], isSelected)
                }
            }

            WheelPickerOrientation.Horizontal -> HorizontalPager(
                state = pagerState,
                contentPadding = contentPadding,
                modifier = pagerModifier,
                pageSpacing = 0.dp,
            ) { page ->
                val actualIndex = page % itemCount
                val isSelected = pagerState.settledPage == page
                WheelPickerItem(
                    page = page,
                    pagerState = pagerState,
                    itemSize = itemSize,
                    orientation = orientation,
                    style = style,
                ) {
                    itemContent(immutableItems[actualIndex], isSelected)
                }
            }
        }
    }
}

/**
 * 각 아이템에 3D 효과를 적용하는 내부 Composable.
 * Internal Composable that applies 3D transform effect to each item.
 */
@Composable
private fun WheelPickerItem(
    page: Int,
    pagerState: PagerState,
    itemSize: Dp,
    orientation: WheelPickerOrientation,
    style: WheelPickerStyle,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(
                if (orientation == WheelPickerOrientation.Vertical)
                    Modifier.fillMaxWidth().height(itemSize)
                else
                    Modifier.width(itemSize).height(itemSize)
            )
            .graphicsLayer {
                val pageOffset = (pagerState.currentPage - page) +
                        pagerState.currentPageOffsetFraction

                val transform = style.transform
                if (transform.rotationEnabled) {
                    val clampedOffset = pageOffset.coerceIn(-3f, 3f)

                    if (orientation == WheelPickerOrientation.Vertical) {
                        rotationX = clampedOffset * transform.maxRotationDegree
                        translationY = pageOffset * (itemSize.toPx() * 0.25f)
                    } else {
                        rotationY = clampedOffset * transform.maxRotationDegree
                        translationX = pageOffset * (itemSize.toPx() * 0.25f)
                    }
                }
                if (transform.scaleEnabled) {
                    val scale = (1f - (pageOffset.absoluteValue * (1f - transform.minScale)))
                        .coerceAtLeast(transform.minScale)
                    scaleX = scale
                    scaleY = scale
                }
                if (transform.alphaEnabled) {
                    alpha = (1f - (pageOffset.absoluteValue * (1f - transform.minAlpha)))
                        .coerceAtLeast(transform.minAlpha)
                }
                cameraDistance = 8 * density
            }
    ) {
        content()
    }
}