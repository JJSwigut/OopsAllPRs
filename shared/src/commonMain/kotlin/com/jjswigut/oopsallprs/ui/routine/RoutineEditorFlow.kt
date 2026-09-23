package com.jjswigut.oopsallprs.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitDialog
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.common.RestDurationRoller
import com.jjswigut.oopsallprs.ui.common.formatRestDurationSeconds
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import com.jjswigut.oopsallprs.ui.exercise.exerciseKindLabel
import com.jjswigut.oopsallprs.ui.exercise.exerciseMetadataLine
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerResultRow
import com.jjswigut.oopsallprs.ui.workout.formatDurationMs
import com.jjswigut.oopsallprs.ui.workout.nextDurationMs
import com.jjswigut.oopsallprs.ui.workout.parseDurationInput

@Composable
fun RoutineEditorFlow(
    draft: RoutineEditorDraft,
    onNameChange: (String) -> Unit,
    onExerciseQueryChange: (String) -> Unit,
    onAddExercise: (ExercisePickerResultRow) -> Unit,
    onRemoveExercise: (FoundationId) -> Unit,
    onAddSet: (FoundationId) -> Unit,
    onRemoveSet: (FoundationId, FoundationId) -> Unit,
    onSetKindChange: (FoundationId, FoundationId, SetKind) -> Unit,
    onSetRepsChange: (FoundationId, FoundationId, Int?) -> Unit,
    onSetWeightChange: (FoundationId, FoundationId, WeightKg?) -> Unit,
    onSetDurationChange: (FoundationId, FoundationId, Long?) -> Unit,
    onAdjustRest: (FoundationId, Int) -> Unit,
    onToggleRest: (FoundationId) -> Unit,
    onGroupSelected: (List<FoundationId>) -> Unit,
    onUngroup: (FoundationId) -> Unit,
    onAdjustGroupRounds: (FoundationId, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isOrganizerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            item(key = "details") {
                FitCard(glow = FitTheme.glow.none) {
                    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FoundationText(
                                text = if (draft.routineId == null) "Create routine" else "Edit routine",
                                modifier = Modifier.weight(1f),
                                style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                            )
                            FoundationTextAction("Organize", onClick = { isOrganizerOpen = true })
                        }
                        FitTextField(
                            value = draft.name,
                            onValueChange = onNameChange,
                            placeholder = "Routine name",
                            selectAllOnFocus = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            items(draft.exercises, key = { exercise -> exercise.draftId.value }) { exercise ->
                RoutineExerciseCard(
                    exercise = exercise,
                    groupLabel = draft.exercises.groupSummaryFor(exercise),
                    onRemoveExercise = { onRemoveExercise(exercise.draftId) },
                    onAddSet = { onAddSet(exercise.draftId) },
                    onRemoveSet = { setId -> onRemoveSet(exercise.draftId, setId) },
                    onSetKindChange = { setId, kind -> onSetKindChange(exercise.draftId, setId, kind) },
                    onSetRepsChange = { setId, reps -> onSetRepsChange(exercise.draftId, setId, reps) },
                    onSetWeightChange = { setId, weight -> onSetWeightChange(exercise.draftId, setId, weight) },
                    onSetDurationChange = { setId, durationMs -> onSetDurationChange(exercise.draftId, setId, durationMs) },
                    onAdjustRest = { delta -> onAdjustRest(exercise.draftId, delta) },
                    onToggleRest = { onToggleRest(exercise.draftId) }
                )
            }
        }

        RoutineExerciseSearch(
            draft = draft,
            onExerciseQueryChange = onExerciseQueryChange,
            onAddExercise = onAddExercise
        )

        draft.errorMessage?.let { message ->
            FoundationText(
                text = message,
                style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FitButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                style = FitButtonStyle.Secondary
            )
            FitButton(
                text = if (draft.isSaving) "Saving" else "Save",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = draft.canSave,
                style = FitButtonStyle.Primary
            )
        }
    }

    if (isOrganizerOpen) {
        CircuitOrganizerDialog(
            exercises = draft.exercises,
            onCreateCircuit = onGroupSelected,
            onUngroup = onUngroup,
            onAdjustGroupRounds = onAdjustGroupRounds,
            onDismiss = { isOrganizerOpen = false }
        )
    }
}

@Composable
private fun RoutineExerciseCard(
    exercise: RoutineExerciseDraft,
    groupLabel: String?,
    onRemoveExercise: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (FoundationId) -> Unit,
    onSetKindChange: (FoundationId, SetKind) -> Unit,
    onSetRepsChange: (FoundationId, Int?) -> Unit,
    onSetWeightChange: (FoundationId, WeightKg?) -> Unit,
    onSetDurationChange: (FoundationId, Long?) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onToggleRest: () -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FoundationText(exercise.displayName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                    FoundationMutedText(listOfNotNull(groupLabel, exerciseKindLabel(exercise.loggingMode)).joinToString(" • "))
                }
                FoundationTextAction("Remove", onRemoveExercise)
            }
            RestControls(
                exercise = exercise,
                onSetRest = { selectedSeconds -> onAdjustRest(selectedSeconds - exercise.rest.durationSeconds) },
                onToggleRest = onToggleRest
            )
            exercise.plannedSets.forEach { set ->
                RoutineSetRow(
                    exercise = exercise,
                    set = set,
                    onRemove = { onRemoveSet(set.draftId) },
                    onSetKindChange = { onSetKindChange(set.draftId, it) },
                    onRepsChange = { onSetRepsChange(set.draftId, it) },
                    onWeightChange = { onSetWeightChange(set.draftId, it) },
                    onDurationChange = { onSetDurationChange(set.draftId, it) }
                )
            }
            FitButton(
                text = "Add set",
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Secondary
            )
        }
    }
}

@Composable
private fun CircuitOrganizerDialog(
    exercises: List<RoutineExerciseDraft>,
    onCreateCircuit: (List<FoundationId>) -> Unit,
    onUngroup: (FoundationId) -> Unit,
    onAdjustGroupRounds: (FoundationId, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember(exercises.map { it.draftId }) { mutableStateOf<Set<FoundationId>>(emptySet()) }
    val selectedExercises = exercises.filter { it.draftId in selectedIds }
    val canCreateCircuit = selectedExercises.size >= 2 && exercises.isAdjacentSelection(selectedIds)
    val selectedGroupedExercise = selectedExercises.firstOrNull { it.groupId != null }

    FitDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                FoundationText(
                    text = "Organize routine",
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
                FoundationMutedText("Select adjacent exercises to create one circuit.")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                items(exercises, key = { it.draftId.value }) { exercise ->
                    val isSelected = exercise.draftId in selectedIds
                    FitListRow(onClick = {
                        selectedIds = if (isSelected) {
                            selectedIds - exercise.draftId
                        } else {
                            selectedIds + exercise.draftId
                        }
                    }) {
                        Column(modifier = Modifier.weight(1f)) {
                            FoundationText(
                                text = exercise.displayName,
                                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                            )
                            FoundationMutedText(
                                listOfNotNull(
                                    exercises.groupSummaryFor(exercise),
                                    exerciseKindLabel(exercise.loggingMode)
                                ).joinToString(" • ")
                            )
                        }
                        FoundationMutedText(if (isSelected) "Selected" else "Select")
                    }
                }
            }

            selectedGroupedExercise?.let { exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FoundationMutedText(exercises.groupSummaryFor(exercise).orEmpty(), modifier = Modifier.weight(1f))
                    FitButton(text = "- round", onClick = { onAdjustGroupRounds(exercise.draftId, -1) }, style = FitButtonStyle.Secondary)
                    FitButton(text = "+ round", onClick = { onAdjustGroupRounds(exercise.draftId, 1) }, style = FitButtonStyle.Secondary)
                }
            }

            if (selectedExercises.size >= 2 && !canCreateCircuit) {
                FoundationText(
                    text = "Select adjacent exercises to create a circuit.",
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FitButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
                selectedGroupedExercise?.let { exercise ->
                    FitButton(
                        text = "Ungroup",
                        onClick = {
                            onUngroup(exercise.draftId)
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                }
                FitButton(
                    text = "Create circuit",
                    onClick = {
                        onCreateCircuit(exercises.filter { it.draftId in selectedIds }.map { it.draftId })
                        selectedIds = emptySet()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canCreateCircuit,
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

private fun List<RoutineExerciseDraft>.isAdjacentSelection(selectedIds: Set<FoundationId>): Boolean {
    if (selectedIds.size < 2) return false
    val positions = mapIndexedNotNull { index, exercise ->
        index.takeIf { exercise.draftId in selectedIds }
    }
    return positions == (positions.first()..positions.last()).toList()
}

@Composable
private fun RestControls(
    exercise: RoutineExerciseDraft,
    onSetRest: (Int) -> Unit,
    onToggleRest: () -> Unit
) {
    var isRestDialogOpen by remember(exercise.draftId) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoundationMutedText("Rest")
            FoundationText(
                text = if (exercise.rest.isEnabled) formatRestDurationSeconds(exercise.rest.durationSeconds) else "Off",
                modifier = Modifier.weight(1f),
                style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
            )
            if (exercise.rest.isEnabled) {
                FoundationTextAction("Change", onClick = { isRestDialogOpen = true })
                FoundationTextAction("Off", onToggleRest)
            } else {
                FoundationTextAction("On", onToggleRest)
            }
        }
    }

    if (isRestDialogOpen) {
        RoutineRestDialog(
            initialSeconds = exercise.rest.durationSeconds,
            onSave = { selectedSeconds ->
                onSetRest(selectedSeconds)
                isRestDialogOpen = false
            },
            onDismiss = { isRestDialogOpen = false }
        )
    }
}

@Composable
private fun RoutineRestDialog(
    initialSeconds: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSeconds by remember(initialSeconds) { mutableStateOf(initialSeconds) }

    FitDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            FoundationText(
                text = "Rest duration",
                style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText(formatRestDurationSeconds(selectedSeconds))
            RestDurationRoller(
                seconds = selectedSeconds,
                onSecondsChange = { selectedSeconds = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
            ) {
                FitButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Save",
                    onClick = { onSave(selectedSeconds) },
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

@Composable
private fun RoutineSetRow(
    exercise: RoutineExerciseDraft,
    set: RoutineSetDraft,
    onRemove: () -> Unit,
    onSetKindChange: (SetKind) -> Unit,
    onRepsChange: (Int?) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onDurationChange: (Long?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoundationMutedText("Set ${set.position.value + 1}", modifier = Modifier.weight(1f))
            FoundationTextAction("Remove", onRemove)
        }
        if (exercise.loggingMode == ExerciseLoggingMode.WEIGHTED) {
            FitSegmentedControl(
                options = listOf("Weighted", "Bodyweight"),
                selectedIndex = if (set.setKind == SetKind.WEIGHTED) 0 else 1,
                onSelect = { onSetKindChange(if (it == 0) SetKind.WEIGHTED else SetKind.BODYWEIGHT) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
        ) {
            if (set.setKind == SetKind.TIMED) {
                FitButton(
                    text = "-15",
                    onClick = { onDurationChange(nextDurationMs(set.targetDurationMs, -15_000L)) },
                    style = FitButtonStyle.Secondary
                )
                FitTextField(
                    value = set.targetDurationMs?.let { formatDurationMs(it) }.orEmpty(),
                    onValueChange = { value -> onDurationChange(parseDurationInput(value)) },
                    placeholder = "Target time",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    selectAllOnFocus = true,
                    modifier = Modifier.weight(1f)
                )
                FitButton(
                    text = "+15",
                    onClick = { onDurationChange(nextDurationMs(set.targetDurationMs, 15_000L)) },
                    style = FitButtonStyle.Secondary
                )
            } else {
                FitTextField(
                    value = set.targetReps?.toString().orEmpty(),
                    onValueChange = { value -> onRepsChange(value.toIntOrNull()) },
                    placeholder = "Target reps",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    selectAllOnFocus = true,
                    modifier = Modifier.weight(1f)
                )
                if (set.setKind == SetKind.WEIGHTED) {
                    FitTextField(
                        value = set.targetWeight?.value?.trimmedString().orEmpty(),
                        onValueChange = { value -> onWeightChange(value.toDoubleOrNull()?.let(::WeightKg)) },
                        placeholder = "Target kg",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        selectAllOnFocus = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineExerciseSearch(
    draft: RoutineEditorDraft,
    onExerciseQueryChange: (String) -> Unit,
    onAddExercise: (ExercisePickerResultRow) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            FitTextField(
                value = draft.exerciseQuery,
                onValueChange = onExerciseQueryChange,
                placeholder = "Search exercises to add",
                modifier = Modifier.fillMaxWidth()
            )
            if (draft.exerciseResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 176.dp),
                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
                ) {
                    items(
                        items = draft.exerciseResults,
                        key = { row -> row.exerciseCatalogId.value }
                    ) { row ->
                        FitListRow(onClick = { onAddExercise(row) }) {
                            Column(modifier = Modifier.weight(1f)) {
                                FoundationText(row.displayName, style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface))
                                FoundationMutedText(exerciseMetadataLine(row.subtitle, row.loggingMode, " • "))
                            }
                            FitButton(text = "Add", onClick = { onAddExercise(row) }, style = FitButtonStyle.Secondary)
                        }
                    }
                }
            } else if (draft.exerciseQuery.isNotBlank()) {
                FoundationMutedText("No local matches")
            }
        }
    }
}

private fun Double.trimmedString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
