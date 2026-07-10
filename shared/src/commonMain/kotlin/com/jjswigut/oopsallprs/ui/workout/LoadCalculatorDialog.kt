package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitDialog
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText

@Composable
fun LoadCalculatorDialog(
    kind: LoadCalculatorKind,
    exerciseName: String,
    weightUnit: WeightUnit,
    initialBarWeight: Double = defaultBarbellState(weightUnit).barWeight,
    onBarWeightSelected: (Double) -> Unit = {},
    onApply: (WeightKg) -> Unit,
    onDismiss: () -> Unit
) {
    FitDialog(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            FoundationText("EZ calc")
            FoundationMutedText(exerciseName)
            when (kind) {
                LoadCalculatorKind.BARBELL -> BarbellCalculatorContent(
                    weightUnit = weightUnit,
                    initialBarWeight = initialBarWeight,
                    onBarWeightSelected = onBarWeightSelected,
                    onApply = onApply,
                    onDismiss = onDismiss
                )

                LoadCalculatorKind.DUMBBELL -> DumbbellCalculatorContent(
                    weightUnit = weightUnit,
                    onApply = onApply
                )
            }
        }
    }
}

@Composable
private fun BarbellCalculatorContent(
    weightUnit: WeightUnit,
    initialBarWeight: Double,
    onBarWeightSelected: (Double) -> Unit,
    onApply: (WeightKg) -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember(weightUnit, initialBarWeight) { mutableStateOf(defaultBarbellState(weightUnit).copy(barWeight = initialBarWeight)) }
    Column(
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        FoundationMutedText("Bar")
        PresetRows(
            values = barbellBarPresets(weightUnit),
            labelFor = { "${formatLoadValue(it)} ${weightUnitLabel(weightUnit)}" },
            selectedValue = state.barWeight,
            onSelect = {
                state = state.copy(barWeight = it)
                onBarWeightSelected(it)
            }
        )
        FoundationMutedText("Plates per side")
        PresetRows(
            values = barbellPlatePresets(weightUnit),
            labelFor = { plate ->
                val count = state.platePairs[plate] ?: 0
                if (count == 0) formatLoadValue(plate) else "${formatLoadValue(plate)} x$count"
            },
            onSelect = { state = state.addPlatePair(it) }
        )
        FoundationText("Total ${formatLoadValue(state.totalDisplayWeight())} ${weightUnitLabel(weightUnit)}")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FitButton(
                text = "Clear",
                onClick = { state = state.clearPlates() },
                modifier = Modifier.weight(1f),
                style = FitButtonStyle.Secondary
            )
            FitButton(
                text = "Apply",
                onClick = {
                    onApply(state.toWeightKg(weightUnit))
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                style = FitButtonStyle.Primary
            )
        }
    }
}

@Composable
private fun DumbbellCalculatorContent(
    weightUnit: WeightUnit,
    onApply: (WeightKg) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        FoundationMutedText("Per dumbbell")
        PresetRows(
            values = dumbbellPresets(weightUnit),
            labelFor = { "${formatLoadValue(it)} ${weightUnitLabel(weightUnit)}" },
            onSelect = { weight ->
                DumbbellLoadCalculatorState().select(weight).toWeightKg(weightUnit)?.let(onApply)
            }
        )
    }
}

@Composable
private fun PresetRows(
    values: List<Double>,
    labelFor: (Double) -> String,
    selectedValue: Double? = null,
    onSelect: (Double) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
    ) {
        values.chunked(3).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
            ) {
                rowValues.forEach { value ->
                    val selectedModifier = if (selectedValue == value) {
                        Modifier.border(
                            width = 1.dp,
                            color = FitTheme.colors.accent,
                            shape = FitTheme.shapes.pillShape
                        )
                    } else {
                        Modifier
                    }
                    FitButton(
                        text = labelFor(value),
                        onClick = { onSelect(value) },
                        modifier = selectedModifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                }
            }
        }
    }
}
