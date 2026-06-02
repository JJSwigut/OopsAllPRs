package com.jjswigut.oopsallprs.ds.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jjswigut.oopsallprs.ds.haptic.HapticFeedback
import com.jjswigut.oopsallprs.ds.haptic.LocalHaptics
import com.jjswigut.oopsallprs.ds.haptic.NoHaptics
import com.jjswigut.oopsallprs.ds.haptic.rememberHapticFeedback
import com.jjswigut.oopsallprs.ds.token.FitColors
import com.jjswigut.oopsallprs.ds.token.FitGlow
import com.jjswigut.oopsallprs.ds.token.FitMotion
import com.jjswigut.oopsallprs.ds.token.FitShapes
import com.jjswigut.oopsallprs.ds.token.FitSize
import com.jjswigut.oopsallprs.ds.token.FitSpacing
import com.jjswigut.oopsallprs.ds.token.FitType

private val LocalFitColors = staticCompositionLocalOf<FitColors> {
    error("FitColors not provided. Wrap your content in FitTheme { }.")
}
private val LocalFitSpacing = staticCompositionLocalOf { FitSpacing() }
private val LocalFitShapes = staticCompositionLocalOf { FitShapes() }
private val LocalFitSize = staticCompositionLocalOf { FitSize() }
private val LocalFitType = staticCompositionLocalOf { FitType() }
private val LocalFitGlow = staticCompositionLocalOf { FitGlow() }
private val LocalFitMotion = staticCompositionLocalOf { FitMotion() }
private val LocalReduceMotion = staticCompositionLocalOf { false }
private val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Root theme wrapper. Pass a different [palette] to re-skin everything or toggle light/dark;
 * color tokens animate to their new values so the change glides rather than snaps.
 */
@Composable
fun FitTheme(
    palette: FitPalette,
    spacing: FitSpacing = FitSpacing(),
    shapes: FitShapes = FitShapes(),
    size: FitSize = FitSize(),
    type: FitType = FitType(),
    glow: FitGlow = FitGlow(),
    motion: FitMotion = FitMotion(),
    reduceMotion: Boolean = false,
    hapticsEnabled: Boolean = true,
    haptics: HapticFeedback = rememberHapticFeedback(),
    content: @Composable () -> Unit,
) {
    val c = palette.colors
    val animated = FitColors(
        background = animatedColor(c.background, motion).value,
        surface = animatedColor(c.surface, motion).value,
        surfaceGlass = animatedColor(c.surfaceGlass, motion).value,
        border = animatedColor(c.border, motion).value,
        borderGlow = animatedColor(c.borderGlow, motion).value,
        onSurface = animatedColor(c.onSurface, motion).value,
        onSurfaceMuted = animatedColor(c.onSurfaceMuted, motion).value,
        accent = animatedColor(c.accent, motion).value,
        accentGlow = animatedColor(c.accentGlow, motion).value,
        onAccent = animatedColor(c.onAccent, motion).value,
        success = animatedColor(c.success, motion).value,
        warning = animatedColor(c.warning, motion).value,
        danger = animatedColor(c.danger, motion).value,
    )

    val gatedHaptics = if (hapticsEnabled) haptics else NoHaptics

    CompositionLocalProvider(
        LocalFitColors provides animated,
        LocalFitSpacing provides spacing,
        LocalFitShapes provides shapes,
        LocalFitSize provides size,
        LocalFitType provides type,
        LocalFitGlow provides glow,
        LocalFitMotion provides motion,
        LocalHaptics provides gatedHaptics,
        LocalReduceMotion provides reduceMotion,
        LocalHapticsEnabled provides hapticsEnabled,
        content = content,
    )
}

@Composable
private fun animatedColor(target: Color, motion: FitMotion): State<Color> =
    animateColorAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = motion.smoothDamping,
            stiffness = motion.smoothStiffness,
        ),
        label = "fitColor",
    )

/** Ergonomic token accessor: read values as FitTheme.colors, .spacing, etc. */
object FitTheme {
    val colors: FitColors
        @Composable @ReadOnlyComposable get() = LocalFitColors.current
    val spacing: FitSpacing
        @Composable @ReadOnlyComposable get() = LocalFitSpacing.current
    val shapes: FitShapes
        @Composable @ReadOnlyComposable get() = LocalFitShapes.current
    val size: FitSize
        @Composable @ReadOnlyComposable get() = LocalFitSize.current
    val type: FitType
        @Composable @ReadOnlyComposable get() = LocalFitType.current
    val glow: FitGlow
        @Composable @ReadOnlyComposable get() = LocalFitGlow.current
    val motion: FitMotion
        @Composable @ReadOnlyComposable get() = LocalFitMotion.current
    val haptics: HapticFeedback
        @Composable @ReadOnlyComposable get() = LocalHaptics.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}
