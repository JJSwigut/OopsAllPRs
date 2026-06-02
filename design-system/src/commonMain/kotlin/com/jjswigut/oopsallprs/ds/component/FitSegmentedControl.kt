package com.jjswigut.oopsallprs.ds.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun FitSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = FitTheme.haptics
    val shape = FitTheme.shapes.pillShape
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = FitTheme.motion.snappy,
        label = "segIndex",
    )

    Box(
        modifier = modifier
            .height(FitTheme.size.controlHeight)
            .fitGlass(shape = shape, glow = FitTheme.glow.none)
            .padding(4.dp),
    ) {
        // sliding selector
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .layout { measurable, constraints ->
                    val segWidth = constraints.maxWidth / options.size
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = segWidth, maxWidth = segWidth),
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative(x = (animatedIndex * segWidth).toInt(), y = 0)
                    }
                }
                .clip(shape)
                .background(FitTheme.colors.accent.copy(alpha = 0.18f))
                .fitGlass(shape = shape, glow = FitTheme.glow.low),
        )
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab) {
                            if (!selected) haptics.perform(HapticType.Selection)
                            onSelect(i)
                        }
                        .clearAndSetSemantics {
                            role = Role.Tab
                            this.selected = selected
                            contentDescription = label
                            stateDescription = fitSelectionStateDescription(selected)
                            onClick(label = label) {
                                if (!selected) haptics.perform(HapticType.Selection)
                                onSelect(i)
                                true
                            }
                        },
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
        }
    }
}
