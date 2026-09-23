package com.jjswigut.oopsallprs.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme
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
    onCancelTemplateSave: () -> Unit = {},
    templateSavedName: String? = null,
    onOpenTrain: () -> Unit = {},
    onViewProgress: () -> Unit = {},
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
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        state.completionNotice?.let { message ->
            FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.onSurface))
        }
        templateSaveDraft?.takeIf {
            it.isSaving && it.completedWorkoutId != state.selectedSummary?.workoutId
        }?.let { pendingSave ->
            FoundationMutedText(
                "Saving template: ${pendingSave.name}",
                modifier = Modifier.fillMaxWidth()
            )
        }
        state.selectedSummary?.let { summary ->
            CompletedWorkoutDetail(
                summary = summary,
                weightUnit = weightUnit,
                templateSaveDraft = templateSaveDraft,
                onBack = onBack,
                onStartSaveTemplate = onStartSaveTemplate,
                onTemplateNameChange = onTemplateNameChange,
                onSaveTemplate = onSaveTemplate,
                onCancelTemplateSave = onCancelTemplateSave,
                templateSavedName = templateSavedName,
                onOpenTrain = onOpenTrain,
                onViewProgress = onViewProgress,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoundationText(
                "History",
                modifier = Modifier.weight(1f).semantics { heading() },
                style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText(historyCountLabel(state.rows.size, "workout"))
        }
        if (state.rows.isEmpty()) {
            FoundationMutedText("No completed workouts yet")
            FitButton(
                text = "Plan your first workout",
                onClick = onOpenTrain,
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Primary
            )
        } else {
            state.rows.forEach { row ->
                FitCard(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = FitTheme.size.touchMin)
                        .semantics { role = Role.Button }
                        .pressable(haptic = HapticType.Light, onClick = { onSelectWorkout(row.workoutId) }),
                    glow = FitTheme.glow.none
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                        FoundationText(
                            row.title,
                            style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                        )
                        FoundationMutedText(row.dateLabel)
                        HistoryWorkoutCounts(row.durationLabel, row.exerciseCount, row.setCount)
                    }
                }
            }
        }
        state.errorMessage?.let { message ->
            FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletedWorkoutDetail(
    summary: CompletedWorkoutSummary,
    weightUnit: WeightUnit,
    templateSaveDraft: TemplateSaveDraft?,
    onBack: () -> Unit,
    onStartSaveTemplate: (FoundationId) -> Unit,
    onTemplateNameChange: (String) -> Unit,
    onSaveTemplate: () -> Unit,
    onCancelTemplateSave: () -> Unit,
    templateSavedName: String?,
    onOpenTrain: () -> Unit,
    onViewProgress: () -> Unit,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                FoundationText(
                    "Completed workout",
                    modifier = Modifier.semantics { heading() },
                    style = FitTheme.type.body.copy(
                        color = FitTheme.colors.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                FoundationMutedText(summary.finishedAt.historyDateLabel())
            }
            FoundationTextAction("Back", onBack)
        }
        HistoryWorkoutCounts(summary.durationLabel, summary.exerciseCount, summary.setCount)
        if (summary.prCount > 0) {
            val prSetCount = summary.exercises.sumOf { exercise ->
                exercise.setRows.count { it.prMarkers.isNotEmpty() }
            }
            FoundationText(
                "${historyCountLabel(summary.prCount, "PR")} across ${historyCountLabel(prSetCount, "set")}",
                modifier = Modifier.semantics { heading() },
                style = FitTheme.type.title.copy(color = FitTheme.colors.accent)
            )
        }
        val draft = templateSaveDraft?.takeIf { it.completedWorkoutId == summary.workoutId }
        if (draft?.isSaving != true) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                if (draft == null && templateSavedName == null) {
                    FitButton(
                        text = "Save as template",
                        onClick = { onStartSaveTemplate(summary.workoutId) },
                        enabled = templateSaveDraft?.isSaving != true,
                        style = FitButtonStyle.Secondary
                    )
                }
                FoundationTextAction("View progress", onViewProgress)
                FoundationTextAction("Edit workout", onEditWorkout)
            }
        }
        templateSavedName?.let { name ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                FoundationText(
                    "Template saved: $name",
                    modifier = Modifier.fillMaxWidth(),
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.success)
                )
                FoundationTextAction("Open in Train", onOpenTrain)
            }
        }
        if (draft != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
            ) {
                FoundationText("Template name", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                if (draft.isSaving) {
                    FoundationText(
                        draft.name,
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = FitTheme.size.controlHeight)
                            .padding(horizontal = FitTheme.spacing.lg, vertical = FitTheme.spacing.md)
                    )
                } else {
                    FitTextField(
                        value = draft.name,
                        onValueChange = onTemplateNameChange,
                        placeholder = "Template name",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                draft.errorMessage?.let { message ->
                    FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                ) {
                    FitButton(
                        text = if (draft.isSaving) "Saving..." else "Save template",
                        onClick = onSaveTemplate,
                        enabled = !draft.isSaving,
                        style = FitButtonStyle.Primary
                    )
                    FitButton(
                        text = "Cancel",
                        onClick = onCancelTemplateSave,
                        enabled = !draft.isSaving,
                        style = FitButtonStyle.Secondary
                    )
                }
            }
        }
        summary.exercises.forEach { exercise ->
            HistorySectionDivider()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
            ) {
                FoundationText(
                    exercise.displayName,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                    style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface, fontWeight = FontWeight.SemiBold)
                )
                exercise.setRows.forEach { row ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                    ) {
                        FoundationText(row.historyPerformanceLabel(weightUnit), modifier = Modifier.fillMaxWidth())
                        row.prMarkers.forEach { marker ->
                            FoundationText(
                                marker.historyLabel(weightUnit),
                                modifier = Modifier.fillMaxWidth(),
                                style = FitTheme.type.label.copy(color = FitTheme.colors.accent)
                            )
                        }
                    }
                }
            }
        }
        HistorySectionDivider()
        Column(modifier = Modifier.fillMaxWidth()) {
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
                FoundationTextAction(
                    label = "Delete workout",
                    onClick = onRequestDeleteWorkout,
                )
            }
        }
        errorMessage?.let { message ->
            FoundationText(message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryWorkoutCounts(durationLabel: String, exerciseCount: Int, setCount: Int) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        FoundationMutedText(durationLabel)
        FoundationMutedText(historyCountLabel(exerciseCount, "exercise"))
        FoundationMutedText(historyCountLabel(setCount, "set"))
    }
}

@Composable
private fun HistorySectionDivider() {
    Box(Modifier.fillMaxWidth().height(FitTheme.size.hairline).background(FitTheme.colors.border))
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
                        FoundationMutedText(row.historyPerformanceLabel(weightUnit))
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
