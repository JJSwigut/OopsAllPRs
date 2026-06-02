package com.jjswigut.oopsallprs.ds

import androidx.compose.ui.graphics.Color
import com.jjswigut.oopsallprs.ds.token.contrastRatio
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {

    @Test
    fun black_on_white_is_maximum_contrast() {
        val ratio = contrastRatio(Color.Black, Color.White)
        assertTrue(ratio in 20.9..21.1, "expected ~21, got $ratio")
    }

    @Test
    fun identical_colors_have_ratio_one() {
        val ratio = contrastRatio(Color(0xFF5EE7FF), Color(0xFF5EE7FF))
        assertTrue(ratio in 0.99..1.01, "expected ~1, got $ratio")
    }

    @Test
    fun ratio_is_symmetric() {
        val a = contrastRatio(Color(0xFFEAF2FF), Color(0xFF0E1422))
        val b = contrastRatio(Color(0xFF0E1422), Color(0xFFEAF2FF))
        assertTrue(abs(a - b) < 0.001)
    }
}
