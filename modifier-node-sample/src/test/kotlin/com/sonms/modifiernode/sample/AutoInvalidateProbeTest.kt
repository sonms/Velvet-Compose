package com.sonms.modifiernode.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * `shouldAutoInvalidate = false` 노드는 `update()` 가 부른 것만 무효화한다 (ARCHITECTURE.md §10).
 * 동일 하네스에서 auto ON/OFF 두 노드를 비교하고, 카운터는 노드 자신의 measure()/draw() 에서 증가시킨다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AutoInvalidateProbeTest {

    @get:Rule
    val rule = createComposeRule()

    private class Counters {
        var measures = 0
        var draws = 0
    }

    private val onC = Counters()
    private val offC = Counters()

    // autoInvalidate ON (기본)
    private inner class AutoOnNode(var drawKey: Int, var measureKey: Int) :
        Modifier.Node(), DrawModifierNode, LayoutModifierNode {
        override fun MeasureScope.measure(m: Measurable, c: Constraints): MeasureResult {
            onC.measures++
            val p = m.measure(c)
            return layout(p.width, p.height) { p.place(0, 0) }
        }
        override fun ContentDrawScope.draw() { onC.draws++; drawContent() }
    }

    private inner class AutoOnElement(val drawKey: Int, val measureKey: Int) :
        ModifierNodeElement<AutoOnNode>() {
        override fun create() = AutoOnNode(drawKey, measureKey)
        override fun update(node: AutoOnNode) {
            val d = node.drawKey != drawKey
            val m = node.measureKey != measureKey
            node.drawKey = drawKey
            node.measureKey = measureKey
            if (m) node.invalidateMeasurement() else if (d) node.invalidateDraw()
        }
        override fun InspectorInfo.inspectableProperties() { name = "autoOn" }
        override fun equals(other: Any?) =
            other is AutoOnElement && other.drawKey == drawKey && other.measureKey == measureKey
        override fun hashCode() = drawKey * 31 + measureKey
    }

    // autoInvalidate OFF
    private inner class AutoOffNode(var drawKey: Int, var measureKey: Int) :
        Modifier.Node(), DrawModifierNode, LayoutModifierNode {
        override val shouldAutoInvalidate: Boolean get() = false
        override fun MeasureScope.measure(m: Measurable, c: Constraints): MeasureResult {
            offC.measures++
            val p = m.measure(c)
            return layout(p.width, p.height) { p.place(0, 0) }
        }
        override fun ContentDrawScope.draw() { offC.draws++; drawContent() }
    }

    private inner class AutoOffElement(val drawKey: Int, val measureKey: Int) :
        ModifierNodeElement<AutoOffNode>() {
        override fun create() = AutoOffNode(drawKey, measureKey)
        override fun update(node: AutoOffNode) {
            val d = node.drawKey != drawKey
            val m = node.measureKey != measureKey
            node.drawKey = drawKey
            node.measureKey = measureKey
            if (m) node.invalidateMeasurement() else if (d) node.invalidateDraw()
        }
        override fun InspectorInfo.inspectableProperties() { name = "autoOff" }
        override fun equals(other: Any?) =
            other is AutoOffElement && other.drawKey == drawKey && other.measureKey == measureKey
        override fun hashCode() = drawKey * 31 + measureKey
    }

    /** @param mutate 0 = drawKey 변경, 1 = measureKey 변경 */
    private fun run(makeOn: Boolean, mutate: Int): Counters {
        val dk = mutableIntStateOf(0)
        val mk = mutableIntStateOf(0)
        val counters = if (makeOn) onC else offC

        rule.setContent {
            val d = dk.intValue
            val m = mk.intValue
            val mod: Modifier = if (makeOn) {
                Modifier.size(24.dp).then(AutoOnElement(d, m))
            } else {
                Modifier.size(24.dp).then(AutoOffElement(d, m))
            }
            Box(mod)
        }
        rule.waitForIdle()
        counters.measures = 0
        counters.draws = 0

        if (mutate == 0) dk.intValue = 1 else mk.intValue = 1
        rule.waitForIdle()
        return counters
    }

    /** 기준선: auto ON 이면 draw 파라미터만 바꿔도 remeasure. */
    @Test
    fun autoOn_drawParam_change_remeasures() {
        val c = run(makeOn = true, mutate = 0)
        assertTrue(c.measures >= 1)
    }

    /** auto OFF: draw 파라미터 변경은 remeasure 를 유발하지 않는다. */
    @Test
    fun autoOff_drawParam_change_does_NOT_remeasure() {
        val c = run(makeOn = false, mutate = 0)
        assertEquals(0, c.measures)
    }

    /** auto OFF: measure 파라미터 변경 시 update() 의 invalidateMeasurement() 는 정상 작동. */
    @Test
    fun autoOff_measureParam_change_DOES_remeasure_via_update() {
        val c = run(makeOn = false, mutate = 1)
        assertTrue(c.measures >= 1)
    }
}
