package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitDialog
import com.jjswigut.oopsallprs.ds.component.FitIconButton
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
import com.jjswigut.oopsallprs.ds.component.FitToggle
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutFlow(
    state: ActiveWorkoutState,
    onAddExercise: () -> Unit,
    onLogSet: (FoundationId) -> Unit,
    onDraftRepsChange: (FoundationId, Int?) -> Unit,
    onDraftWeightChange: (FoundationId, WeightKg?) -> Unit,
    onDraftWeightInputChange: (FoundationId, MeasureInputUpdate<WeightKg>) -> Unit,
    onDraftDurationChange: (FoundationId, Long?) -> Unit,
    onDraftDistanceChange: (FoundationId, Double?) -> Unit,
    onDraftDistanceInputChange: (FoundationId, MeasureInputUpdate<Double>) -> Unit,
    onDraftEffortChange: (FoundationId, MeasureInputUpdate<Effort>) -> Unit,
    onDraftTimerToggle: (FoundationId) -> Unit,
    onBeginEditSet: (FoundationId) -> Unit,
    onEditRepsChange: (Int?) -> Unit,
    onEditWeightChange: (WeightKg?) -> Unit,
    onEditWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit,
    onEditDurationChange: (Long?) -> Unit,
    onEditDistanceChange: (Double?) -> Unit,
    onEditDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit,
    onEditEffortChange: (MeasureInputUpdate<Effort>) -> Unit,
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
    onTrackAddedWeightChange: (FoundationId, Boolean) -> Unit,
    onTrackEffortChange: (FoundationId, Boolean) -> Unit,
    onEffortKindChange: (FoundationId, EffortKind) -> Unit,
    onSaveConfigurationAsDefault: (FoundationId) -> Unit,
    onGroupCircuit: (List<FoundationId>) -> Unit,
    onUngroupCircuit: (FoundationId) -> Unit,
    onAdjustCircuitRounds: (FoundationId, Int) -> Unit,
    onFocusExercise: (FoundationId) -> Unit,
    onShowExerciseOverview: () -> Unit,
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
    var isOrganizerOpen by remember(state.workout?.workoutId) { mutableStateOf(false) }
    var settingsExerciseId by remember(state.workout?.workoutId) { mutableStateOf<FoundationId?>(null) }

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
                Row(horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
                    state.workout?.takeIf { it.exerciseBlocks.size >= 2 }?.let {
                        FoundationTextAction("Organize", onClick = { isOrganizerOpen = true })
                    }
                    FoundationTextAction(
                        "Close",
                        if (state.isExerciseOverviewVisible || state.workout?.exerciseBlocks.isNullOrEmpty()) {
                            onDismiss
                        } else {
                            onShowExerciseOverview
                        }
                    )
                }
            }
            state.workout?.let { workout ->
                if (workout.exerciseBlocks.isEmpty()) {
                    FitCard(modifier = Modifier.fillMaxWidth(), glow = FitTheme.glow.none) {
                        FoundationText("Add an exercise")
                        FoundationMutedText("Start with a seeded or custom movement.")
                    }
                }
                workout.exerciseBlockGroups().forEach { group ->
                    if (group.isGrouped) {
                        ExerciseBlockGroupCard(
                            group = group,
                            modifier = Modifier.fillMaxWidth(),
                            focusedExerciseId = workout.focus?.exerciseInstanceId
                                ?.takeUnless { state.isExerciseOverviewVisible },
                            weightUnit = weightUnit,
                            onFocus = onFocusExercise,
                            onSettings = { settingsExerciseId = it },
                            onEditSet = onBeginEditSet,
                            onDeleteSet = onDeleteSet
                        )
                    } else {
                        group.blocks.firstOrNull()?.let { block ->
                            ExerciseBlock(
                                block = block,
                                modifier = Modifier.fillMaxWidth(),
                                isFocused = !state.isExerciseOverviewVisible &&
                                    workout.focus?.exerciseInstanceId == block.exerciseInstanceId,
                                weightUnit = weightUnit,
                                onFocus = { onFocusExercise(block.exerciseInstanceId) },
                                onSettings = { settingsExerciseId = it },
                                onEditSet = onBeginEditSet,
                                onDeleteSet = onDeleteSet
                            )
                        }
                    }
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
                onDraftWeightInputChange = onDraftWeightInputChange,
                onDraftDurationChange = onDraftDurationChange,
                onDraftDistanceChange = onDraftDistanceChange,
                onDraftDistanceInputChange = onDraftDistanceInputChange,
                onDraftEffortChange = onDraftEffortChange,
                onDraftTimerToggle = onDraftTimerToggle,
                onEditRepsChange = onEditRepsChange,
                onEditWeightChange = onEditWeightChange,
                onEditWeightInputChange = onEditWeightInputChange,
                onEditDurationChange = onEditDurationChange,
                onEditDistanceChange = onEditDistanceChange,
                onEditDistanceInputChange = onEditDistanceInputChange,
                onEditEffortChange = onEditEffortChange,
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
                onFocusExercise = onFocusExercise,
                showExerciseOverview = state.isExerciseOverviewVisible,
                weightUnit = weightUnit,
                weightStepAmount = displayWeightStep,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    state.workout?.takeIf { isOrganizerOpen }?.let { workout ->
        ActiveCircuitOrganizerDialog(
            blocks = workout.exerciseBlocks,
            onCreateCircuit = onGroupCircuit,
            onUngroup = onUngroupCircuit,
            onAdjustCircuitRounds = onAdjustCircuitRounds,
            onDismiss = { isOrganizerOpen = false }
        )
    }

    state.workout?.exerciseBlocks
        ?.firstOrNull { it.exerciseInstanceId == settingsExerciseId }
        ?.let { block ->
            ExerciseSettingsDialog(
                block = block,
                onDecreaseRest = { onAdjustExerciseRest(block.exerciseInstanceId, -30) },
                onIncreaseRest = { onAdjustExerciseRest(block.exerciseInstanceId, 30) },
                onToggleRest = { onToggleExerciseRest(block.exerciseInstanceId) },
                onTrackAddedWeightChange = { onTrackAddedWeightChange(block.exerciseInstanceId, it) },
                onTrackEffortChange = { onTrackEffortChange(block.exerciseInstanceId, it) },
                onEffortKindChange = { onEffortKindChange(block.exerciseInstanceId, it) },
                onSaveAsDefault = { onSaveConfigurationAsDefault(block.exerciseInstanceId) },
                onDismiss = { settingsExerciseId = null }
            )
        }
}

@Composable
private fun ExerciseSettingsDialog(
    block: ExerciseBlockState,
    onDecreaseRest: () -> Unit,
    onIncreaseRest: () -> Unit,
    onToggleRest: () -> Unit,
    onTrackAddedWeightChange: (Boolean) -> Unit,
    onTrackEffortChange: (Boolean) -> Unit,
    onEffortKindChange: (EffortKind) -> Unit,
    onSaveAsDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    FitDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                FoundationText(
                    text = block.displayName,
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
                FoundationMutedText("Exercise settings")
            }

            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                FoundationMutedText("Auto rest")
                FoundationText(
                    text = if (block.rest.isEnabled) formatRest(block.rest.durationSeconds * 1_000L) else "Off",
                    style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FitButton(text = "-30", onClick = onDecreaseRest, modifier = Modifier.weight(1f), style = FitButtonStyle.Secondary)
                FitButton(text = "+30", onClick = onIncreaseRest, modifier = Modifier.weight(1f), style = FitButtonStyle.Secondary)
                FitButton(
                    text = if (block.rest.isEnabled) "Turn off" else "Turn on",
                    onClick = onToggleRest,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
            }

            if (block.isBodyweight) {
                SettingToggleRow(
                    label = "Track added weight",
                    helper = "Show an optional added-weight field for this workout.",
                    checked = block.tracksAddedWeight,
                    onCheckedChange = onTrackAddedWeightChange
                )
            }

            SettingToggleRow(
                label = "Track effort",
                helper = null,
                checked = block.effortKind != null,
                onCheckedChange = onTrackEffortChange
            )
            block.effortKind?.let { selected ->
                val kinds = listOf(EffortKind.RIR, EffortKind.RPE, EffortKind.FAILURE_OUTCOME)
                FitSegmentedControl(
                    options = listOf("RIR", "RPE", "Failure"),
                    selectedIndex = kinds.indexOf(selected).coerceAtLeast(0),
                    onSelect = { onEffortKindChange(kinds[it]) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (block.canSaveConfigurationAsDefault) {
                Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                    FitButton(
                        text = "Save as default",
                        onClick = onSaveAsDefault,
                        modifier = Modifier.fillMaxWidth(),
                        style = FitButtonStyle.Secondary
                    )
                    FoundationMutedText("Use this setting for future workouts. Existing routines won't change.")
                }
            }

            FitButton(
                text = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Primary
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    helper: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
            FoundationText(label, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
            helper?.let { FoundationMutedText(it) }
        }
        FitToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = label
        )
    }
}

@Composable
private fun ActiveCircuitOrganizerDialog(
    blocks: List<ExerciseBlockState>,
    onCreateCircuit: (List<FoundationId>) -> Unit,
    onUngroup: (FoundationId) -> Unit,
    onAdjustCircuitRounds: (FoundationId, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember(blocks.map { it.exerciseInstanceId }) { mutableStateOf<Set<FoundationId>>(emptySet()) }
    val selectedBlocks = blocks.filter { it.exerciseInstanceId in selectedIds }
    val canCreateCircuit = selectedBlocks.size >= 2 && blocks.isAdjacentSelection(selectedIds)
    val selectedGroupedBlock = selectedBlocks.firstOrNull { it.groupId != null }

    FitDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                FoundationText(
                    text = "Organize workout",
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
                FoundationMutedText("Select adjacent exercises to create a circuit while you log.")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                items(blocks, key = { it.exerciseInstanceId.value }) { block ->
                    val isSelected = block.exerciseInstanceId in selectedIds
                    OrganizerExerciseRow(
                        block = block,
                        isSelected = isSelected,
                        onClick = {
                            selectedIds = if (isSelected) {
                                selectedIds - block.exerciseInstanceId
                            } else {
                                selectedIds + block.exerciseInstanceId
                            }
                        }
                    )
                }
            }

            selectedGroupedBlock?.let { block ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FoundationMutedText(block.groupSummary().orEmpty(), modifier = Modifier.weight(1f))
                    OrganizerSymbolButton(
                        symbol = "-",
                        contentDescription = "Decrease rounds",
                        onClick = { onAdjustCircuitRounds(block.exerciseInstanceId, -1) }
                    )
                    OrganizerSymbolButton(
                        symbol = "+",
                        contentDescription = "Increase rounds",
                        onClick = { onAdjustCircuitRounds(block.exerciseInstanceId, 1) }
                    )
                }
            }

            if (selectedBlocks.size >= 2 && !canCreateCircuit) {
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
                selectedGroupedBlock?.let { block ->
                    FitIconButton(
                        onClick = {
                            onUngroup(block.exerciseInstanceId)
                            selectedIds = emptySet()
                        },
                        contentDescription = "Ungroup"
                    ) {
                        BrokenLinkIcon()
                    }
                }
                FitButton(
                    text = "Create circuit",
                    onClick = {
                        onCreateCircuit(blocks.filter { it.exerciseInstanceId in selectedIds }.map { it.exerciseInstanceId })
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

@Composable
private fun OrganizerExerciseRow(
    block: ExerciseBlockState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedModifier = if (isSelected) {
        Modifier.background(
            color = FitTheme.colors.accent.copy(alpha = 0.14f),
            shape = FitTheme.shapes.mediumShape
        )
    } else {
        Modifier
    }
    val titleColor = if (isSelected) FitTheme.colors.accent else FitTheme.colors.onSurface

    FitListRow(
        modifier = selectedModifier,
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FoundationText(
                text = block.displayName,
                style = FitTheme.type.label.copy(color = titleColor)
            )
            FoundationMutedText(
                listOfNotNull(
                    block.groupSummary(),
                    when (block.loggingMode) {
                        com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode.TIMED -> "Timed"
                        com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode.BODYWEIGHT -> "Bodyweight"
                        com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode.WEIGHTED -> "Weighted"
                    }
                ).joinToString(" • ")
            )
        }
    }
}

@Composable
private fun OrganizerSymbolButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    FitIconButton(
        onClick = onClick,
        contentDescription = contentDescription
    ) {
        FoundationText(
            text = symbol,
            style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
        )
    }
}

@Composable
private fun BrokenLinkIcon(modifier: Modifier = Modifier) {
    val color = FitTheme.colors.onSurface
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val linkHeight = size.height * 0.38f
        val linkWidth = size.width * 0.42f
        val linkTop = (size.height - linkHeight) / 2f
        val corner = CornerRadius(linkHeight / 2f, linkHeight / 2f)

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, linkTop),
            size = Size(linkWidth, linkHeight),
            cornerRadius = corner,
            style = stroke
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - linkWidth, linkTop),
            size = Size(linkWidth, linkHeight),
            cornerRadius = corner,
            style = stroke
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.45f, size.height * 0.22f),
            end = Offset(size.width * 0.36f, size.height * 0.02f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.55f, size.height * 0.78f),
            end = Offset(size.width * 0.64f, size.height * 0.98f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun List<ExerciseBlockState>.isAdjacentSelection(selectedIds: Set<FoundationId>): Boolean {
    if (selectedIds.size < 2) return false
    val positions = mapIndexedNotNull { index, block ->
        index.takeIf { block.exerciseInstanceId in selectedIds }
    }
    return positions == (positions.first()..positions.last()).toList()
}

private fun ExerciseBlockState.groupSummary(): String? {
    val label = groupLabel ?: return null
    val rounds = groupRounds ?: return label
    return "$label x$rounds"
}

@Composable
private fun ActiveWorkoutBottomBar(
    state: ActiveWorkoutState,
    workout: ActiveWorkoutView,
    onAddExercise: () -> Unit,
    onLogSet: (FoundationId) -> Unit,
    onDraftRepsChange: (FoundationId, Int?) -> Unit,
    onDraftWeightChange: (FoundationId, WeightKg?) -> Unit,
    onDraftWeightInputChange: (FoundationId, MeasureInputUpdate<WeightKg>) -> Unit,
    onDraftDurationChange: (FoundationId, Long?) -> Unit,
    onDraftDistanceChange: (FoundationId, Double?) -> Unit,
    onDraftDistanceInputChange: (FoundationId, MeasureInputUpdate<Double>) -> Unit,
    onDraftEffortChange: (FoundationId, MeasureInputUpdate<Effort>) -> Unit,
    onDraftTimerToggle: (FoundationId) -> Unit,
    onEditRepsChange: (Int?) -> Unit,
    onEditWeightChange: (WeightKg?) -> Unit,
    onEditWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit,
    onEditDurationChange: (Long?) -> Unit,
    onEditDistanceChange: (Double?) -> Unit,
    onEditDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit,
    onEditEffortChange: (MeasureInputUpdate<Effort>) -> Unit,
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
    onFocusExercise: (FoundationId) -> Unit,
    showExerciseOverview: Boolean,
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
                        text = "Finish workout",
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
                    onWeightInputChange = onEditWeightInputChange,
                    onDurationChange = onEditDurationChange,
                    onDistanceChange = onEditDistanceChange,
                    onDistanceInputChange = onEditDistanceInputChange,
                    onEffortChange = onEditEffortChange,
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

            if (showExerciseOverview && workout.exerciseBlocks.isNotEmpty()) {
                FoundationText(
                    text = "Exercises",
                    style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                )
                FoundationMutedText("Choose an exercise to keep logging.")
                FitButton(
                    text = "Add exercise",
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                    style = FitButtonStyle.Primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FitButton(
                        text = "Discard workout",
                        onClick = onRequestDiscard,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                    FitButton(
                        text = "Finish workout",
                        onClick = onRequestFinish,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                }
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
                        text = "Finish workout",
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

            focusedBlock?.let { block ->
                LoggingContextHeader(block = block, workout = workout)
            }
            focusedBlock?.let { block ->
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
                    onWeightInputChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftWeightInputChange(block.exerciseInstanceId, it)
                    },
                    onDurationChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftDurationChange(block.exerciseInstanceId, it)
                    },
                    onDistanceChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftDistanceChange(block.exerciseInstanceId, it)
                    },
                    onDistanceInputChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftDistanceInputChange(block.exerciseInstanceId, it)
                    },
                    onEffortChange = {
                        onFocusExercise(block.exerciseInstanceId)
                        onDraftEffortChange(block.exerciseInstanceId, it)
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
                    style = FitButtonStyle.Primary
                )
                FitButton(
                    text = "Finish workout",
                    onClick = onRequestFinish,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
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

@Composable
private fun LoggingContextHeader(
    block: ExerciseBlockState,
    workout: ActiveWorkoutView
) {
    val circuitLabel = block.groupLabel
    if (circuitLabel == null || block.groupId == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            FoundationMutedText("Logging")
            FoundationText(
                text = block.displayName,
                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
            )
        }
        return
    }

    val round = block.draft.position.value + 1
    val rounds = block.groupRounds ?: 1
    val next = workout.nextCircuitBlockAfter(block)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        FoundationText(
            text = circuitLabel,
            style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
        )
        FoundationMutedText(
            listOfNotNull(
                "Round $round of $rounds",
                next?.let { "Next: ${it.displayName}" }
            ).joinToString(" • ")
        )
        FoundationText(
            text = block.displayName,
            style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
        )
    }
}

private fun ActiveWorkoutView.nextCircuitBlockAfter(block: ExerciseBlockState): ExerciseBlockState? {
    val groupId = block.groupId ?: return null
    val grouped = exerciseBlocks.filter { it.groupId == groupId }.sortedBy { it.position.value }
    val index = grouped.indexOfFirst { it.exerciseInstanceId == block.exerciseInstanceId }
    if (index == -1) return null
    return when {
        index < grouped.lastIndex -> grouped[index + 1]
        block.draft.position.value + 1 < (block.groupRounds ?: 1) -> grouped.firstOrNull()
        else -> null
    }
}

private fun ExerciseBlockState.loggingContextLabel(): String {
    val label = groupLabel ?: return "Logging"
    val rounds = groupRounds ?: return label
    return "$label · round ${draft.position.value + 1} of $rounds"
}
