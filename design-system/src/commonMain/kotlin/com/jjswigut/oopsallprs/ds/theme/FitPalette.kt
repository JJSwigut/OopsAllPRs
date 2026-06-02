package com.jjswigut.oopsallprs.ds.theme

import androidx.compose.runtime.Immutable
import com.jjswigut.oopsallprs.ds.token.FitColors

/** A named, immutable bundle of one full color set. Swapping this re-skins the whole system. */
@Immutable
data class FitPalette(
    val name: String,
    val isDark: Boolean,
    val colors: FitColors,
)
