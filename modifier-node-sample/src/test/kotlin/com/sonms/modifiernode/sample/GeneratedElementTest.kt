package com.sonms.modifiernode.sample

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * codegen 이 실제로 보장하는 계약을 고정한다 — Compose 런타임 불필요.
 *
 *  1. **재사용(equals/hashCode) 계약**: 같은 인자 → equal, 한 인자라도 다르면 non-equal.
 *     이게 `composed` 대비 진짜 이득이다. Element 가 equal 이면 Compose 는 `update()` 를
 *     아예 호출하지 않는다.
 *  2. **update() 필드 동기화**: 바뀐 파라미터가 노드 필드에 반영된다.
 *
 * ⚠️ 여기서 검증하지 않는 것: "파라미터 단위 invalidation 스코프". 현행 Compose 에서
 *    `NodeChain.updateNode` 가 `update()` 직후 모든 capability 를 무조건 무효화하므로
 *    생성된 `if (redraw) invalidateDraw()` 는 런타임 효과가 없다 (ARCHITECTURE.md §10).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeneratedElementTest {

    @Test
    fun equal_args_produce_equal_elements() {
        assertEquals(DebugTintElement(Color.Red), DebugTintElement(Color.Red))
        assertEquals(
            DebugTintElement(Color.Red).hashCode(),
            DebugTintElement(Color.Red).hashCode(),
        )
    }

    @Test
    fun differing_args_produce_non_equal_elements() {
        assertNotEquals(DebugTintElement(Color.Red), DebugTintElement(Color.Blue))
    }

    @Test
    fun multi_param_equality_is_per_field() {
        assertEquals(FixedSquareElement(mkDp(8), Color.Red), FixedSquareElement(mkDp(8), Color.Red))
        assertNotEquals(FixedSquareElement(mkDp(8), Color.Red), FixedSquareElement(mkDp(9), Color.Red))
        assertNotEquals(FixedSquareElement(mkDp(8), Color.Red), FixedSquareElement(mkDp(8), Color.Blue))
    }

    @Test
    fun update_syncs_changed_field_to_node() {
        val node = DebugTintNode(Color.Red)
        // update() 는 detached 노드에서 invalidateDraw() 가 no-op 이라 안전.
        DebugTintElement(Color.Blue).update(node)
        assertEquals(Color.Blue, node.color)
    }

    @Test
    fun create_seeds_node_from_element_args() {
        val node = DebugTintElement(Color.Green).create()
        assertEquals(Color.Green, node.color)
    }

    private fun mkDp(v: Int) = androidx.compose.ui.unit.Dp(v.toFloat())
}
