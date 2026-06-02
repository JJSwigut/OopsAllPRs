package com.jjswigut.oopsallprs.ds.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ds.token.FitGlowLevel

/**
 * Theme-aware shortcut over [glass]: pulls fill/border/glow colors from the current palette so
 * components don't repeat token wiring. `Modifier.fitGlass()` is all most surfaces need.
 */
@Composable
fun Modifier.fitGlass(
    shape: Shape = FitTheme.shapes.mediumShape,
    glow: FitGlowLevel = FitTheme.glow.medium,
): Modifier = glass(
    shape = shape,
    fill = FitTheme.colors.surfaceGlass,
    borderColor = FitTheme.colors.border,
    glowColor = FitTheme.colors.borderGlow,
    glow = glow,
)
