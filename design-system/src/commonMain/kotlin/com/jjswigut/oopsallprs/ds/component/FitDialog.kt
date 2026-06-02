package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jjswigut.oopsallprs.ds.foundation.glass
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import kotlin.math.max

private const val MinimumDialogBorderAlpha = 0.32f

/** Modal dialog surface. Uses the Fit glass treatment with an opaque fill for legibility. */
@Composable
fun FitDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = FitTheme.shapes.largeShape
    val border = FitTheme.colors.border.copy(
        alpha = max(FitTheme.colors.border.alpha, MinimumDialogBorderAlpha),
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .glass(
                    shape = shape,
                    fill = FitTheme.colors.surface,
                    borderColor = border,
                    glowColor = FitTheme.colors.borderGlow,
                    glow = FitTheme.glow.none,
                )
                .padding(FitTheme.spacing.xl),
            content = content,
        )
    }
}
