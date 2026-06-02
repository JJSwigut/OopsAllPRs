package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import kotlinx.coroutines.delay

/** Formats whole seconds as m:ss. */
internal fun formatRest(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val rem = s % 60
    return "$m:${rem.toString().padStart(2, '0')}"
}

/**
 * Countdown rest timer rendered inside a progress ring. Ticks every second while [running];
 * ramps a [HapticType.Tick] in the final 3 seconds and fires [HapticType.Success] at zero.
 */
@Composable
fun FitRestTimer(
    totalSeconds: Int,
    running: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var remaining by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }
    val haptics = FitTheme.haptics

    LaunchedEffect(running, totalSeconds) {
        if (!running) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
            if (remaining in 1..3) haptics.perform(HapticType.Tick)
        }
        haptics.perform(HapticType.Success)
        onFinished()
    }

    val progress = if (totalSeconds == 0) 0f else remaining.toFloat() / totalSeconds.toFloat()

    FitProgressRing(progress = progress, modifier = modifier) {
        BasicText(
            text = formatRest(remaining),
            style = FitTheme.type.stat.copy(color = FitTheme.colors.onSurface),
        )
    }
}
