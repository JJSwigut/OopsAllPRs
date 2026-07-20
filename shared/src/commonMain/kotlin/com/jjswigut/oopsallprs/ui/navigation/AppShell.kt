package com.jjswigut.oopsallprs.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import com.jjswigut.oopsallprs.AppState
import com.jjswigut.oopsallprs.dev.DeveloperSeedState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.platform.PlatformBackHandler
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitDialog
import com.jjswigut.oopsallprs.ds.component.FitTabBar
import com.jjswigut.oopsallprs.ds.component.FitTabItem
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.components.AppNavRail
import com.jjswigut.oopsallprs.ui.components.ResumeBanner
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.exercise.ExerciseManagementFlow
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerFlow
import com.jjswigut.oopsallprs.ui.history.HistoryFlow
import com.jjswigut.oopsallprs.ui.profile.ProfileFlow
import com.jjswigut.oopsallprs.ui.profile.openPrivacyPolicy
import com.jjswigut.oopsallprs.ui.progress.ProgressFlow
import com.jjswigut.oopsallprs.ui.routine.RoutineEditorFlow
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutFlow
import com.jjswigut.oopsallprs.ui.workout.WorkoutHomeFlow
import kotlinx.coroutines.launch

@Composable
fun AppShell(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val shellState by appState.navigation.state.collectAsState()
    val workoutHomeState by appState.workoutHome.state.collectAsState()
    val activeWorkoutState by appState.activeWorkout.state.collectAsState()
    val exercisePickerState by appState.exercisePicker.state.collectAsState()
    val exerciseManagementState by appState.exerciseManagement.state.collectAsState()
    val progressState by appState.progress.state.collectAsState()
    val historyState by appState.history.state.collectAsState()
    val routineState by appState.routines.state.collectAsState()
    val profileState by appState.profile.state.collectAsState()
    val developerSeedState = appState.developerSeeds?.state?.collectAsState()?.value
    var isDiscardDialogVisible by remember { mutableStateOf(false) }
    val confirmDiscardActiveWorkout: () -> Unit = {
        scope.launch {
            when (appState.workoutHome.discardActive()) {
                is FoundationResult.Failure -> Unit
                is FoundationResult.Success -> {
                    isDiscardDialogVisible = false
                    appState.hydrate()
                    appState.navigation.dismissActiveWorkout()
                }
            }
        }
    }

    PlatformBackHandler(
        enabled = exerciseManagementState.isOpen ||
            routineState.editorDraft != null ||
            exercisePickerState.isOpen ||
            shellState.isActiveWorkoutPresented ||
            historyState.selectedSummary != null
    ) {
        when {
            exerciseManagementState.isOpen -> appState.exerciseManagement.close()
            routineState.editorDraft != null -> appState.routines.cancelEditor()
            exercisePickerState.isOpen -> appState.exercisePicker.dismiss()
            shellState.isActiveWorkoutPresented -> {
                scope.launch { appState.navigation.dismissActiveWorkout() }
            }
            historyState.selectedSummary != null -> appState.history.clearSelection()
        }
    }

    LaunchedEffect(Unit) {
        appState.hydrate()
        scope.launch { appState.refreshFullAccessEntitlements() }
        scope.launch { appState.checkBackupSyncOnLaunchOrResume() }
    }

    LaunchedEffect(shellState.selectedDestination) {
        when (shellState.selectedDestination) {
            TopLevelDestination.HISTORY -> appState.history.refresh()
            TopLevelDestination.PROGRESS -> appState.progress.refresh()
            TopLevelDestination.TRAIN -> appState.workoutHome.hydrate()
            TopLevelDestination.PROFILE -> {
                appState.profile.hydrate()
                scope.launch { appState.refreshFullAccessEntitlements() }
                scope.launch { appState.checkBackupSyncOnLaunchOrResume() }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FitTheme.colors.background)
            .safeDrawingPadding()
    ) {
        LaunchedEffect(maxWidth) {
            appState.navigation.setLayoutClass(NavigationLayoutClass.fromWidthDp(maxWidth.value))
        }

        val contentPadding = FitTheme.spacing.lg
        if (shellState.layoutClass.usesBottomBar) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DestinationContent(
                        destination = shellState.selectedDestination,
                        appState = appState,
                        workoutHomeState = workoutHomeState,
                        progressState = progressState,
                        historyState = historyState,
                        routineState = routineState,
                        profileState = profileState,
                        developerSeedState = developerSeedState
                    )
                }
                shellState.activeWorkoutResume?.let { resume ->
                    ResumeBanner(
                        resume = resume,
                        onResume = { scope.launch { appState.navigation.presentActiveWorkout() } },
                        onDiscard = { isDiscardDialogVisible = true }
                    )
                }
                FitTabBar(
                    items = TopLevelDestination.ordered.map { destination ->
                        FitTabItem(destination.label) { tint -> TabGlyph(destination, tint) }
                    },
                    selectedIndex = TopLevelDestination.ordered.indexOf(shellState.selectedDestination),
                    onSelect = { index ->
                        scope.launch {
                            appState.navigation.selectDestination(TopLevelDestination.ordered[index])
                        }
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.lg)
            ) {
                AppNavRail(
                    destinations = TopLevelDestination.ordered,
                    selected = shellState.selectedDestination,
                    onSelect = { destination ->
                        scope.launch { appState.navigation.selectDestination(destination) }
                    }
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
                ) {
                    shellState.activeWorkoutResume?.let { resume ->
                        ResumeBanner(
                            resume = resume,
                            onResume = { scope.launch { appState.navigation.presentActiveWorkout() } },
                            onDiscard = { isDiscardDialogVisible = true }
                        )
                    }
                    DestinationContent(
                        destination = shellState.selectedDestination,
                        appState = appState,
                        workoutHomeState = workoutHomeState,
                        progressState = progressState,
                        historyState = historyState,
                        routineState = routineState,
                        profileState = profileState,
                        developerSeedState = developerSeedState
                    )
                }
            }
        }

        if (shellState.isActiveWorkoutPresented) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FitTheme.colors.background)
                    .padding(contentPadding)
            ) {
                ActiveWorkoutFlow(
                    state = activeWorkoutState,
                    onAddExercise = {
                        val workoutId = activeWorkoutState.workout?.workoutId ?: shellState.activeWorkoutResume?.activeWorkoutId
                        workoutId?.let { activeWorkoutId ->
                            scope.launch { appState.exercisePicker.open(activeWorkoutId) }
                        }
                    },
                    onLogSet = { exerciseId ->
                        scope.launch {
                            when (appState.activeWorkout.confirmDraft(exerciseId)) {
                                is FoundationResult.Failure -> Unit
                                is FoundationResult.Success -> appState.refreshActiveSession()
                            }
                        }
                    },
                    onDraftRepsChange = { exerciseId, reps ->
                        scope.launch { appState.activeWorkout.updateDraftReps(exerciseId, reps) }
                    },
                    onDraftWeightChange = { exerciseId, weight ->
                        scope.launch { appState.activeWorkout.updateDraftWeight(exerciseId, weight) }
                    },
                    onDraftWeightInputChange = { exerciseId, update ->
                        scope.launch { appState.activeWorkout.updateDraftWeightInput(exerciseId, update) }
                    },
                    onDraftDurationChange = { exerciseId, durationMs ->
                        scope.launch { appState.activeWorkout.updateDraftDuration(exerciseId, durationMs) }
                    },
                    onDraftDistanceChange = { exerciseId, distanceMeters ->
                        scope.launch { appState.activeWorkout.updateDraftDistance(exerciseId, distanceMeters) }
                    },
                    onDraftDistanceInputChange = { exerciseId, update ->
                        scope.launch { appState.activeWorkout.updateDraftDistanceInput(exerciseId, update) }
                    },
                    onDraftEffortChange = { exerciseId, update ->
                        scope.launch { appState.activeWorkout.updateDraftEffort(exerciseId, update) }
                    },
                    onDraftTimerToggle = { exerciseId ->
                        scope.launch { appState.activeWorkout.toggleDraftTimer(exerciseId) }
                    },
                    onBeginEditSet = { setId -> appState.activeWorkout.beginEditSet(setId) },
                    onEditRepsChange = { reps -> appState.activeWorkout.updateEditReps(reps) },
                    onEditWeightChange = { weight -> appState.activeWorkout.updateEditWeight(weight) },
                    onEditWeightInputChange = { update -> appState.activeWorkout.updateEditWeightInput(update) },
                    onEditDurationChange = { durationMs -> appState.activeWorkout.updateEditDuration(durationMs) },
                    onEditDistanceChange = { distanceMeters -> appState.activeWorkout.updateEditDistance(distanceMeters) },
                    onEditDistanceInputChange = { update -> appState.activeWorkout.updateEditDistanceInput(update) },
                    onEditEffortChange = { update -> appState.activeWorkout.updateEditEffort(update) },
                    onSaveEditedSet = {
                        scope.launch { appState.activeWorkout.confirmEditSet() }
                    },
                    onCancelEditSet = { appState.activeWorkout.cancelEditSet() },
                    onDeleteSet = { setId ->
                        scope.launch {
                            appState.activeWorkout.deleteLoggedSet(setId)
                            appState.refreshActiveSession()
                        }
                    },
                    onUndoLastSet = {
                        scope.launch {
                            appState.activeWorkout.undoLastLoggedSet()
                            appState.refreshActiveSession()
                        }
                    },
                    onRequestDiscard = { appState.activeWorkout.requestDiscard() },
                    onCancelDiscard = { appState.activeWorkout.cancelDiscard() },
                    onConfirmDiscard = {
                        scope.launch {
                            when (appState.activeWorkout.confirmDiscard()) {
                                is FoundationResult.Failure -> Unit
                                is FoundationResult.Success -> {
                                    appState.hydrate()
                                    appState.navigation.dismissActiveWorkout()
                                    appState.navigation.selectDestination(TopLevelDestination.TRAIN)
                                }
                            }
                        }
                    },
                    onRequestFinish = { appState.activeWorkout.requestFinish() },
                    onCancelFinish = { appState.activeWorkout.cancelFinish() },
                    onConfirmFinish = { workoutId ->
                        scope.launch {
                            when (val result = appState.routines.finishWorkout(workoutId)) {
                                is FoundationResult.Failure -> Unit
                                is FoundationResult.Success -> {
                                    appState.hydrate()
                                    appState.history.presentCompletedWorkout(result.value.id)
                                    appState.progress.refresh()
                                    appState.navigation.selectDestination(TopLevelDestination.HISTORY)
                                }
                            }
                        }
                    },
                    onTimerTick = {
                        scope.launch {
                            appState.activeWorkout.refreshTimers()
                            appState.refreshActiveSession()
                        }
                    },
                    onAdjustActiveRest = { deltaSeconds ->
                        scope.launch {
                            appState.activeWorkout.adjustActiveRest(deltaSeconds)
                            appState.refreshActiveSession()
                        }
                    },
                    onSkipActiveRest = {
                        scope.launch {
                            appState.activeWorkout.skipActiveRest()
                            appState.refreshActiveSession()
                        }
                    },
                    onAdjustExerciseRest = { exerciseId, deltaSeconds ->
                        scope.launch { appState.activeWorkout.adjustExerciseRest(exerciseId, deltaSeconds) }
                    },
                    onToggleExerciseRest = { exerciseId ->
                        scope.launch { appState.activeWorkout.toggleExerciseRest(exerciseId) }
                    },
                    onTrackAddedWeightChange = { exerciseId, enabled ->
                        scope.launch { appState.activeWorkout.setBodyweightAddedLoad(exerciseId, enabled) }
                    },
                    onTrackEffortChange = { exerciseId, enabled ->
                        scope.launch { appState.activeWorkout.setTrackEffort(exerciseId, enabled) }
                    },
                    onEffortKindChange = { exerciseId, effortKind ->
                        scope.launch { appState.activeWorkout.setEffortKind(exerciseId, effortKind) }
                    },
                    onSaveConfigurationAsDefault = { exerciseId ->
                        scope.launch { appState.activeWorkout.saveActiveConfigurationAsDefault(exerciseId) }
                    },
                    onGroupCircuit = { exerciseIds ->
                        scope.launch { appState.activeWorkout.groupExercisesAsCircuit(exerciseIds) }
                    },
                    onUngroupCircuit = { exerciseId ->
                        scope.launch { appState.activeWorkout.ungroupCircuit(exerciseId) }
                    },
                    onAdjustCircuitRounds = { exerciseId, delta ->
                        scope.launch { appState.activeWorkout.adjustCircuitRounds(exerciseId, delta) }
                    },
                    onFocusExercise = { exerciseId ->
                        scope.launch { appState.activeWorkout.setFocus(exerciseId) }
                    },
                    onShowExerciseOverview = { appState.activeWorkout.showExerciseOverview() },
                    onDismiss = { scope.launch { appState.navigation.dismissActiveWorkout() } },
                    weightUnit = profileState.weightUnit,
                    weightStepAmount = profileState.weightStep
                )
                if (exercisePickerState.isOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FitTheme.colors.background)
                            .padding(contentPadding)
                    ) {
                        ExercisePickerFlow(
                            state = exercisePickerState,
                            onQueryChange = { query ->
                                scope.launch { appState.exercisePicker.search(query) }
                            },
                            onSelect = { row ->
                                scope.launch { appState.exercisePicker.select(row) }
                            },
                            onShowCreate = { appState.exercisePicker.showCustomCreation() },
                            onCustomNameChange = { appState.exercisePicker.updateCustomName(it) },
                            onCustomLoggingModeChange = { appState.exercisePicker.updateCustomLoggingMode(it) },
                            onCreateExercise = {
                                scope.launch { appState.exercisePicker.createCustom() }
                            },
                            onDismiss = { appState.exercisePicker.dismiss() }
                        )
                    }
                }
            }
        }

        routineState.editorDraft?.let { draft ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FitTheme.colors.background)
                    .padding(contentPadding)
            ) {
                RoutineEditorFlow(
                    draft = draft,
                    onNameChange = { appState.routines.updateEditorName(it) },
                    onExerciseQueryChange = { query ->
                        scope.launch { appState.routines.searchEditorExercises(query) }
                    },
                    onAddExercise = { row -> appState.routines.addEditorExercise(row) },
                    onRemoveExercise = { id -> appState.routines.removeEditorExercise(id) },
                    onAddSet = { id -> appState.routines.addEditorSet(id) },
                    onRemoveSet = { exerciseId, setId -> appState.routines.removeEditorSet(exerciseId, setId) },
                    onSetKindChange = { exerciseId, setId, kind -> appState.routines.updateEditorSetKind(exerciseId, setId, kind) },
                    onSetRepsChange = { exerciseId, setId, reps -> appState.routines.updateEditorSetReps(exerciseId, setId, reps) },
                    onSetWeightChange = { exerciseId, setId, weight -> appState.routines.updateEditorSetWeight(exerciseId, setId, weight) },
                    onSetDurationChange = { exerciseId, setId, durationMs -> appState.routines.updateEditorSetDuration(exerciseId, setId, durationMs) },
                    onAdjustRest = { exerciseId, delta -> appState.routines.adjustEditorRest(exerciseId, delta) },
                    onToggleRest = { exerciseId -> appState.routines.toggleEditorRest(exerciseId) },
                    onGroupSelected = { exerciseIds -> appState.routines.groupEditorExercises(exerciseIds) },
                    onUngroup = { exerciseId -> appState.routines.ungroupEditorExercise(exerciseId) },
                    onAdjustGroupRounds = { exerciseId, delta -> appState.routines.adjustEditorGroupRounds(exerciseId, delta) },
                    onSave = {
                        scope.launch {
                            when (appState.routines.saveEditor()) {
                                is FoundationResult.Failure -> Unit
                                is FoundationResult.Success -> {
                                    appState.workoutHome.hydrate()
                                }
                            }
                        }
                    },
                    onCancel = { appState.routines.cancelEditor() }
                )
            }
        }

        if (exerciseManagementState.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FitTheme.colors.background)
                    .padding(contentPadding)
            ) {
                ExerciseManagementFlow(
                    state = exerciseManagementState,
                    onQueryChange = { query -> scope.launch { appState.exerciseManagement.search(query) } },
                    onBeginCreate = { appState.exerciseManagement.beginCreate() },
                    onBeginEdit = { row -> appState.exerciseManagement.beginEdit(row) },
                    onRequestArchive = { row -> appState.exerciseManagement.requestArchive(row) },
                    onDraftNameChange = { appState.exerciseManagement.updateDraftName(it) },
                    onDraftLoggingModeChange = { appState.exerciseManagement.updateDraftLoggingMode(it) },
                    onSaveDraft = {
                        scope.launch {
                            when (appState.exerciseManagement.saveDraft()) {
                                is FoundationResult.Failure -> Unit
                                is FoundationResult.Success -> {
                                    appState.exercisePicker.dismiss()
                                }
                            }
                        }
                    },
                    onCancelDraft = { appState.exerciseManagement.cancelDraft() },
                    onConfirmArchive = {
                        scope.launch { appState.exerciseManagement.confirmArchive() }
                    },
                    onCancelArchive = { appState.exerciseManagement.cancelArchive() },
                    onClose = { appState.exerciseManagement.close() }
                )
            }
        }

        if (isDiscardDialogVisible) {
            DiscardActiveWorkoutDialog(
                onCancel = { isDiscardDialogVisible = false },
                onConfirm = confirmDiscardActiveWorkout
            )
        }
    }
}

@Composable
private fun DiscardActiveWorkoutDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    FitDialog(onDismissRequest = onCancel) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            FoundationText(
                text = "Discard active workout?",
                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText("This removes the active workout from this device.")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                FitButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Discard",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: TopLevelDestination,
    appState: AppState,
    workoutHomeState: com.jjswigut.oopsallprs.ui.workout.WorkoutHomeState,
    progressState: com.jjswigut.oopsallprs.ui.progress.ProgressState,
    historyState: com.jjswigut.oopsallprs.ui.history.HistoryState,
    routineState: com.jjswigut.oopsallprs.ui.routine.RoutineState,
    profileState: com.jjswigut.oopsallprs.ui.profile.ProfileState,
    developerSeedState: DeveloperSeedState?
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    when (destination) {
        TopLevelDestination.TRAIN -> WorkoutHomeFlow(
            state = workoutHomeState,
            fullAccessStatus = profileState.fullAccessStatus,
            onStartEmpty = {
                scope.launch {
                    appState.workoutHome.startEmpty()
                    appState.hydrate()
                }
            },
            onStartRoutine = { templateId ->
                scope.launch {
                    when (appState.workoutHome.launchTemplate(templateId)) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> {
                            appState.hydrate()
                            appState.navigation.presentActiveWorkout()
                        }
                    }
                }
            },
            onPurchaseLifetimeUnlock = {
                scope.launch {
                    appState.profile.purchaseLifetimeUnlock()
                    appState.workoutHome.refreshFullAccess()
                }
            },
            onRestorePurchases = {
                scope.launch {
                    appState.profile.restorePurchases()
                    appState.workoutHome.refreshFullAccess()
                }
            },
            onCreateRoutine = {
                appState.routines.beginCreateRoutine()
            },
            onEditRoutine = { templateId ->
                scope.launch { appState.routines.beginEditRoutine(templateId) }
            },
            onRequestDeleteTemplate = { templateId ->
                appState.workoutHome.requestTemplateDelete(templateId)
            },
            onCancelDeleteTemplate = {
                appState.workoutHome.cancelTemplateDelete()
            },
            onConfirmDeleteTemplate = {
                scope.launch {
                    when (appState.workoutHome.confirmTemplateDelete()) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> appState.routines.refresh()
                    }
                }
            }
        )
        TopLevelDestination.HISTORY -> HistoryFlow(
            state = historyState,
            weightUnit = profileState.weightUnit,
            templateSaveDraft = routineState.saveDraft,
            onSelectWorkout = { workoutId ->
                scope.launch { appState.history.selectWorkout(workoutId) }
            },
            onBack = { appState.history.clearSelection() },
            onStartSaveTemplate = { completedWorkoutId ->
                appState.routines.beginTemplateSave(completedWorkoutId)
            },
            onTemplateNameChange = { appState.routines.updateTemplateName(it) },
            onSaveTemplate = {
                scope.launch {
                    appState.routines.saveTemplate()
                    appState.workoutHome.hydrate()
                }
            },
            onRequestDeleteWorkout = { appState.history.requestDeleteSelectedWorkout() },
            onCancelDeleteWorkout = { appState.history.cancelDeleteWorkout() },
            onConfirmDeleteWorkout = {
                scope.launch {
                    when (appState.history.confirmDeleteWorkout()) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> {
                            appState.progress.refresh()
                            appState.workoutHome.hydrate()
                        }
                    }
                }
            },
            onEditWorkout = { scope.launch { appState.history.beginEditing() } },
            onCancelEditWorkout = { appState.history.cancelEditing() },
            onSaveEditWorkout = {
                scope.launch {
                    when (appState.history.saveEditing()) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> appState.progress.refresh()
                    }
                }
            },
            onEditSet = { exerciseId, setId -> scope.launch { appState.history.editSet(exerciseId, setId) } },
            onAddSet = { exerciseId -> scope.launch { appState.history.addSet(exerciseId) } },
            onCancelSetEdit = { appState.history.cancelSetEdit() },
            onApplySetEdit = { appState.history.applySetEdit() },
            onRepsChange = { appState.history.updateReps(it) },
            onWeightChange = { appState.history.updateWeight(it) },
            onWeightInputChange = { appState.history.updateWeightInput(it) },
            onDurationChange = { appState.history.updateDuration(it) },
            onDistanceChange = { appState.history.updateDistance(it) },
            onDistanceInputChange = { appState.history.updateDistanceInput(it) },
            onEffortChange = { appState.history.updateEffort(it) },
            onRequestDeleteSet = { appState.history.requestDeleteSet(it) },
            onCancelDeleteSet = { appState.history.cancelDeleteSet() },
            onConfirmDeleteSet = { appState.history.confirmDeleteSet() }
        )
        TopLevelDestination.PROGRESS -> ProgressFlow(
            state = progressState,
            onSelectExercise = { exerciseId -> appState.progress.selectExercise(exerciseId) },
            onChartMetricSelected = { metric -> appState.progress.selectChartMetric(metric) },
            onBackFromExercise = { appState.progress.clearExerciseSelection() },
            onOpenEvidence = { recordId ->
                scope.launch { appState.progress.openEvidence(recordId) }
            },
            onCloseEvidence = { appState.progress.clearEvidence() }
        )
        TopLevelDestination.PROFILE -> ProfileFlow(
            state = profileState,
            onWeightUnitSelected = { unit ->
                scope.launch {
                    when (appState.profile.setWeightUnit(unit)) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> {
                            appState.history.refresh()
                            appState.progress.refresh()
                        }
                    }
                }
            },
            onWeightStepClick = { appState.profile.openWeightStepPicker() },
            onWeightStepDraftChange = { step -> appState.profile.setDraftWeightStep(step) },
            onWeightStepSave = { scope.launch { appState.profile.saveWeightStep() } },
            onWeightStepCancel = { appState.profile.cancelWeightStepPicker() },
            onDefaultRestClick = { appState.profile.openDefaultRestPicker() },
            onDefaultRestDraftChange = { seconds -> appState.profile.setDraftDefaultRestSeconds(seconds) },
            onDefaultRestSave = { scope.launch { appState.profile.saveDefaultRest() } },
            onDefaultRestCancel = { appState.profile.cancelDefaultRestPicker() },
            onRestSoundChanged = { enabled ->
                scope.launch { appState.profile.setRestSoundEnabled(enabled) }
            },
            onStartWorkoutTimerWithFirstSetChanged = { enabled ->
                scope.launch { appState.profile.setStartWorkoutTimerWithFirstSet(enabled) }
            },
            onRestTimerSurfaceChanged = { enabled ->
                scope.launch { appState.profile.setRestTimerSurfaceEnabled(enabled) }
            },
            onPaletteModeSelected = { mode ->
                appState.profile.setPaletteMode(mode)
                appState.navigation.setPreferences(
                    reduceMotion = profileState.reduceMotion,
                    hapticsEnabled = profileState.hapticsEnabled,
                    paletteMode = mode
                )
            },
            onHapticsChanged = { enabled ->
                appState.profile.setHapticsEnabled(enabled)
                appState.navigation.setPreferences(
                    reduceMotion = profileState.reduceMotion,
                    hapticsEnabled = enabled,
                    paletteMode = profileState.paletteMode
                )
            },
            onReduceMotionChanged = { enabled ->
                appState.profile.setReduceMotion(enabled)
                appState.navigation.setPreferences(
                    reduceMotion = enabled,
                    hapticsEnabled = profileState.hapticsEnabled,
                    paletteMode = profileState.paletteMode
                )
            },
            onExportRequested = { type ->
                scope.launch { appState.profile.export(type) }
            },
            onPurchaseLifetimeUnlock = {
                scope.launch {
                    appState.profile.purchaseLifetimeUnlock()
                    appState.workoutHome.refreshFullAccess()
                }
            },
            onRestorePurchases = {
                scope.launch {
                    appState.profile.restorePurchases()
                    appState.workoutHome.refreshFullAccess()
                }
            },
            onStartBackupSetup = { appState.profile.startBackupSetup() },
            onBackupSetupNext = { appState.profile.advanceBackupSetup() },
            onBackupSetupBack = { appState.profile.backUpBackupSetup() },
            onBackupSetupDismiss = { appState.profile.dismissBackupSetup() },
            onLinkBackupFile = { scope.launch { appState.profile.linkBackupFile() } },
            onBackupNow = { scope.launch { appState.profile.backupNow() } },
            onSyncNow = { scope.launch { appState.profile.syncNow() } },
            onRestoreFromFile = {
                scope.launch {
                    when (appState.profile.restoreFromFile()) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> appState.hydrate()
                    }
                }
            },
            onKeepLocalBackup = { scope.launch { appState.profile.keepLocalBackup() } },
            onRestoreBackupConflict = {
                scope.launch {
                    when (appState.profile.restoreBackupConflict()) {
                        is FoundationResult.Failure -> Unit
                        is FoundationResult.Success -> appState.hydrate()
                    }
                }
            },
            onCancelBackupConflict = { scope.launch { appState.profile.cancelBackupConflict() } },
            onManageExercises = {
                scope.launch { appState.exerciseManagement.open() }
            },
            onPrivacyPolicy = { openPrivacyPolicy(uriHandler::openUri) },
            developerSeedState = developerSeedState,
            onDeveloperSeedSelected = { scenario ->
                scope.launch {
                    appState.developerSeeds?.load(scenario)
                    appState.hydrate()
                    appState.history.refresh()
                    appState.progress.refresh()
                    appState.workoutHome.hydrate()
                }
            }
        )
    }
}

@Composable
private fun TabGlyph(destination: TopLevelDestination, tint: Color) {
    Canvas(modifier = Modifier.size(FitTheme.size.iconSize)) {
        val strokeWidth = size.minDimension * 0.11f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        when (destination) {
            TopLevelDestination.TRAIN -> {
                val y = size.height * 0.5f
                drawLine(tint, Offset(size.width * 0.22f, y), Offset(size.width * 0.78f, y), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.18f, size.height * 0.34f), Offset(size.width * 0.18f, size.height * 0.66f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.30f, size.height * 0.28f), Offset(size.width * 0.30f, size.height * 0.72f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.70f, size.height * 0.28f), Offset(size.width * 0.70f, size.height * 0.72f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.82f, size.height * 0.34f), Offset(size.width * 0.82f, size.height * 0.66f), strokeWidth, StrokeCap.Round)
            }
            TopLevelDestination.HISTORY -> {
                drawCircle(tint, radius = size.minDimension * 0.36f, center = center, style = stroke)
                drawLine(tint, center, Offset(size.width * 0.50f, size.height * 0.30f), strokeWidth, StrokeCap.Round)
                drawLine(tint, center, Offset(size.width * 0.66f, size.height * 0.55f), strokeWidth, StrokeCap.Round)
            }
            TopLevelDestination.PROGRESS -> {
                val points = listOf(
                    Offset(size.width * 0.16f, size.height * 0.72f),
                    Offset(size.width * 0.38f, size.height * 0.56f),
                    Offset(size.width * 0.56f, size.height * 0.64f),
                    Offset(size.width * 0.82f, size.height * 0.30f)
                )
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(tint, start, end, strokeWidth, StrokeCap.Round)
                }
                points.forEach { point ->
                    drawCircle(tint, radius = strokeWidth * 0.65f, center = point)
                }
            }
            TopLevelDestination.PROFILE -> {
                drawCircle(tint, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.5f, size.height * 0.34f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.20f, size.height * 0.48f),
                    size = Size(size.width * 0.60f, size.height * 0.42f),
                    style = stroke
                )
            }
        }
    }
}
