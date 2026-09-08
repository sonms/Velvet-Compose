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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sonms.modifiernode.annotations.Invalidates
import com.sonms.modifiernode.annotations.InvalidationScope.Draw
import com.sonms.modifiernode.annotations.InvalidationScope.Measure
import com.sonms.modifiernode.annotations.ModifierNodeFactory
import com.sonms.modifiernode.annotations.OnChange
import com.sonms.modifiernode.annotations.SkipWhenFalse
import com.sonms.modifiernode.annotations.SkipWhenTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 노드가 자기 measure/draw 호출 횟수를 기록. */
object ProbeCounters {
    var measures = 0
    var draws = 0
    fun reset() {
        measures = 0
        draws = 0
    }
}

@ModifierNodeFactory(name = "probe")
internal class ProbeNode(
    @Invalidates(Draw) var drawKey: Int,
    @Invalidates(Measure) var measureKey: Int,
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        ProbeCounters.measures++
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        ProbeCounters.draws++
        drawContent()
    }
}

@ModifierNodeFactory(name = "skipProbe")
internal class SkipProbeNode(
    @SkipWhenFalse var on: Boolean,
    @SkipWhenTrue var hidden: Boolean,
    @Invalidates(Draw) var tag: Int,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false
    override fun ContentDrawScope.draw() = drawContent()
}

object OnChangeCount {
    var value = 0
}

@ModifierNodeFactory(name = "onChangeProbe")
internal class OnChangeProbeNode(
    @OnChange @Invalidates(Draw) var key: Int,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false
    fun onKeyChanged() { OnChangeCount.value++ }
    override fun ContentDrawScope.draw() = drawContent()
}

/** equals 스킵과 `@SkipWhenFalse` 가드. Element 가 equal 이면 Compose 는 `update()` 를 부르지 않는다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class InvalidationContractTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun unchanged_params_skip_update_entirely() {
        val drawKey = mutableIntStateOf(0)
        val tick = mutableIntStateOf(0)

        rule.setContent {
            tick.intValue // 상위 재구성만 유발, probe 파라미터는 고정
            Box(Modifier.size(24.dp).probe(drawKey.intValue, 0))
        }
        rule.waitForIdle()
        ProbeCounters.reset()

        tick.intValue = 1
        rule.waitForIdle()

        assertEquals(0, ProbeCounters.measures)
        assertEquals(0, ProbeCounters.draws)
    }

    @Test
    fun skip_markers_gate_application() {
        // 적용: on=true 이고 hidden=false
        assertNotSame(Modifier, Modifier.skipProbe(on = true, hidden = false, tag = 0))
        // @SkipWhenFalse: on=false → skip
        assertSame(Modifier, Modifier.skipProbe(on = false, hidden = false, tag = 0))
        // @SkipWhenTrue: hidden=true → skip
        assertSame(Modifier, Modifier.skipProbe(on = true, hidden = true, tag = 0))
    }

    @Test
    fun onChange_fires_only_when_param_changes() {
        val key = mutableIntStateOf(0)
        rule.setContent { Box(Modifier.size(24.dp).onChangeProbe(key.intValue)) }
        rule.waitForIdle()
        OnChangeCount.value = 0

        key.intValue = 1
        rule.waitForIdle()
        assertEquals(1, OnChangeCount.value)

        key.intValue = 1 // 같은 값 → update() 미호출
        rule.waitForIdle()
        assertEquals(1, OnChangeCount.value)
    }
}
