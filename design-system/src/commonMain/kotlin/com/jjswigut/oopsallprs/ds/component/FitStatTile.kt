package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ds.motion.AnimatedCount
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ds.token.FitGlowLevel

/** A glass tile showing one big animated number with a label and optional unit. */
@Composable
fun FitStatTile(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    unit: String = "",
    glow: FitGlowLevel = FitTheme.glow.none,
) {
    FitCard(modifier = modifier, glow = glow) {
        BasicText(
            text = label.uppercase(),
            style = FitTheme.type.caption.copy(color = FitTheme.colors.onSurfaceMuted),
        )
        Row(
            modifier = Modifier.padding(top = FitTheme.spacing.xs),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs),
        ) {
            AnimatedCount(value = value, style = FitTheme.type.stat.copy(color = FitTheme.colors.onSurface))
            if (unit.isNotEmpty()) {
                BasicText(
                    text = unit,
                    style = FitTheme.type.label.copy(color = FitTheme.colors.onSurfaceMuted),
                )
            }
        }
    }
}
