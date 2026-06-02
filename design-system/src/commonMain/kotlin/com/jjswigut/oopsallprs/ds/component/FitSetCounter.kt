package com.jjswigut.oopsallprs.ds.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/** Row of dots showing completed vs remaining sets. [completed] of [total] are filled with accent. */
@Composable
fun FitSetCounter(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
    ) {
        repeat(total) { i ->
            val filled = i < completed
            val color by animateColorAsState(
                targetValue = if (filled) FitTheme.colors.accent else FitTheme.colors.border,
                animationSpec = FitTheme.motion.smoothSpec(),
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color = color, shape = FitTheme.shapes.pillShape),
            )
        }
    }
}
