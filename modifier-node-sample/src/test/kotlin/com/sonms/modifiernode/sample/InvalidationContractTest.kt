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
import com.sonms.modifiernode.annotations.SkipWhenFalse
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
    @Invalidates(Draw) var tag: Int,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false
    override fun ContentDrawScope.draw() = drawContent()
}

/**
 * codegen 이 주는 최적화 중 하나 = **equals 스킵**.
 * 파라미터가 안 바뀌면 `Element.equals` == true → Compose 가 `update()` 를 아예 안 부름
 * → 측정/그리기 무효화 없음.
 *
 * (파라미터 단위 invalidation 스코프는 `shouldAutoInvalidate = false` 를 통해 작동한다 —
 *  `AutoInvalidateProbeTest` + ARCHITECTURE.md §10.)
 */
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
            tick.intValue // 재구성 트리거용, modifier 파라미터엔 안 들어감
            Box(Modifier.size(24.dp).probe(drawKey.intValue, 0))
        }
        rule.waitForIdle()
        ProbeCounters.reset()

        // 상위만 재구성, probe 파라미터는 동일 → Element equal → update 미호출
        tick.intValue = 1
        rule.waitForIdle()

        assertEquals("equal Element must not trigger remeasure", 0, ProbeCounters.measures)
        assertEquals("equal Element must not trigger redraw", 0, ProbeCounters.draws)
    }

    /** @SkipWhenFalse: on=false 면 생성 함수가 리시버를 그대로 반환(노드 미적용). */
    @Test
    fun skipWhenFalse_gates_application() {
        assertSame("on=false → 리시버 그대로", Modifier, Modifier.skipProbe(on = false, tag = 0))
        assertNotSame("on=true → Element 추가됨", Modifier, Modifier.skipProbe(on = true, tag = 0))
    }
}
