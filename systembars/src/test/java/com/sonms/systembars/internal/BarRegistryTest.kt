package com.sonms.systembars.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarRegistryTest {
    private val auto = Spec.Auto
    private val dark = Spec.Dark
    private val light = Spec.Light

    @Test
    fun empty_resolvesToNull() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        assertNull(r.resolveStatus())
        assertNull(r.resolveNavigation())
    }

    @Test
    fun lastRegistered_wins_andRestoresOnDispose() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        val first = r.register(status = dark, navigation = dark)
        val second = r.register(status = light, navigation = auto)
        assertEquals(light, r.resolveStatus())
        assertEquals(auto, r.resolveNavigation())
        second.dispose()
        assertEquals(dark, r.resolveStatus())
        first.dispose()
        assertNull(r.resolveStatus())
    }

    @Test
    fun inherit_fallsThroughPerBar() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        r.register(status = dark, navigation = light)
        r.register(status = Spec.Inherit, navigation = auto)
        assertEquals(dark, r.resolveStatus())
        assertEquals(auto, r.resolveNavigation())
    }

    @Test
    fun update_replacesInPlaceWithoutChangingOrder() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        val a = r.register(status = dark, navigation = dark)
        r.register(status = light, navigation = light)
        a.update(status = auto, navigation = auto)
        assertEquals(light, r.resolveStatus())
        a.dispose()
        assertEquals(light, r.resolveStatus())
    }

    @Test
    fun listener_firesOnRegisterUpdateDispose() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        var count = 0
        r.onChanged = { count++ }
        val h = r.register(dark, dark)
        h.update(light, light)
        h.dispose()
        assertEquals(3, count)
    }

    @Test
    fun higherPriority_winsRegardlessOfRegistrationOrder() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        r.register(status = dark, navigation = dark, priority = 3) // deep child registers first
        r.register(status = light, navigation = light, priority = 1) // shallow parent registers later
        assertEquals(dark, r.resolveStatus())
        r.register(status = auto, navigation = auto, priority = 3) // same depth: newer wins
        assertEquals(auto, r.resolveStatus())
    }

    @Test
    fun disposeTwice_isNoOp() {
        val r = BarRegistry<Spec>(isInherit = { it == Spec.Inherit })
        var count = 0
        r.onChanged = { count++ }
        val h = r.register(dark, dark)
        h.dispose()
        h.dispose()
        assertEquals(2, count)
    }

    enum class Spec { Auto, Dark, Light, Inherit }
}
