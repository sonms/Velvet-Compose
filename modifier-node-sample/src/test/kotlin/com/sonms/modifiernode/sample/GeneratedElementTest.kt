package com.sonms.modifiernode.sample

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KVisibility

/** 생성된 Element 의 equals/hashCode/create/update 계약과 가시성. Compose 런타임 불필요. */
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
        DebugTintElement(Color.Blue).update(node) // detached 노드에서 invalidateDraw() 는 no-op

        assertEquals(Color.Blue, node.color)
    }

    @Test
    fun create_seeds_node_from_element_args() {
        val node = DebugTintElement(Color.Green).create()
        assertEquals(Color.Green, node.color)
    }

    /** ABI: 생성된 Element 는 노드 가시성과 무관하게 internal. */
    @Test
    fun generated_element_is_internal() {
        assertEquals(KVisibility.INTERNAL, DebugTintElement::class.visibility)
        assertEquals(KVisibility.INTERNAL, FixedSquareElement::class.visibility)
    }

    private fun mkDp(v: Int) = androidx.compose.ui.unit.Dp(v.toFloat())
}
