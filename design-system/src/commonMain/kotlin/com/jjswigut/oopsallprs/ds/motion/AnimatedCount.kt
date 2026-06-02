package com.jjswigut.oopsallprs.ds.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/** Splits a non-negative integer into its digit characters, most-significant first. */
fun formatCountDigits(value: Int): List<String> =
    value.coerceAtLeast(0).toString().map { it.toString() }

/**
 * Tabular digit roll: when [value] changes, each digit place slides up (increase) or down
 * (decrease). Tabular numerals keep widths fixed so the row never reflows. Collapses to an
 * instant swap when reduce-motion is on.
 */
@Composable
fun AnimatedCount(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = FitTheme.type.stat,
) {
    val digits = formatCountDigits(value)
    val reduceMotion = FitTheme.reduceMotion
    val color = FitTheme.colors.onSurface
    val stiffness = FitTheme.motion.smoothStiffness

    Row(modifier = modifier) {
        digits.forEach { digit ->
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    if (reduceMotion) {
                        slideInVertically { 0 } togetherWith slideOutVertically { 0 }
                    } else {
                        val rising = (targetState.toIntOrNull() ?: 0) >= (initialState.toIntOrNull() ?: 0)
                        val dir = if (rising) -1 else 1
                        slideInVertically(spring(stiffness = stiffness)) { it * -dir } togetherWith
                            slideOutVertically(spring(stiffness = stiffness)) { it * dir }
                    }
                },
                label = "digit",
            ) { d ->
                BasicText(text = d, style = style.copy(color = color))
            }
        }
    }
}
