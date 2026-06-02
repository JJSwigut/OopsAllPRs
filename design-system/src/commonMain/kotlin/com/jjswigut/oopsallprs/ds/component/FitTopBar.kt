package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun FitTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .fitGlass(shape = FitTheme.shapes.mediumShape, glow = FitTheme.glow.low)
            .padding(horizontal = FitTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
    ) {
        if (navigation != null) {
            Box(contentAlignment = Alignment.Center) { navigation() }
        }
        BasicText(
            text = title,
            modifier = Modifier.weight(1f),
            style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface),
        )
        actions()
    }
}
