package com.jjswigut.oopsallprs.ds.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color roles — never raw color names at call sites.
 * `surfaceGlass`, `border`, `borderGlow`, and `accentGlow` are typically translucent
 * (they are composited over [surface]/[background] by the glass renderer).
 */
@Immutable
data class FitColors(
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val border: Color,
    val borderGlow: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val accentGlow: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)
