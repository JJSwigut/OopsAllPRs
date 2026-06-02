package com.jjswigut.oopsallprs.ds

import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.token.FitShapes
import com.jjswigut.oopsallprs.ds.token.FitSize
import com.jjswigut.oopsallprs.ds.token.FitSpacing
import kotlin.test.Test
import kotlin.test.assertTrue

class TokenSanityTest {

    @Test
    fun spacing_scale_is_monotonically_increasing() {
        val s = FitSpacing()
        val scale = listOf(s.xs, s.sm, s.md, s.lg, s.xl, s.xxl, s.xxxl)
        scale.zipWithNext { a, b -> assertTrue(b > a, "expected $b > $a") }
    }

    @Test
    fun touch_target_meets_large_minimum() {
        assertTrue(FitSize().touchMin >= 56.dp)
    }

    @Test
    fun shapes_increase_in_radius() {
        val sh = FitShapes()
        assertTrue(sh.md > sh.sm)
        assertTrue(sh.lg > sh.md)
    }
}
