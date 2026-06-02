package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ds.token.FitGlowLevel

@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    shape: Shape = FitTheme.shapes.largeShape,
    glow: FitGlowLevel = FitTheme.glow.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fitGlass(shape = shape, glow = glow)
            .padding(FitTheme.spacing.lg),
        content = content,
    )
}
