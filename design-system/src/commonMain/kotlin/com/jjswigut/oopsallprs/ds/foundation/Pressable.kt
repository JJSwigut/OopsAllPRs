package com.jjswigut.oopsallprs.ds.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/**
 * Shared press feedback for every interactive component: spring scale-down on press plus a
 * haptic on press-down. Respects reduce-motion (no scale) and the global haptics-enabled flag
 * (via FitTheme.haptics, which is already gated).
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    haptic: HapticType = HapticType.Light,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier {
    val haptics = FitTheme.haptics
    val reduceMotion = FitTheme.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LaunchedEffect(pressed) {
        if (pressed && enabled) haptics.perform(haptic)
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) pressedScale else 1f,
        animationSpec = FitTheme.motion.snappy,
        label = "pressScale",
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
