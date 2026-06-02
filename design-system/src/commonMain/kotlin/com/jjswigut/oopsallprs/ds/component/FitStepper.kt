package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ds.motion.AnimatedCount
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/** Pure step math: nudge [current] by [delta] steps of [step], clamped to [min]..[max]. */
fun stepValue(current: Int, delta: Int, step: Int, min: Int, max: Int): Int =
    (current + delta * step).coerceIn(min, max)

@Composable
fun FitStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    range: IntRange = 0..999,
) {
    Row(
        modifier = modifier.defaultMinSize(minHeight = FitTheme.size.controlHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.lg),
    ) {
        FitIconButton(
            onClick = { onValueChange(stepValue(value, -1, step, range.first, range.last)) },
            contentDescription = "Decrease",
        ) { BasicText("–", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)) }

        Box(contentAlignment = Alignment.Center) {
            AnimatedCount(value = value, style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
        }

        FitIconButton(
            onClick = { onValueChange(stepValue(value, +1, step, range.first, range.last)) },
            contentDescription = "Increase",
        ) { BasicText("+", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)) }
    }
}
