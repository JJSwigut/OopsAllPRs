package com.jjswigut.oopsallprs.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitStatTile
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import com.jjswigut.oopsallprs.ui.workout.CompactSetInput
import com.jjswigut.oopsallprs.ui.workout.MeasureInputUpdate

@Composable
fun HistoryFlow(
    state: HistoryState,
    weightUnit: WeightUnit = WeightUnit.POUNDS,
    templateSaveDraft: TemplateSaveDraft? = null,
    onSelectWorkout: (FoundationId) -> Unit = {},
    onBack: () -> Unit = {},
    onStartSaveTemplate: (FoundationId) -> Unit = {},
    onTemplateNameChange: (String) -> Unit = {},
    onSaveTemplate: () -> Unit = {},
    onRequestDeleteWorkout: () -> Unit = {},
    onCancelDeleteWorkout: () -> Unit = {},
    onConfirmDeleteWorkout: () -> Unit = {},
    onEditWorkout: () -> Unit = {},
    onCancelEditWorkout: () -> Unit = {},
    onSaveEditWorkout: () -> Unit = {},
    onEditSet: (FoundationId, FoundationId) -> Unit = { _, _ -> },
    onAddSet: (FoundationId) -> Unit = {},
    onCancelSetEdit: () -> Unit = {},
    onApplySetEdit: () -> Unit = {},
    onRepsChange: (Int?) -> Unit = {},
    onWeightChange: (WeightKg?) -> Unit = {},
    onWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit = {},
    onDurationChange: (Long?) -> Unit = {},
    onDistanceChange: (Double?) -> Unit = {},
    onDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit = {},
    onEffortChange: (MeasureInputUpdate<Effort>) -> Unit = {},
    onRequestDeleteSet: (FoundationId) -> Unit = {},
    onCancelDeleteSet: () -> Unit = {},
    onConfirmDeleteSet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        state.selectedSummary?.let { summary ->
            CompletedWorkoutDetail(
                summary = summary,
                weightUnit = weightUnit,
                templateSaveDraft = templateSaveDraft,
                onBack = onBack,
                onStartSaveTemplate = onStartSaveTemplate,
                onTemplateNameChange = onTemplateNameChange,
                onSaveTemplate = onSaveTemplate,
                pendingDeleteSummary = state.pendingDeleteSummary,
                errorMessage = state.errorMessage,
                onRequestDeleteWorkout = onRequestDeleteWorkout,
                onCancelDeleteWorkout = onCancelDeleteWorkout,
                onConfirmDeleteWorkout = onConfirmDeleteWorkout,
                editDraft = state.editDraft,
                onEditWorkout = onEditWorkout,
                onCancelEditWorkout = onCancelEditWorkout,
                onSaveEditWorkout = onSaveEditWorkout,
                onEditSet = onEditSet,
                onAddSet = onAddSet,
                onCancelSetEdit = onCancelSetEdit,
                onApplySetEdit = onApplySetEdit,
                onRepsChange = onRepsChange,
                onWeightChange = onWeightChange,
                onWeightInputChange = onWeightInputChange,
                onDurationChange = onDurationChange,
                onDistanceChange = onDistanceChange,
                onDistanceInputChange = onDistanceInputChange,
                onEffortChange = onEffortChange,
                onRequestDeleteSet = onRequestDeleteSet,
                onCancelDeleteSet = onCancelDeleteSet,
                onConfirmDeleteSet = onConfirmDeleteSet
            )
            return@Column
        }

        FitStatTile(
            label = "Workouts",
            value = state.rows.size,
            modifier = Modifier.fillMaxWidth(),
            glow = FitTheme.glow.none
        )
        if (state.rows.isEmpty()) {
            FitCard(glow = FitTheme.glow.none) {
                FoundationText(
                    text = "No completed workouts yet",
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
                FoundationMutedText("Finish a workout and it will show here with sets, PRs, and template actions.")
            }
        } else {
            state.rows.forEach { row ->
                FitCard(glow = FitTheme.glow.none) {
                    FitListRow(onClick = { onSelectWorkout(row.workoutId) }) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                        ) {
                            FoundationText(row.title, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText("${row.durationLabel} • ${row.exerciseCount} exercises • ${row.setCount} sets")
                        }
                    }
                }
            }
        }
        state.errorMessage?.let { message ->
            FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
        }
    }
}

@Composable
private fun CompletedWorkoutDetail(
    summary: CompletedWorkoutSummary,
    weightUnit: WeightUnit,
    templateSaveDraft: TemplateSaveDraft?,
    onBack: () -> Unit,
    onStartSaveTemplate: (FoundationId) -> Unit,
    onTemplateNameChange: (String) -> Unit,
    onSaveTemplate: () -> Unit,
    pendingDeleteSummary: CompletedWorkoutSummary?,
    errorMessage: String?,
    onRequestDeleteWorkout: () -> Unit,
    onCancelDeleteWorkout: () -> Unit,
    onConfirmDeleteWorkout: () -> Unit,
    editDraft: CompletedWorkoutEditDraft?,
    onEditWorkout: () -> Unit,
    onCancelEditWorkout: () -> Unit,
    onSaveEditWorkout: () -> Unit,
    onEditSet: (FoundationId, FoundationId) -> Unit,
    onAddSet: (FoundationId) -> Unit,
    onCancelSetEdit: () -> Unit,
    onApplySetEdit: () -> Unit,
    onRepsChange: (Int?) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit,
    onDurationChange: (Long?) -> Unit,
    onDistanceChange: (Double?) -> Unit,
    onDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit,
    onEffortChange: (MeasureInputUpdate<Effort>) -> Unit,
    onRequestDeleteSet: (FoundationId) -> Unit,
    onCancelDeleteSet: () -> Unit,
    onConfirmDeleteSet: () -> Unit
) {
    val activeEdit = editDraft?.takeIf { it.workout.id == summary.workoutId }
    if (activeEdit != null) {
        CompletedWorkoutEditor(
            editDraft = activeEdit,
            weightUnit = weightUnit,
            onCancel = onCancelEditWorkout,
            onSave = onSaveEditWorkout,
            onEditSet = onEditSet,
            onAddSet = onAddSet,
            onCancelSetEdit = onCancelSetEdit,
            onApplySetEdit = onApplySetEdit,
            onRepsChange = onRepsChange,
            onWeightChange = onWeightChange,
            onWeightInputChange = onWeightInputChange,
            onDurationChange = onDurationChange,
            onDistanceChange = onDistanceChange,
            onDistanceInputChange = onDistanceInputChange,
            onEffortChange = onEffortChange,
            onRequestDeleteSet = onRequestDeleteSet,
            onCancelDeleteSet = onCancelDeleteSet,
            onConfirmDeleteSet = onConfirmDeleteSet
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                FoundationText("Completed workout", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText(
                    "${summary.finishedAt.shortDateLabel()} • ${summary.durationLabel} • " +
                        "${summary.exerciseCount} exercises • ${summary.setCount} sets"
                )
            }
            FoundationTextAction("Back", onBack)
        }
        summary.exercises.forEach { exercise ->
            FitCard(glow = FitTheme.glow.none) {
                FoundationText(exercise.displayName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                exercise.setRows.forEach { row ->
                    FoundationMutedText(row.historyDisplayLabel(weightUnit))
                }
            }
        }
        FitButton(
            text = "Edit workout",
            onClick = onEditWorkout,
            modifier = Modifier.fillMaxWidth(),
            style = FitButtonStyle.Primary
        )
        FitCard(glow = FitTheme.glow.none) {
            val draft = templateSaveDraft?.takeIf { it.completedWorkoutId == summary.workoutId }
            if (draft == null) {
                FitButton(
                    text = "Save as template",
                    onClick = { onStartSaveTemplate(summary.workoutId) },
                    style = FitButtonStyle.Secondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                    FoundationText("Template name", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                    FitTextField(
                        value = draft.name,
                        onValueChange = onTemplateNameChange,
                        placeholder = "Template name",
                        modifier = Modifier.fillMaxWidth()
                    )
                    draft.errorMessage?.let { message ->
                        FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
                    }
                    FitButton(
                        text = "Save template",
                        onClick = onSaveTemplate,
                        style = FitButtonStyle.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        FitCard(glow = FitTheme.glow.none) {
            val isConfirmingDelete = pendingDeleteSummary?.workoutId == summary.workoutId
            if (isConfirmingDelete) {
                Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                    FoundationText("Delete workout?", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                    FoundationMutedText("This removes the completed workout and recalculates PRs.")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FitButton(
                            text = "Cancel",
                            onClick = onCancelDeleteWorkout,
                            modifier = Modifier.weight(1f),
                            style = FitButtonStyle.Secondary
                        )
                        FitButton(
                            text = "Delete",
                            onClick = onConfirmDeleteWorkout,
                            modifier = Modifier.weight(1f),
                            style = FitButtonStyle.Primary
                        )
                    }
                }
            } else {
                FitButton(
                    text = "Delete workout",
                    onClick = onRequestDeleteWorkout,
                    modifier = Modifier.fillMaxWidth(),
                    style = FitButtonStyle.Secondary
                )
            }
        }
        errorMessage?.let { message ->
            FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
        }
    }
}

@Composable
private fun CompletedWorkoutEditor(
    editDraft: CompletedWorkoutEditDraft,
    weightUnit: WeightUnit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onEditSet: (FoundationId, FoundationId) -> Unit,
    onAddSet: (FoundationId) -> Unit,
    onCancelSetEdit: () -> Unit,
    onApplySetEdit: () -> Unit,
    onRepsChange: (Int?) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit,
    onDurationChange: (Long?) -> Unit,
    onDistanceChange: (Double?) -> Unit,
    onDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit,
    onEffortChange: (MeasureInputUpdate<Effort>) -> Unit,
    onRequestDeleteSet: (FoundationId) -> Unit,
    onCancelDeleteSet: () -> Unit,
    onConfirmDeleteSet: () -> Unit
) {
    val summary = editDraft.workout.toSummary()
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
        FitCard(glow = FitTheme.glow.none) {
            FoundationText("Editing completed workout", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
            FoundationMutedText("Changes stay in this draft until you save.")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
            ) {
                FitButton("Cancel", onCancel, Modifier.weight(1f), style = FitButtonStyle.Secondary)
                FitButton(
                    if (editDraft.isSaving) "Saving..." else "Save changes",
                    onSave,
                    Modifier.weight(1f),
                    enabled = !editDraft.isSaving && editDraft.setEditor == null,
                    style = FitButtonStyle.Primary
                )
            }
        }
        summary.exercises.forEach { exercise ->
            FitCard(glow = FitTheme.glow.none) {
                FoundationText(exercise.displayName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                exercise.setRows.forEach { row ->
                    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                        FoundationMutedText(row.historyDisplayLabel(weightUnit))
                        if (editDraft.pendingDeleteSetId == row.setId) {
                            FoundationText("Delete this set?", style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
                            Row(horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                                FoundationTextAction("Keep set", onCancelDeleteSet)
                                FoundationTextAction("Delete set", onConfirmDeleteSet)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                                FoundationTextAction("Edit", { onEditSet(exercise.completedExerciseId, row.setId) })
                                FoundationTextAction("Delete", { onRequestDeleteSet(row.setId) })
                            }
                        }
                    }
                }
                FoundationTextAction("Add missed set", { onAddSet(exercise.completedExerciseId) })
            }
        }
        editDraft.setEditor?.let { editor ->
            FitCard(glow = FitTheme.glow.none) {
                FoundationText(
                    if (editor.originalSetId == null) "Add missed set" else "Edit set",
                    style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                )
                CompactSetInput(
                    draft = editor.draft,
                    onRepsChange = onRepsChange,
                    onWeightChange = onWeightChange,
                    onWeightInputChange = onWeightInputChange,
                    onDurationChange = onDurationChange,
                    onDistanceChange = onDistanceChange,
                    onDistanceInputChange = onDistanceInputChange,
                    onEffortChange = onEffortChange,
                    onLog = onApplySetEdit,
                    weightUnit = weightUnit,
                    actionLabel = "Apply set",
                    showTimerControl = false,
                    modifier = Modifier.fillMaxWidth()
                )
                FitButton("Cancel set edit", onCancelSetEdit, Modifier.fillMaxWidth(), style = FitButtonStyle.Secondary)
            }
        }
        editDraft.errorMessage?.let { message ->
            FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
        }
    }
}
