package com.jjswigut.oopsallprs.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
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
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.theme.FitTheme
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
    onGroupWithNext: (FoundationId) -> Unit,
    onUngroup: (FoundationId) -> Unit,
    onAdjustGroupRounds: (FoundationId, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        FoundationText(
                            text = if (draft.routineId == null) "Create routine" else "Edit routine",
                            style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                        )
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

            itemsIndexed(draft.exercises, key = { _, exercise -> exercise.draftId.value }) { index, exercise ->
                RoutineExerciseCard(
                    exercise = exercise,
                    groupLabel = draft.exercises.groupSummaryFor(exercise),
                    canGroupWithNext = draft.exercises.canGroupWithNext(index),
                    onRemoveExercise = { onRemoveExercise(exercise.draftId) },
                    onGroupWithNext = { onGroupWithNext(exercise.draftId) },
                    onUngroup = { onUngroup(exercise.draftId) },
                    onAdjustGroupRounds = { delta -> onAdjustGroupRounds(exercise.draftId, delta) },
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
}

@Composable
private fun RoutineExerciseCard(
    exercise: RoutineExerciseDraft,
    groupLabel: String?,
    canGroupWithNext: Boolean,
    onRemoveExercise: () -> Unit,
    onGroupWithNext: () -> Unit,
    onUngroup: () -> Unit,
    onAdjustGroupRounds: (Int) -> Unit,
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
            RoutineGroupControls(
                groupLabel = groupLabel,
                canGroupWithNext = canGroupWithNext,
                onGroupWithNext = onGroupWithNext,
                onUngroup = onUngroup,
                onAdjustGroupRounds = onAdjustGroupRounds
            )
            RestControls(exercise, onAdjustRest, onToggleRest)
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
private fun RoutineGroupControls(
    groupLabel: String?,
    canGroupWithNext: Boolean,
    onGroupWithNext: () -> Unit,
    onUngroup: () -> Unit,
    onAdjustGroupRounds: (Int) -> Unit
) {
    if (!canGroupWithNext && groupLabel == null) return
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGroupWithNext) {
                FitButton(
                    text = if (groupLabel == null) "Superset" else "Circuit",
                    onClick = onGroupWithNext,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
            }
            if (groupLabel != null) {
                FitButton(
                    text = "Ungroup",
                    onClick = onUngroup,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
            }
        }
        if (groupLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoundationMutedText(groupLabel, modifier = Modifier.weight(1f))
                FitButton(text = "- round", onClick = { onAdjustGroupRounds(-1) }, style = FitButtonStyle.Secondary)
                FitButton(text = "+ round", onClick = { onAdjustGroupRounds(1) }, style = FitButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun RestControls(
    exercise: RoutineExerciseDraft,
    onAdjustRest: (Int) -> Unit,
    onToggleRest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FoundationMutedText("Rest")
            FoundationText(
                text = if (exercise.rest.isEnabled) formatRest(exercise.rest.durationSeconds) else "Off",
                style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
            )
        }
        FitButton(text = "-30", onClick = { onAdjustRest(-30) }, style = FitButtonStyle.Secondary)
        FitButton(text = "+30", onClick = { onAdjustRest(30) }, style = FitButtonStyle.Secondary)
        FoundationTextAction(if (exercise.rest.isEnabled) "Off" else "On", onToggleRest)
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
                    placeholder = "Last time",
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
                    placeholder = "Last reps",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    selectAllOnFocus = true,
                    modifier = Modifier.weight(1f)
                )
                if (set.setKind == SetKind.WEIGHTED) {
                    FitTextField(
                        value = set.targetWeight?.value?.trimmedString().orEmpty(),
                        onValueChange = { value -> onWeightChange(value.toDoubleOrNull()?.let(::WeightKg)) },
                        placeholder = "Last kg",
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
                        FitListRow {
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

private fun formatRest(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0) "${minutes}m" else "$minutes:${remainder.toString().padStart(2, '0')}"
}

private fun Double.trimmedString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun List<RoutineExerciseDraft>.canGroupWithNext(index: Int): Boolean {
    if (index !in indices || index == lastIndex) return false
    val currentGroupId = this[index].groupId
    val nextGroupId = this[index + 1].groupId
    return currentGroupId == null || currentGroupId != nextGroupId
}
