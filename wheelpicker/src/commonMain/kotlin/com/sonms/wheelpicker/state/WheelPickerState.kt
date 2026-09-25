package com.sonms.wheelpicker.state

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * WheelPicker의 상태를 관리하는 클래스.
 * State class that manages the state of WheelPicker.
 *
 * @param initialIndex 초기 선택 인덱스. 기본값은 0.
 * Initial selected index. Defaults to 0.
 * */
@Stable
class WheelPickerState(
    val initialIndex: Int = 0,
) {
    /**
     * 내부적으로 사용되는 PagerState.
     * Internal PagerState used by WheelPickerImpl.
     *
     * 외부에서 직접 접근하지 마세요.
     * Do not access this directly from outside.
     */
    internal lateinit var pagerState: PagerState

    /**
     * 현재 아이템 총 개수. WheelPickerImpl에서 주입됩니다.
     * Total item count. Injected by WheelPickerImpl.
     */
    internal var itemCount: Int by mutableIntStateOf(0)  // State로 변경

    /**
     * 무한 스크롤 모드 여부.
     * Whether the WheelPicker is in infinite scroll mode.
     * */
    internal var isInfinite: Boolean = false

    /**
     * 현재 선택된 아이템의 실제 인덱스.
     * The actual index of the currently selected item.
     *
     * 무한 스크롤 모드에서도 items 리스트 기준의 인덱스를 반환합니다.
     * Returns the index relative to the items list, even in infinite scroll mode.
     */
    val currentIndex: Int
        get() = if (itemCount == 0 || !::pagerState.isInitialized) 0
                else pagerState.currentPage % itemCount

    /**
     * 현재 스크롤이 진행 중인지 여부.
     * Whether scrolling is currently in progress.
     */
    val isScrollInProgress: Boolean
        get() = if (!::pagerState.isInitialized) false
                else pagerState.isScrollInProgress

    /**
     * 애니메이션 없이 즉시 해당 인덱스로 이동합니다.
     * Immediately scrolls to the given index without animation.
     *
     * @param index 이동할 대상 인덱스. items 범위 내여야 합니다.
     *              Target index to scroll to. Must be within the items range.
     *
     * @throws IllegalArgumentException index가 유효 범위를 벗어난 경우.
     *                                  If index is out of valid range.
     */
    suspend fun scrollToIndex(index: Int) {
        requireValidIndex(index)
        pagerState.scrollToPage(
            if (isInfinite) targetPage(index) else index
        )
    }

    /**
     * 애니메이션과 함께 해당 인덱스로 이동합니다.
     * Animates scrolling to the given index.
     *
     * @param index 이동할 대상 인덱스. items 범위 내여야 합니다.
     *              Target index to scroll to. Must be within the items range.
     *
     * @throws IllegalArgumentException index가 유효 범위를 벗어난 경우.
     *                                  If index is out of valid range.
     */
    suspend fun animateScrollToIndex(index: Int) {
        requireValidIndex(index)
        pagerState.animateScrollToPage(
            if (isInfinite) targetPage(index) else index
        )
    }

    /**
     * 무한 스크롤에서 현재 위치 기준으로 가장 가까운 타겟 페이지를 계산합니다.
     * Calculates the nearest target page based on current position in infinite scroll.
     *
     * @param index 이동할 실제 아이템 인덱스.
     *              The actual item index to move to.
     * @return 이동할 PagerState 페이지 번호.
     *         The PagerState page number to scroll to.
     */
    private fun targetPage(index: Int): Int {
        val current = pagerState.currentPage
        val center = current - (current % itemCount) + index
        return when {
            center - current > itemCount / 2 -> center - itemCount
            current - center > itemCount / 2 -> center + itemCount
            else -> center
        }
    }

    /**
     * 인덱스가 유효한 범위인지 검증합니다.
     * Validates that the given index is within the valid range.
     *
     * @param index 검증할 인덱스.
     *              The index to validate.
     *
     * @throws IllegalArgumentException index가 0 미만이거나 itemCount 이상인 경우.
     *                                  If index is less than 0 or >= itemCount.
     */
    private fun requireValidIndex(index: Int) {
        require(index in 0 until itemCount) {
            "index $index out of bounds (itemCount=$itemCount)"
        }
    }
}

/**
 * [WheelPickerState]를 remember하여 반환합니다.
 * Creates and remembers a [WheelPickerState].
 */
@Composable
fun rememberWheelPickerState(
    initialIndex: Int = 0,
): WheelPickerState {
    return remember(initialIndex) {
        WheelPickerState(initialIndex)
    }
}