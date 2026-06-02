package com.jjswigut.oopsallprs.ds.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun FitToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val trackWidth = 64.dp
    val trackHeight = 36.dp
    val thumb = 28.dp
    val haptics = FitTheme.haptics

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumb - 4.dp else 4.dp,
        animationSpec = FitTheme.motion.snappySpec(),
        label = "thumb",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) FitTheme.colors.accent else FitTheme.colors.onSurfaceMuted,
        animationSpec = FitTheme.motion.smoothSpec(),
        label = "thumbColor",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { next ->
                    haptics.perform(HapticType.Selection)
                    onCheckedChange(next)
                },
            )
            .clearAndSetSemantics {
                role = Role.Switch
                label?.let { contentDescription = it }
                stateDescription = fitToggleStateDescription(checked)
                if (enabled) {
                    onClick(label = label) {
                        haptics.perform(HapticType.Selection)
                        onCheckedChange(!checked)
                        true
                    }
                } else {
                    disabled()
                }
            }
            .fitGlass(shape = FitTheme.shapes.pillShape, glow = FitTheme.glow.none),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumb)
                .background(color = thumbColor, shape = FitTheme.shapes.pillShape),
        )
    }
}

internal fun fitToggleStateDescription(checked: Boolean): String =
    if (checked) "On" else "Off"
