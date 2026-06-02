package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

internal fun sliderFraction(value: Float, min: Float, max: Float): Float =
    ((value - min) / (max - min)).coerceIn(0f, 1f)

internal fun valueFromFraction(fraction: Float, min: Float, max: Float): Float =
    min + (max - min) * fraction.coerceIn(0f, 1f)

@Composable
fun FitSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val fraction = sliderFraction(value, min, max)
    val haptics = FitTheme.haptics
    val track = FitTheme.colors.border
    val accent = FitTheme.colors.accent
    val glow = FitTheme.colors.accentGlow

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(FitTheme.size.touchMin)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, min..max)
                setProgress { target -> onValueChange(target); true }
            }
            .pointerInput(min, max) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onValueChange(valueFromFraction(offset.x / size.width, min, max))
                        haptics.perform(HapticType.Selection)
                    },
                ) { change, _ ->
                    val f = (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(valueFromFraction(f, min, max))
                }
            },
    ) {
        val cy = size.height / 2f
        val strokePx = 8.dp.toPx()
        drawLine(
            color = track,
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            strokeWidth = strokePx,
        )
        drawLine(
            color = accent,
            start = Offset(0f, cy),
            end = Offset(size.width * fraction, cy),
            strokeWidth = strokePx,
        )
        val thumbX = size.width * fraction
        drawCircle(color = glow.copy(alpha = 0.35f), radius = strokePx * 2.2f, center = Offset(thumbX, cy))
        drawCircle(color = accent, radius = strokePx * 1.3f, center = Offset(thumbX, cy))
    }
}
