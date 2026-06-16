package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.common.RestDurationRoller
import com.jjswigut.oopsallprs.ui.common.formatRestDurationSeconds
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutFlow(
    state: ActiveWorkoutState,
    onAddExercise: () -> Unit,
    onLogSet: (FoundationId) -> Unit,
    onDraftRepsChange: (FoundationId, Int) -> Unit,
    onDraftWeightChange: (FoundationId, WeightKg?) -> Unit,
    onDraftDurationChange: (FoundationId, Long?) -> Unit,
    onDraftTimerToggle: (FoundationId) -> Unit,
    onBeginEditSet: (FoundationId) -> Unit,
    onEditRepsChange: (Int) -> Unit,
    onEditWeightChange: (WeightKg?) -> Unit,
    onEditDurationChange: (Long?) -> Unit,
    onSaveEditedSet: () -> Unit,
    onCancelEditSet: () -> Unit,
    onDeleteSet: (FoundationId) -> Unit,
    onUndoLastSet: () -> Unit,
    onRequestDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onRequestFinish: () -> Unit,
    onCancelFinish: () -> Unit,
    onConfirmFinish: (FoundationId) -> Unit,
    onRestTick: () -> Unit,
    onTimedTick: () -> Unit,
    onAdjustActiveRest: (Int) -> Unit,
    onSkipActiveRest: () -> Unit,
    onAdjustExerciseRest: (FoundationId, Int) -> Unit,
    onToggleExerciseRest: (FoundationId) -> Unit,
    onFocusExercise: (FoundationId) -> Unit,
    onDismiss: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    weightStepAmount: Double = weightStep(weightUnit),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.workout?.workoutId, state.workout?.activeRest?.endsAt) {
        if (state.workout?.activeRest == null) return@LaunchedEffect
        while (true) {
            delay(1_000)
            onRestTick()
        }
    }
    val hasRunningTimedDraft = state.workout?.exerciseBlocks.orEmpty().any { it.draft.isTimerRunning }
    LaunchedEffect(state.workout?.workoutId, hasRunningTimedDraft) {
        if (!hasRunningTimedDraft) return@LaunchedEffect
        while (true) {
            delay(1_000)
            onTimedTick()
        }
    }
    val displayWeightStep = weightStepAmount

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoundationMutedText(state.workout?.elapsedMillis?.let(::formatElapsed).orEmpty())
                FoundationTextAction("Close", onDismiss)
            }
            state.workout?.let { workout ->
                if (workout.exerciseBlocks.isEmpty()) {
                    FitCard(modifier = Modifier.fillMaxWidth(), glow = FitTheme.glow.none) {
                        FoundationText("Add an exercise")
                        FoundationMutedText("Start with a seeded or custom movement.")
                    }
                }
                workout.exerciseBlocks.forEach { block ->
                    ExerciseBlock(
                        block = block,
                        modifier = Modifier.fillMaxWidth(),
                        isFocused = workout.focus?.exerciseInstanceId == block.exerciseInstanceId,
                        weightUnit = weightUnit,
                        onFocus = { onFocusExercise(block.exerciseInstanceId) },
                        onEditSet = onBeginEditSet,
                        onDeleteSet = onDeleteSet
                    )
                }
            } ?: FitCard(modifier = Modifier.fillMaxWidth(), glow = FitTheme.glow.none) {
                FoundationText("No active workout loaded")
                FoundationMutedText("Return to Train and start or resume a workout.")
            }
            state.errorMessage?.let { message ->
                FoundationText(
                    text = message,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }

        state.workout?.let { workout ->
            ActiveWorkoutBottomBar(
                state = state,
                workout = workout,
                onAddExercise = onAddExercise,
                onLogSet = onLogSet,
                onDraftRepsChange = onDraftRepsChange,
                onDraftWeightChange = onDraftWeightChange,
                onDraftDurationChange = onDraftDurationChange,
                onDraftTimerToggle = onDraftTimerToggle,
                onEditRepsChange = onEditRepsChange,
                onEditWeightChange = onEditWeightChange,
                onEditDurationChange = onEditDurationChange,
                onSaveEditedSet = onSaveEditedSet,
                onCancelEditSet = onCancelEditSet,
                onUndoLastSet = onUndoLastSet,
                onRequestDiscard = onRequestDiscard,
                onCancelDiscard = onCancelDiscard,
                onConfirmDiscard = onConfirmDiscard,
                onRequestFinish = onRequestFinish,
                onCancelFinish = onCancelFinish,
                onConfirmFinish = onConfirmFinish,
                onAdjustActiveRest = onAdjustActiveRest,
                onSkipActiveRest = onSkipActiveRest,
                onAdjustExerciseRest = onAdjustExerciseRest,
                onToggleExerciseRest = onToggleExerciseRest,
                onFocusExercise = onFocusExercise,
                weightUnit = weightUnit,
                weightStepAmount = displayWeightStep,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ActiveWorkoutBottomBar(
    state: ActiveWorkoutState,
    workout: ActiveWorkoutView,
    onAddExercise: () -> Unit,
    onLogSet: (FoundationId) -> Unit,
    onDraftRepsChange: (FoundationId, Int) -> Unit,
    onDraftWeightChange: (FoundationId, WeightKg?) -> Unit,
    onDraftDurationChange: (FoundationId, Long?) -> Unit,
    onDraftTimerToggle: (FoundationId) -> Unit,
    onEditRepsChange: (Int) -> Unit,
    onEditWeightChange: (WeightKg?) -> Unit,
    onEditDurationChange: (Long?) -> Unit,
    onSaveEditedSet: () -> Unit,
    onCancelEditSet: () -> Unit,
    onUndoLastSet: () -> Unit,
    onRequestDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onRequestFinish: () -> Unit,
    onCancelFinish: () -> Unit,
    onConfirmFinish: (FoundationId) -> Unit,
    onAdjustActiveRest: (Int) -> Unit,
    onSkipActiveRest: () -> Unit,
    onAdjustExerciseRest: (FoundationId, Int) -> Unit,
    onToggleExerciseRest: (FoundationId) -> Unit,
    onFocusExercise: (FoundationId) -> Unit,
    weightUnit: WeightUnit,
    weightStepAmount: Double,
    modifier: Modifier = Modifier
) {
    val focusedBlock = workout.exerciseBlocks.firstOrNull {
        it.exerciseInstanceId == workout.focus?.exerciseInstanceId
    } ?: workout.exerciseBlocks.firstOrNull()
    var calculatorTarget by remember(workout.workoutId) { mutableStateOf<LoadCalculatorTarget?>(null) }
    var preferredBarWeights by remember(workout.workoutId) { mutableStateOf<Map<WeightUnit, Double>>(emptyMap()) }

    calculatorTarget?.let { target ->
        LoadCalculatorDialog(
            kind = target.kind,
            exerciseName = target.exerciseName,
            weightUnit = weightUnit,
            initialBarWeight = preferredBarWeights[weightUnit] ?: defaultBarbellState(weightUnit).barWeight,
            onBarWeightSelected = { selected ->
                preferredBarWeights = preferredBarWeights + (weightUnit to selected)
            },
            onApply = { weight ->
                onFocusExercise(target.exerciseInstanceId)
                onDraftWeightChange(target.exerciseInstanceId, weight)
                calculatorTarget = null
            },
            onDismiss = { calculatorTarget = null }
        )
    }

    FitCard(
        modifier = modifier,
        glow = FitTheme.glow.none
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            workout.activeRest?.let { rest ->
                RestTimerPanel(
                    rest = rest,
                    onAddTime = { onAdjustActiveRest(15) },
                    onSubtractTime = { onAdjustActiveRest(-15) },
                    onSkip = onSkipActiveRest
                )
            }

            if (state.isDiscardConfirmationVisible) {
                FoundationText("Discard workout?", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText("This removes the active workout from this device.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FitButton(
                        text = "Cancel",
                        onClick = onCancelDiscard,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                    FitButton(
                        text = "Discard",
                        onClick = onConfirmDiscard,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Primary
                    )
                }
                return@Column
            }

            if (state.isFinishConfirmationVisible) {
                FoundationText("Finish workout?", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText("This saves the workout to History and closes the active session.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FitButton(
                        text = "Cancel",
                        onClick = onCancelFinish,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                    FitButton(
                        text = "Finish",
                        onClick = { onConfirmFinish(workout.workoutId) },
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Primary
                    )
                }
                return@Column
            }

            state.editDraft?.let { edit ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                ) {
                    FoundationMutedText("Editing")
                    FoundationText(
                        text = edit.exerciseName,
                        style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                    )
                }
                SetRow(
                    draft = edit.rowDraft,
                    onRepsChange = onEditRepsChange,
                    onWeightChange = onEditWeightChange,
                    onDurationChange = onEditDurationChange,
                    onLog = onSaveEditedSet,
                    weightUnit = weightUnit,
                    weightStepAmount = weightStepAmount,
                    actionLabel = "Save changes",
                    pendingLabel = "Saving..."
                )
                FitButton(
                    text = "Cancel edit",
                    onClick = onCancelEditSet,
                    modifier = Modifier.fillMaxWidth(),
                    style = FitButtonStyle.Secondary
                )
                return@Column
            }

            if (workout.exerciseBlocks.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FitButton(
                        text = "Add exercise",
                        onClick = onAddExercise,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Primary
                    )
                    FitButton(
                        text = "Finish",
                        onClick = onRequestFinish,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                }
                FitButton(
                    text = "Discard workout",
                    onClick = onRequestDiscard,
                    modifier = Modifier.fillMaxWidth(),
                    style = FitButtonStyle.Secondary
                )
                return@Column
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                FoundationMutedText(focusedBlock?.loggingContextLabel() ?: "Logging")
                FoundationText(
                    text = focusedBlock?.displayName.orEmpty(),
                    style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                )
            }
            focusedBlock?.let { block ->
                ExerciseRestControls(
                    block = block,
                    onSetRest = { selectedSeconds ->
                        onAdjustExerciseRest(block.exerciseInstanceId, selectedSeconds - block.rest.durationSeconds)
                    },
                    onToggle = { onToggleExerciseRest(block.exerciseInstanceId) }
                )
                SetRow(
                    draft = block.draft,
                    onRepsChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftRepsChange(block.exerciseInstanceId, it)
                    },
                    onWeightChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftWeightChange(block.exerciseInstanceId, it)
                    },
                    onDurationChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftDurationChange(block.exerciseInstanceId, it)
                    },
                    onTimerToggle = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftTimerToggle(block.exerciseInstanceId)
                    },
                    onLog = {
                        onFocusExercise(block.exerciseInstanceId)
                        onLogSet(block.exerciseInstanceId)
                    },
                    weightStepAmount = weightStepAmount,
                    weightUnit = weightUnit,
                    loadCalculatorKind = block.loadCalculatorKind,
                    onOpenLoadCalculator = block.loadCalculatorKind?.let { kind ->
                        {
                            onFocusExercise(block.exerciseInstanceId)
                            calculatorTarget = LoadCalculatorTarget(block.exerciseInstanceId, block.displayName, kind)
                        }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.canUndoLastSet) {
                    FitButton(
                        text = "Undo set",
                        onClick = onUndoLastSet,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                }
                FitButton(
                    text = "Discard",
                    onClick = onRequestDiscard,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FitButton(
                    text = "Add exercise",
                    onClick = onAddExercise,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Finish",
                    onClick = onRequestFinish,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

private data class LoadCalculatorTarget(
    val exerciseInstanceId: FoundationId,
    val exerciseName: String,
    val kind: LoadCalculatorKind
)

@Composable
private fun RestTimerPanel(
    rest: ActiveRestTimerView,
    onAddTime: () -> Unit,
    onSubtractTime: () -> Unit,
    onSkip: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FoundationMutedText("Rest")
                FoundationText(
                    text = if (rest.isComplete) "Done" else formatRest(rest.remainingMillis),
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
            }
            FitButton(text = "-15", onClick = onSubtractTime, style = FitButtonStyle.Secondary)
            FitButton(text = "+15", onClick = onAddTime, style = FitButtonStyle.Secondary)
            FoundationTextAction("Skip", onSkip)
        }
    }
}

@Composable
private fun ExerciseRestControls(
    block: ExerciseBlockState,
    onSetRest: (Int) -> Unit,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoundationMutedText("Auto rest")
            FoundationText(
                text = if (block.rest.isEnabled) formatRestDurationSeconds(block.rest.durationSeconds) else "Off",
                modifier = Modifier.weight(1f),
                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
            )
            FoundationTextAction(if (block.rest.isEnabled) "Off" else "On", onToggle)
        }
        if (block.rest.isEnabled) {
            RestDurationRoller(
                seconds = block.rest.durationSeconds,
                onSecondsChange = onSetRest
            )
        }
    }
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalMinutes = elapsedMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatRest(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun ExerciseBlockState.loggingContextLabel(): String {
    val label = groupLabel ?: return "Logging"
    val rounds = groupRounds ?: return label
    return "$label · round ${draft.position.value + 1} of $rounds"
}
