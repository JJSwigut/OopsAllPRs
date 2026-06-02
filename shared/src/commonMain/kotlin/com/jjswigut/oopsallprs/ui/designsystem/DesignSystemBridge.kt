package com.jjswigut.oopsallprs.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitPalettes
import com.jjswigut.oopsallprs.ds.theme.FitPalette
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun OopsAllPrsFoundationTheme(
    palette: FitPalette = FitPalettes.IceDark,
    reduceMotion: Boolean = false,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FitTheme(
        palette = palette,
        reduceMotion = reduceMotion,
        hapticsEnabled = hapticsEnabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FitTheme.colors.background)
        ) {
            content()
        }
    }
}

@Composable
fun FoundationActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true
) {
    FitButton(
        text = label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = if (primary) FitButtonStyle.Primary else FitButtonStyle.Secondary
    )
}

@Composable
fun FoundationTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = FitTheme.size.touchMin, minHeight = FitTheme.size.touchMin)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .pressable(haptic = HapticType.Light, onClick = onClick)
            .padding(horizontal = FitTheme.spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = FitTheme.type.label.copy(color = FitTheme.colors.accent)
        )
    }
}

@Composable
fun FoundationText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
) {
    BasicText(text = text, modifier = modifier, style = style)
}

@Composable
fun FoundationMutedText(
    text: String,
    modifier: Modifier = Modifier
) {
    FoundationText(
        text = text,
        modifier = modifier,
        style = FitTheme.type.caption.copy(color = FitTheme.colors.onSurfaceMuted)
    )
}
