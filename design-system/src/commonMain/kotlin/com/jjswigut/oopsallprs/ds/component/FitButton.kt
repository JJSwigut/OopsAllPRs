package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick as semanticsOnClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

enum class FitButtonStyle { Primary, Secondary }

@Composable
fun FitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FitButtonStyle = FitButtonStyle.Primary,
) {
    val shape = FitTheme.shapes.pillShape
    val base = modifier
        .defaultMinSize(minHeight = FitTheme.size.controlHeight)
        .semantics { role = Role.Button }

    val styled = when (style) {
        FitButtonStyle.Primary -> base.background(
            brush = Brush.verticalGradient(
                0f to lerp(FitTheme.colors.accent, Color.White, 0.12f),
                1f to FitTheme.colors.accent,
            ),
            shape = shape,
        )
        FitButtonStyle.Secondary -> base.fitGlass(shape = shape, glow = FitTheme.glow.none)
    }

    Box(
        modifier = styled
            .pressable(
                enabled = enabled,
                haptic = if (style == FitButtonStyle.Primary) HapticType.Medium else HapticType.Light,
                onClick = onClick,
            )
            .padding(horizontal = FitTheme.spacing.lg, vertical = FitTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = FitTheme.type.label.copy(
                color = if (style == FitButtonStyle.Primary) FitTheme.colors.onAccent
                else FitTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FitIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = FitTheme.size.touchMin, minHeight = FitTheme.size.touchMin)
            .fitGlass(shape = FitTheme.shapes.pillShape, glow = FitTheme.glow.none)
            .pressable(enabled = enabled, haptic = HapticType.Light, onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                this.contentDescription = contentDescription
                if (enabled) {
                    semanticsOnClick(label = contentDescription) {
                        onClick()
                        true
                    }
                } else {
                    disabled()
                }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}
