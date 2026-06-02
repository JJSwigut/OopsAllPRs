package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun FitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    selectAllOnFocus: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length))
        }
    }

    LaunchedEffect(focused, selectAllOnFocus) {
        if (focused && selectAllOnFocus) {
            fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
        }
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            onValueChange(it.text)
        },
        modifier = modifier
            .fitGlass(
                shape = FitTheme.shapes.mediumShape,
                glow = if (focused) FitTheme.glow.low else FitTheme.glow.none,
            )
            .defaultMinSize(minHeight = FitTheme.size.controlHeight)
            .padding(horizontal = FitTheme.spacing.lg, vertical = FitTheme.spacing.md),
        textStyle = FitTheme.type.body.copy(color = FitTheme.colors.onSurface),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(FitTheme.colors.accent),
        interactionSource = interaction,
        decorationBox = { inner ->
            Box {
                if (fieldValue.text.isEmpty() && placeholder.isNotEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = FitTheme.type.body.copy(color = FitTheme.colors.onSurfaceMuted),
                    )
                }
                inner()
            }
        },
    )
}
