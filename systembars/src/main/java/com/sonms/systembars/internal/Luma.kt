package com.sonms.systembars.internal

import kotlin.math.pow

/** WCAG 2.x relative luminance / contrast helpers on packed ARGB ints. Pure Kotlin. */
internal object Luma {
    fun relativeLuminance(argb: Int): Float {
        val r = linear((argb ushr 16) and 0xFF)
        val g = linear((argb ushr 8) and 0xFF)
        val b = linear(argb and 0xFF)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    fun contrastRatio(l1: Float, l2: Float): Float {
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    /** Source-over: [fg] drawn on [bg]. Result is opaque. */
    fun composite(fg: Int, bg: Int): Int {
        val a = ((fg ushr 24) and 0xFF) / 255f
        fun ch(shift: Int): Int {
            val f = (fg ushr shift) and 0xFF
            val b = (bg ushr shift) and 0xFF
            return (f * a + b * (1f - a) + 0.5f).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun linear(c: Int): Float {
        val s = c / 255f
        return if (s <= 0.03928f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
    }
}
