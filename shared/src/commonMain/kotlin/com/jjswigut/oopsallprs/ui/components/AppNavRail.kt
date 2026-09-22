package com.jjswigut.oopsallprs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.navigation.TopLevelDestination

@Composable
fun AppNavRail(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    FitCard(modifier = modifier.width(156.dp), glow = FitTheme.glow.low) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
        ) {
            destinations.forEach { destination ->
                val isSelected = destination == selected
                FitListRow(
                    modifier = Modifier.semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
                    onClick = { onSelect(destination) }
                ) {
                    BasicText(
                        text = destination.label,
                        style = FitTheme.type.label.copy(
                            color = if (isSelected) FitTheme.colors.accent else FitTheme.colors.onSurfaceMuted
                        )
                    )
                }
            }
        }
    }
}
