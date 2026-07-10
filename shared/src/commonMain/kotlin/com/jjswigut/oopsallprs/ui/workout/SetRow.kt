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
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction

@Composable
fun SetRow(
    draft: SetRowDraft,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onDurationChange: (Long?) -> Unit = {},
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
            val weight = row.weight?.let { " @ ${formatDisplayWeight(it, weightUnit)} ${weightUnitLabel(weightUnit)}" }.orEmpty()
            val value = if (row.setKind == com.jjswigut.oopsallprs.domain.model.SetKind.TIMED) {
                formatDurationMs(row.durationMs)
            } else {
                "${row.reps ?: 0} reps$weight"
            }
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
        FoundationTextAction("Edit", onEdit)
        FoundationTextAction("Delete", onDelete)
    }
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
