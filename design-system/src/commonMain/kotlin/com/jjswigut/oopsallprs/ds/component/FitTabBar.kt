package com.jjswigut.oopsallprs.ds.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/** One tab definition. [icon] receives the resolved tint color so it can match the active state. */
data class FitTabItem(
    val label: String,
    val icon: @Composable (tint: Color) -> Unit,
)

@Composable
fun FitTabBar(
    items: List<FitTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = FitTheme.haptics
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FitTheme.spacing.sm, vertical = FitTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            val tint by animateColorAsState(
                targetValue = if (selected) FitTheme.colors.accent else FitTheme.colors.onSurfaceMuted,
                animationSpec = FitTheme.motion.smoothSpec(),
                label = "tabTint",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(FitTheme.size.touchMin + FitTheme.spacing.md)
                    .selectable(selected = selected, role = Role.Tab) {
                        if (!selected) haptics.perform(HapticType.Selection)
                        onSelect(i)
                    }
                    .clearAndSetSemantics {
                        role = Role.Tab
                        this.selected = selected
                        contentDescription = item.label
                        stateDescription = fitSelectionStateDescription(selected)
                        onClick(label = item.label) {
                            if (!selected) haptics.perform(HapticType.Selection)
                            onSelect(i)
                            true
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-8).dp),
                    contentAlignment = Alignment.Center
                ) {
                    item.icon(tint)
                }
                BasicText(
                    text = item.label,
                    modifier = Modifier
                        .align(Alignment.BottomCenter),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FitTheme.type.caption.copy(color = tint)
                )
            }
        }
    }
}

internal fun fitSelectionStateDescription(selected: Boolean): String =
    if (selected) "Selected" else "Not selected"
