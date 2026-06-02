package com.jjswigut.oopsallprs.ds

import com.jjswigut.oopsallprs.ds.theme.FitPalettes
import com.jjswigut.oopsallprs.ds.token.contrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitPaletteTest {

    @Test
    fun ice_dark_body_text_meets_AA_normal() {
        val c = FitPalettes.IceDark.colors
        assertTrue(
            contrastRatio(c.onSurface, c.surface) >= 4.5,
            "onSurface/surface ratio = ${contrastRatio(c.onSurface, c.surface)}",
        )
    }

    @Test
    fun ice_light_body_text_meets_AA_normal() {
        val c = FitPalettes.IceLight.colors
        assertTrue(
            contrastRatio(c.onSurface, c.surface) >= 4.5,
            "onSurface/surface ratio = ${contrastRatio(c.onSurface, c.surface)}",
        )
    }

    @Test
    fun muted_text_meets_AA_large_in_both_palettes() {
        listOf(FitPalettes.IceDark, FitPalettes.IceLight).forEach { p ->
            val ratio = contrastRatio(p.colors.onSurfaceMuted, p.colors.surface)
            assertTrue(ratio >= 3.0, "${p.name} muted ratio = $ratio")
        }
    }

    @Test
    fun accent_label_is_legible_on_accent_fill() {
        listOf(FitPalettes.IceDark, FitPalettes.IceLight).forEach { p ->
            val ratio = contrastRatio(p.colors.onAccent, p.colors.accent)
            assertTrue(ratio >= 3.0, "${p.name} onAccent/accent ratio = $ratio")
        }
    }

    @Test
    fun palettes_declare_correct_mode() {
        assertTrue(FitPalettes.IceDark.isDark)
        assertEquals(false, FitPalettes.IceLight.isDark)
    }
}
