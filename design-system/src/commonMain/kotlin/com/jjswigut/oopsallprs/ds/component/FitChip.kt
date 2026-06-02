package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun FitChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = FitTheme.haptics
    val shape = FitTheme.shapes.pillShape
    val surface = modifier
        .defaultMinSize(minHeight = 44.dp)
        .selectable(selected = selected, role = Role.Tab) {
            haptics.perform(HapticType.Selection)
            onClick()
        }

    val decorated =
        if (selected) {
            surface
                .background(FitTheme.colors.accent.copy(alpha = 0.16f), shape)
                .fitGlass(shape = shape, glow = FitTheme.glow.low)
        } else {
            surface.fitGlass(shape = shape, glow = FitTheme.glow.none)
        }

    Box(
        modifier = decorated.padding(horizontal = FitTheme.spacing.lg, vertical = FitTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = FitTheme.type.label.copy(
                color = if (selected) FitTheme.colors.accent else FitTheme.colors.onSurfaceMuted,
            ),
        )
    }
}
