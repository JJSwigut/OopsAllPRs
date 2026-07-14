package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedbackKind
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction

@Composable
fun SetRow(
    draft: SetRowDraft,
    onRepsChange: (Int?) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onDurationChange: (Long?) -> Unit = {},
    onDistanceChange: (Double?) -> Unit = {},
    onDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit = {},
    onWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit = {},
    onEffortChange: (MeasureInputUpdate<Effort>) -> Unit = {},
    onTimerToggle: () -> Unit = {},
    onLog: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    weightStepAmount: Double = weightStep(weightUnit),
    loadCalculatorKind: LoadCalculatorKind? = null,
    onOpenLoadCalculator: (() -> Unit)? = null,
    actionLabel: String = "Log set",
    pendingLabel: String = "Logging...",
    modifier: Modifier = Modifier
) {
    CompactSetInput(
        draft = draft,
        onRepsChange = onRepsChange,
        onWeightChange = onWeightChange,
        onDurationChange = onDurationChange,
        onDistanceChange = onDistanceChange,
        onDistanceInputChange = onDistanceInputChange,
        onWeightInputChange = onWeightInputChange,
        onEffortChange = onEffortChange,
        onTimerToggle = onTimerToggle,
        onLog = onLog,
        weightUnit = weightUnit,
        weightStepAmount = weightStepAmount,
        loadCalculatorKind = loadCalculatorKind,
        onOpenLoadCalculator = onOpenLoadCalculator,
        actionLabel = actionLabel,
        pendingLabel = pendingLabel,
        modifier = modifier
    )
}

@Composable
fun LoggedSetRowView(
    row: LoggedSetRow,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FitTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            val value = row.loggingConfiguration?.let { configuration ->
                configuration.measures.mapNotNull { measure ->
                    when (measure.kind) {
                        MeasureKind.REPETITIONS -> row.reps?.let { "$it reps" }
                        MeasureKind.LOAD -> row.weight?.let { weight ->
                            if (measure.loadRole == LoadRole.ADDED_TO_BODYWEIGHT && weight.value == 0.0) {
                                "Unloaded"
                            } else {
                                "${requireNotNull(measure.loadRole).displayLabel(weightUnit)} " +
                                    "${formatDisplayWeight(weight, weightUnit)} ${weightUnitLabel(weightUnit)}"
                            }
                        }
                        MeasureKind.DURATION -> row.durationMs?.let(::formatDurationMs)
                        MeasureKind.DISTANCE -> row.distanceMeters?.let { "${it.formatCompact()} m" }
                    }
                }.plus(listOfNotNull(row.observedEffort?.displayLabel())).joinToString(" • ")
            } ?: "Configuration unavailable"
            FoundationText(
                text = "${row.position.value + 1}. $value",
                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
            )
            row.prFeedback?.let { feedback ->
                FoundationText(
                    text = feedback.displayLabel(row.reps, weightUnit),
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.success)
                )
            } ?: FoundationMutedText(if (row.editedAt != null) "Edited" else "Logged")
        }
        if (row.loggingConfiguration != null) FoundationTextAction("Edit", onEdit)
        FoundationTextAction("Delete", onDelete)
    }
}

private fun Effort.displayLabel(): String? =
    when {
        rir != null -> "RIR $rir"
        rpeTenths != null -> "RPE ${(rpeTenths / 10.0).formatCompact()}"
        failureOutcome != null -> when (failureOutcome) {
            com.jjswigut.oopsallprs.domain.model.FailureOutcome.REACHED -> "Failure reached"
            com.jjswigut.oopsallprs.domain.model.FailureOutcome.NOT_REACHED -> "Failure not reached"
        }
        else -> null
    }

internal fun ActivePrFeedback.displayLabel(reps: Int?, weightUnit: WeightUnit): String {
    val prefix = if (previousValue == null) "New PR" else "PR"
    val value = when (kind) {
        ActivePrFeedbackKind.BODYWEIGHT_REPS -> "${newValue.toInt()} reps"
        ActivePrFeedbackKind.WEIGHT_FOR_REPS -> {
            val weight = WeightKg(newValue)
            "${formatDisplayWeight(weight, weightUnit)} ${weightUnitLabel(weightUnit)} x ${reps ?: 0}"
        }
        ActivePrFeedbackKind.TIME -> formatDurationMs(newValue.toLong())
    }
    return "$prefix: $value"
}
