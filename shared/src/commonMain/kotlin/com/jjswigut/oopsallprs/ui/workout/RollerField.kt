package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.jjswigut.oopsallprs.ds.component.FitIconButton
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText

@Composable
fun RepsRollerField(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactStepperField(
        label = "Reps",
        value = reps.coerceAtLeast(0).toString(),
        onDecrease = { onRepsChange((reps - 1).coerceAtLeast(0)) },
        onIncrease = { onRepsChange(reps + 1) },
        modifier = modifier
    )
}

@Composable
fun WeightRollerField(
    weight: Double,
    onWeightChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactStepperField(
        label = "Weight",
        value = "${formatWeight(weight)} kg",
        onDecrease = { onWeightChange((weight - 2.5).coerceAtLeast(0.0)) },
        onIncrease = { onWeightChange(weight + 2.5) },
        modifier = modifier
    )
}

@Composable
private fun CompactStepperField(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
        FoundationMutedText(label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            FitIconButton(
                onClick = onDecrease,
                contentDescription = "Decrease $label",
                modifier = Modifier.weight(1f),
            ) {
                FoundationText(
                    text = "-",
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
            }
            FoundationText(
                text = value,
                modifier = Modifier.weight(1.4f),
                style = FitTheme.type.title.copy(
                    color = FitTheme.colors.accent,
                    textAlign = TextAlign.Center
                )
            )
            FitIconButton(
                onClick = onIncrease,
                contentDescription = "Increase $label",
                modifier = Modifier.weight(1f),
            ) {
                FoundationText(
                    text = "+",
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
            }
        }
    }
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
