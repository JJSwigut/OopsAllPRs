package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.usecase.ActivePrFeedbackUseCase
import com.jjswigut.oopsallprs.domain.usecase.ExerciseLoggingConfigurationUseCases
import com.jjswigut.oopsallprs.domain.usecase.PreviousWorkoutDefaultsUseCase
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ActiveWorkoutState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val lastLoggedSet: ExerciseSet? = null,
    val workout: ActiveWorkoutView? = null,
    val focusSnapshot: ActiveWorkoutFocus? = null,
    val editDraft: LoggedSetEditDraft? = null,
    val isExerciseOverviewVisible: Boolean = false,
    val isDiscardConfirmationVisible: Boolean = false,
    val isFinishConfirmationVisible: Boolean = false,
    val canUndoLastSet: Boolean = false,
    val personalRecords: List<PersonalRecord> = emptyList()
)

class ActiveWorkoutStateHolder(
    private val setLogging: SetLoggingUseCases,
    private val lifecycle: WorkoutLifecycleUseCases,
    private val activeUx: ActiveWorkoutUxRepository? = null,
    private val activePrFeedback: ActivePrFeedbackUseCase? = null,
    private val previousDefaults: PreviousWorkoutDefaultsUseCase? = null,
    private val configurationManagement: ExerciseLoggingConfigurationUseCases? = null,
    private val preferences: PreferencesRepository? = null,
    private val progress: ProgressRepository? = null
) {
    private val drafts = linkedMapOf<FoundationId, SetRowDraft>()
    private val prFeedbackBySetId = linkedMapOf<FoundationId, ActivePrFeedback>()
    private val configurations = linkedMapOf<LoggingConfigurationId, LoggingConfiguration>()
    private val saveDefaultExerciseIds = linkedSetOf<FoundationId>()
    private var activeWorkoutId: FoundationId? = null
    private var focus: ActiveWorkoutFocus? = null
    private var startTimerWithFirstSet: Boolean = true

    private val _state = MutableStateFlow(ActiveWorkoutState())
    val state: StateFlow<ActiveWorkoutState> = _state

    suspend fun hydrate(
        workoutId: FoundationId,
        restoredFocus: ActiveWorkoutFocus? = null,
        now: Instant = Clock.System.now()
    ) {
        activeWorkoutId = workoutId
        val workout = lifecycle.activeWorkout(workoutId)
        if (workout == null) {
            _state.value = ActiveWorkoutState(errorMessage = "Active workout not found")
            return
        }

        hydrateConfigurations(workout)
        val personalRecords = progress?.personalRecords().orEmpty()
        startTimerWithFirstSet = preferences?.startWorkoutTimerWithFirstSet() ?: true
        hydrateDrafts(workout)
        hydrateConfigurationPreferences(workout)
        focus = restoredFocus ?: activeUx?.loadUxSession(workoutId)?.toFocus()
        val focusedExerciseId = focus?.exerciseInstanceId
            ?: workout.exercises.minByOrNull { it.position.value }?.id
        focusedExerciseId?.let {
            workout.nextExerciseAfterCompletedCircuit(it)?.let { next ->
                focus = ActiveWorkoutFocus(
                    exerciseInstanceId = next.id,
                    draftId = FoundationId("draft-${next.id.value}-${next.sets.size}"),
                    updatedAt = now
                )
            }
        }
        val session = lifecycle.restoreActiveSession(now)?.takeIf { it.activeWorkoutId == workoutId }
        publish(workout, now, session)
        _state.value = _state.value.copy(personalRecords = personalRecords)
        val focusedBlock = _state.value.workout?.exerciseBlocks?.firstOrNull {
            it.exerciseInstanceId == focus?.exerciseInstanceId
        }
        if (focusedBlock?.circuitProgress?.isComplete == true) {
            _state.value = _state.value.copy(isExerciseOverviewVisible = true)
        }
        persistVisibleDraftsAndFocus(now)
        recomputeVisiblePrFeedback(workout, now)
    }

    suspend fun addExercise(
        workoutId: FoundationId,
        reference: ExerciseReference
    ): FoundationResult<FoundationId> {
        val result = setLogging.addExercise(workoutId, reference)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                activeWorkoutId = workoutId
                val now = Clock.System.now()
                val draftId = FoundationId("draft-${result.value.id.value}-0")
                previousDefaults?.valueFor(
                    exerciseCatalogId = result.value.reference.exerciseCatalogId,
                    isBodyweight = result.value.reference.isBodyweight,
                    loggingMode = result.value.reference.loggingMode,
                    setIndex = 0,
                    loggingConfiguration = result.value.resolvedLoggingConfiguration.configuration
                )?.let { previous ->
                    drafts[result.value.id] = SetRowDraft(
                        draftId = draftId,
                        exerciseInstanceId = result.value.id,
                        position = OrderedPosition(0),
                        setKind = result.value.resolvedLoggingConfiguration.configuration
                            .legacySetKind(result.value.reference.isBodyweight),
                        captureConfigurationId = result.value.resolvedLoggingConfiguration.configuration.id,
                        loggingConfiguration = result.value.resolvedLoggingConfiguration.configuration,
                        reps = previous.reps,
                        weight = previous.weight,
                        durationMs = previous.durationMs,
                        distanceMeters = previous.distanceMeters,
                        prefillHint = "Last logged values"
                    )
                }
                focus = ActiveWorkoutFocus(
                    exerciseInstanceId = result.value.id,
                    draftId = draftId,
                    updatedAt = now
                )
                persistFocus(now)
                hydrate(workoutId, focus, now)
                _state.value = _state.value.copy(isExerciseOverviewVisible = false)
                foundationSuccess(result.value.id)
            }
        }
    }

    suspend fun confirmFocusedDraft(): FoundationResult<ExerciseSet> {
        val exerciseId = focus?.exerciseInstanceId
            ?: return foundationFailure(FoundationError.Validation("No focused set to log"))
        return confirmDraft(exerciseId)
    }

    suspend fun groupExercisesAsCircuit(exerciseInstanceIds: List<FoundationId>): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        return when (val result = setLogging.groupExercisesAsCircuit(workoutId, exerciseInstanceIds)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus)
                foundationSuccess(Unit)
            }
        }
    }

    suspend fun ungroupCircuit(exerciseInstanceId: FoundationId): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        return when (val result = setLogging.ungroupCircuit(workoutId, exerciseInstanceId)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus)
                foundationSuccess(Unit)
            }
        }
    }

    suspend fun adjustCircuitRounds(exerciseInstanceId: FoundationId, deltaRounds: Int): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        return when (val result = setLogging.adjustCircuitRounds(workoutId, exerciseInstanceId, deltaRounds)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus)
                foundationSuccess(Unit)
            }
        }
    }

    suspend fun confirmDraft(exerciseInstanceId: FoundationId): FoundationResult<ExerciseSet> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val view = _state.value.workout
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val block = view.exerciseBlocks.firstOrNull { it.exerciseInstanceId == exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        val draft = block.draft
        if (draft.isPending) {
            return foundationFailure(FoundationError.Conflict("Set is already saving"))
        }
        validateDraft(draft)?.let { error ->
            setDraft(draft.copy(inlineError = error.message))
            return foundationFailure(error)
        }

        setDraft(draft.copy(isPending = true, inlineError = null))
        _state.value = _state.value.copy(isSaving = true, errorMessage = null)
        val loggedAt = Clock.System.now()
        val result = setLogging.confirmSet(
            activeWorkoutId = workoutId,
            exerciseInstanceId = exerciseInstanceId,
            setKind = draft.setKind,
            reps = draft.reps,
            weight = draft.weight,
            durationMs = draft.effectiveDurationMs(),
            distanceMeters = draft.distanceMeters,
            observedEffort = draft.observedEffort,
            position = draft.position.value,
            loggedAt = loggedAt
        )

        return when (result) {
            is FoundationResult.Failure -> {
                setDraft(draft.copy(isPending = false, inlineError = result.error.message))
                _state.value = _state.value.copy(isSaving = false, errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                activeUx?.clearExerciseDrafts(workoutId, exerciseInstanceId)
                drafts.remove(exerciseInstanceId)
                val savedWorkout = lifecycle.activeWorkout(workoutId)
                if (savedWorkout != null) {
                    derivePrFeedback(savedWorkout, exerciseInstanceId, result.value)
                }
                val transition = savedWorkout?.loggingTransitionAfter(
                    exerciseInstanceId = exerciseInstanceId,
                    loggedPosition = result.value.position
                ) ?: LoggingTransition(exerciseInstanceId, RestAfterLogging.KEEP)
                val transitionAt = result.value.loggedAt ?: loggedAt
                when (transition.restAfterLogging) {
                    RestAfterLogging.KEEP -> Unit
                    RestAfterLogging.CLEAR -> lifecycle.clearRestTimer(workoutId, transitionAt)
                    RestAfterLogging.START_CONFIGURED -> lifecycle.startRestTimer(
                        activeWorkoutId = workoutId,
                        originSetId = result.value.id,
                        durationSeconds = block.rest.durationSeconds,
                        now = transitionAt
                    )
                }
                _state.value = _state.value.copy(isSaving = false, lastLoggedSet = result.value, errorMessage = null)
                val nextExercise = savedWorkout?.exercises?.firstOrNull { it.id == transition.nextExerciseId }
                focus = nextExercise?.let {
                    ActiveWorkoutFocus(
                        exerciseInstanceId = it.id,
                        draftId = FoundationId("draft-${it.id.value}-${it.sets.size}"),
                        updatedAt = transitionAt
                    )
                } ?: focus
                hydrate(workoutId, focus)
                _state.value = _state.value.copy(isExerciseOverviewVisible = transition.showExerciseOverview)
                result
            }
        }
    }

    suspend fun updateDraftReps(exerciseInstanceId: FoundationId, reps: Int?) {
        updateDraft(exerciseInstanceId) { it.copy(reps = reps, inlineError = null) }
    }

    suspend fun updateDraftWeight(exerciseInstanceId: FoundationId, weight: WeightKg?) {
        updateDraft(exerciseInstanceId) {
            it.copy(weight = weight, weightInput = null, inputError = null, inlineError = null)
        }
    }

    suspend fun updateDraftWeightInput(exerciseInstanceId: FoundationId, update: MeasureInputUpdate<WeightKg>) {
        updateDraft(exerciseInstanceId) {
            it.copy(
                weight = update.value,
                weightInput = update.rawValue,
                inputError = update.errorMessage,
                inlineError = null
            )
        }
    }

    suspend fun updateDraftDuration(exerciseInstanceId: FoundationId, durationMs: Long?) {
        updateDraft(exerciseInstanceId) {
            it.copy(
                durationMs = durationMs?.coerceAtLeast(0L),
                timerStartedAt = null,
                previewDurationMs = null,
                inlineError = null
            )
        }
    }

    suspend fun updateDraftDistance(exerciseInstanceId: FoundationId, distanceMeters: Double?) {
        updateDraft(exerciseInstanceId) {
            it.copy(distanceMeters = distanceMeters, distanceInput = null, inputError = null, inlineError = null)
        }
    }

    suspend fun updateDraftDistanceInput(exerciseInstanceId: FoundationId, update: MeasureInputUpdate<Double>) {
        updateDraft(exerciseInstanceId) {
            it.copy(
                distanceMeters = update.value,
                distanceInput = update.rawValue,
                inputError = update.errorMessage,
                inlineError = null
            )
        }
    }

    suspend fun updateDraftEffort(exerciseInstanceId: FoundationId, update: MeasureInputUpdate<Effort>) {
        updateDraft(exerciseInstanceId) {
            it.copy(
                observedEffort = update.value,
                effortInput = update.rawValue,
                inputError = update.errorMessage,
                inlineError = null
            )
        }
    }

    suspend fun setBodyweightAddedLoad(exerciseInstanceId: FoundationId, enabled: Boolean): FoundationResult<Unit> =
        updateLoggingConfiguration(exerciseInstanceId) { workoutId, management ->
            management.setBodyweightAddedLoad(workoutId, exerciseInstanceId, enabled)
        }

    suspend fun setTrackEffort(exerciseInstanceId: FoundationId, enabled: Boolean): FoundationResult<Unit> {
        val block = _state.value.workout?.exerciseBlocks?.firstOrNull { it.exerciseInstanceId == exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        val kinds = if (enabled) listOf(block.effortKind ?: EffortKind.RIR) else emptyList()
        return setEffortKind(exerciseInstanceId, kinds.firstOrNull())
    }

    suspend fun setEffortKind(exerciseInstanceId: FoundationId, effortKind: EffortKind?): FoundationResult<Unit> =
        updateLoggingConfiguration(exerciseInstanceId) { workoutId, management ->
            management.setObservedEffort(workoutId, exerciseInstanceId, listOfNotNull(effortKind))
        }

    suspend fun saveActiveConfigurationAsDefault(exerciseInstanceId: FoundationId): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val management = configurationManagement
            ?: return foundationFailure(FoundationError.Persistence("Exercise logging configuration is unavailable"))
        return when (val result = management.saveActiveConfigurationAsDefault(workoutId, exerciseInstanceId)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                lifecycle.activeWorkout(workoutId)?.let { hydrate(workoutId, focus) }
                foundationSuccess(Unit)
            }
        }
    }

    suspend fun adjustDraftDuration(exerciseInstanceId: FoundationId, deltaMs: Long) {
        updateDraft(exerciseInstanceId) {
            val next = ((it.effectiveDurationMs() ?: 0L) + deltaMs).coerceAtLeast(0L)
            it.copy(durationMs = next, timerStartedAt = null, previewDurationMs = null, inlineError = null)
        }
    }

    suspend fun toggleDraftTimer(exerciseInstanceId: FoundationId, now: Instant = Clock.System.now()) {
        updateDraft(exerciseInstanceId) { draft ->
            if (draft.timerStartedAt == null) {
                draft.copy(timerStartedAt = now, previewDurationMs = null, inlineError = null)
            } else {
                draft.copy(durationMs = draft.effectiveDurationMs(), timerStartedAt = null, previewDurationMs = null, inlineError = null)
            }
        }
    }

    suspend fun adjustExerciseRest(exerciseInstanceId: FoundationId, deltaSeconds: Int): FoundationResult<FoundationId> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val block = _state.value.workout?.exerciseBlocks?.firstOrNull { it.exerciseInstanceId == exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        val current = block.rest
        val base = if (current.durationSeconds > 0) current.durationSeconds else RestConfiguration.DEFAULT_SECONDS
        val nextRest = current.copy(
            durationSeconds = (base + deltaSeconds).coerceAtLeast(0),
            autoStart = true
        )
        return when (val result = setLogging.updateExerciseRest(workoutId, exerciseInstanceId, nextRest)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus)
                foundationSuccess(exerciseInstanceId)
            }
        }
    }

    suspend fun toggleExerciseRest(exerciseInstanceId: FoundationId): FoundationResult<FoundationId> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val block = _state.value.workout?.exerciseBlocks?.firstOrNull { it.exerciseInstanceId == exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        val nextRest = if (block.rest.isEnabled) {
            RestConfiguration.disabled()
        } else {
            RestConfiguration.default()
        }
        return when (val result = setLogging.updateExerciseRest(workoutId, exerciseInstanceId, nextRest)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus)
                foundationSuccess(exerciseInstanceId)
            }
        }
    }

    suspend fun adjustActiveRest(deltaSeconds: Int, now: Instant = Clock.System.now()): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        return when (val result = lifecycle.adjustRestTimer(workoutId, deltaSeconds, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus, now)
                foundationSuccess(Unit)
            }
        }
    }

    suspend fun skipActiveRest(now: Instant = Clock.System.now()): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        return when (val result = lifecycle.clearRestTimer(workoutId, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus, now)
                foundationSuccess(Unit)
            }
        }
    }

    suspend fun refreshTimers(now: Instant = Clock.System.now()) {
        val workoutId = activeWorkoutId ?: return
        val workout = lifecycle.activeWorkout(workoutId) ?: return
        val session = lifecycle.restoreActiveSession(now)?.takeIf { it.activeWorkoutId == workoutId }
        publish(workout, now, session)
    }

    suspend fun setFocus(exerciseInstanceId: FoundationId, now: Instant = Clock.System.now()) {
        val block = _state.value.workout?.exerciseBlocks?.firstOrNull { it.exerciseInstanceId == exerciseInstanceId }
        if (block?.circuitProgress?.isComplete == true) {
            _state.value = _state.value.copy(isExerciseOverviewVisible = true)
            return
        }
        val draft = block?.draft
        if (draft != null) {
            focus = ActiveWorkoutFocus(exerciseInstanceId, draft.draftId, now)
            persistFocus(now)
            _state.value = _state.value.copy(
                focusSnapshot = focus,
                workout = _state.value.workout?.copy(focus = focus),
                isExerciseOverviewVisible = false
            )
        }
    }

    fun showExerciseOverview() {
        if (_state.value.workout?.exerciseBlocks?.isNotEmpty() == true) {
            _state.value = _state.value.copy(isExerciseOverviewVisible = true)
        }
    }

    fun beginEditSet(setId: FoundationId) {
        val view = _state.value.workout ?: return
        val block = view.exerciseBlocks.firstNotNullOfOrNull { candidate ->
            candidate.loggedRows.firstOrNull { it.setId == setId }?.let { row -> candidate to row }
        } ?: return
        val (exercise, row) = block
        val configuration = row.loggingConfiguration
        if (configuration == null) {
            _state.value = _state.value.copy(
                errorMessage = "This set's logging configuration is unavailable, so it cannot be edited"
            )
            return
        }
        _state.value = _state.value.copy(
            editDraft = LoggedSetEditDraft(
                setId = row.setId,
                exerciseName = exercise.displayName,
                rowDraft = SetRowDraft(
                    draftId = FoundationId("edit-${row.setId.value}"),
                    exerciseInstanceId = exercise.exerciseInstanceId,
                    position = row.position,
                    setKind = row.setKind,
                    captureConfigurationId = row.captureConfigurationId,
                    loggingConfiguration = configuration,
                    reps = row.reps,
                    weight = row.weight,
                    durationMs = row.durationMs,
                    distanceMeters = row.distanceMeters,
                    observedEffort = row.observedEffort
                )
            ),
            isDiscardConfirmationVisible = false,
            isFinishConfirmationVisible = false,
            errorMessage = null
        )
    }

    fun cancelEditSet() {
        _state.value = _state.value.copy(editDraft = null, errorMessage = null)
    }

    fun updateEditReps(reps: Int?) {
        updateEditDraft { it.copy(reps = reps, inlineError = null) }
    }

    fun updateEditWeight(weight: WeightKg?) {
        updateEditDraft { it.copy(weight = weight, weightInput = null, inputError = null, inlineError = null) }
    }

    fun updateEditWeightInput(update: MeasureInputUpdate<WeightKg>) {
        updateEditDraft {
            it.copy(
                weight = update.value,
                weightInput = update.rawValue,
                inputError = update.errorMessage,
                inlineError = null
            )
        }
    }

    fun updateEditDuration(durationMs: Long?) {
        updateEditDraft { it.copy(durationMs = durationMs?.coerceAtLeast(0L), inlineError = null) }
    }

    fun updateEditDistance(distanceMeters: Double?) {
        updateEditDraft {
            it.copy(distanceMeters = distanceMeters, distanceInput = null, inputError = null, inlineError = null)
        }
    }

    fun updateEditDistanceInput(update: MeasureInputUpdate<Double>) {
        updateEditDraft {
            it.copy(
                distanceMeters = update.value,
                distanceInput = update.rawValue,
                inputError = update.errorMessage,
                inlineError = null
            )
        }
    }

    fun updateEditEffort(update: MeasureInputUpdate<Effort>) {
        updateEditDraft {
            it.copy(
                observedEffort = update.value,
                effortInput = update.rawValue,
                inputError = update.errorMessage,
                inlineError = null
            )
        }
    }

    suspend fun confirmEditSet(now: Instant = Clock.System.now()): FoundationResult<ExerciseSet> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val edit = _state.value.editDraft
            ?: return foundationFailure(FoundationError.Validation("No logged set selected for editing"))
        validateDraft(edit.rowDraft)?.let { error ->
            _state.value = _state.value.copy(
                editDraft = edit.copy(rowDraft = edit.rowDraft.copy(inlineError = error.message)),
                errorMessage = error.message
            )
            return foundationFailure(error)
        }

        val pendingEdit = edit.copy(rowDraft = edit.rowDraft.copy(isPending = true, inlineError = null))
        _state.value = _state.value.copy(editDraft = pendingEdit, isSaving = true, errorMessage = null)
        return when (
            val result = setLogging.editLoggedSet(
                activeWorkoutId = workoutId,
                setId = edit.setId,
                reps = edit.rowDraft.reps,
                weight = edit.rowDraft.weight,
                durationMs = edit.rowDraft.effectiveDurationMs(),
                distanceMeters = edit.rowDraft.distanceMeters,
                observedEffort = edit.rowDraft.observedEffort,
                now = now
            )
        ) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(
                    editDraft = edit.copy(rowDraft = edit.rowDraft.copy(isPending = false, inlineError = result.error.message)),
                    isSaving = false,
                    errorMessage = result.error.message
                )
                result
            }
            is FoundationResult.Success -> {
                prFeedbackBySetId.remove(edit.setId)
                _state.value = _state.value.copy(editDraft = null, isSaving = false, lastLoggedSet = result.value, errorMessage = null)
                hydrate(workoutId, focus, now)
                result
            }
        }
    }

    suspend fun deleteLoggedSet(setId: FoundationId, now: Instant = Clock.System.now()): FoundationResult<ExerciseSet> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        _state.value = _state.value.copy(isSaving = true, errorMessage = null)
        return when (val result = setLogging.deleteLoggedSet(workoutId, setId, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(isSaving = false, errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                prFeedbackBySetId.remove(setId)
                lifecycle.clearRestIfOrigin(workoutId, setId, now)
                _state.value = _state.value.copy(
                    isSaving = false,
                    editDraft = _state.value.editDraft?.takeIf { it.setId != setId },
                    lastLoggedSet = _state.value.lastLoggedSet?.takeIf { it.id != setId },
                    errorMessage = null
                )
                hydrate(workoutId, focus, now)
                result
            }
        }
    }

    suspend fun undoLastLoggedSet(now: Instant = Clock.System.now()): FoundationResult<ExerciseSet> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        _state.value = _state.value.copy(isSaving = true, errorMessage = null)
        return when (val result = setLogging.undoLastLoggedSet(workoutId, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(isSaving = false, errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                prFeedbackBySetId.remove(result.value.id)
                lifecycle.clearRestIfOrigin(workoutId, result.value.id, now)
                _state.value = _state.value.copy(
                    isSaving = false,
                    editDraft = _state.value.editDraft?.takeIf { it.setId != result.value.id },
                    lastLoggedSet = _state.value.lastLoggedSet?.takeIf { it.id != result.value.id },
                    errorMessage = null
                )
                hydrate(workoutId, focus, now)
                result
            }
        }
    }

    fun requestDiscard() {
        _state.value = _state.value.copy(
            isDiscardConfirmationVisible = true,
            isFinishConfirmationVisible = false,
            editDraft = null,
            errorMessage = null
        )
    }

    fun cancelDiscard() {
        _state.value = _state.value.copy(isDiscardConfirmationVisible = false, errorMessage = null)
    }

    fun requestFinish() {
        if (_state.value.workout?.exerciseBlocks?.any { it.loggedRows.isNotEmpty() } != true) {
            _state.value = _state.value.copy(
                isFinishConfirmationVisible = false,
                isDiscardConfirmationVisible = false,
                editDraft = null,
                errorMessage = "Log at least one set before finishing, or discard this workout."
            )
            return
        }
        _state.value = _state.value.copy(
            isFinishConfirmationVisible = true,
            isDiscardConfirmationVisible = false,
            editDraft = null,
            errorMessage = null
        )
    }

    fun cancelFinish() {
        _state.value = _state.value.copy(isFinishConfirmationVisible = false, errorMessage = null)
    }

    fun reportFinishFailure() {
        _state.value = _state.value.copy(
            isFinishConfirmationVisible = true,
            errorMessage = "Couldn't finish the workout. Please try again."
        )
    }

    suspend fun confirmDiscard(now: Instant = Clock.System.now()): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        _state.value = _state.value.copy(isSaving = true, errorMessage = null)
        return when (val result = lifecycle.discard(workoutId, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(isSaving = false, errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                activeWorkoutId = null
                focus = null
                drafts.clear()
                prFeedbackBySetId.clear()
                _state.value = ActiveWorkoutState()
                result
            }
        }
    }

    fun snapshotFocus(): ActiveWorkoutFocus? = focus

    internal fun restoreDraftForTest(draft: SetRowDraft) {
        drafts[draft.exerciseInstanceId] = draft
        _state.value.workout?.let { view ->
            _state.value = _state.value.copy(
                workout = view.copy(
                    exerciseBlocks = view.exerciseBlocks.map { block ->
                        if (block.exerciseInstanceId == draft.exerciseInstanceId) {
                            block.copy(draft = draft, inlineError = draft.inlineError)
                        } else {
                            block
                        }
                    }
                )
            )
        }
    }

    private suspend fun hydrateDrafts(workout: ActiveWorkout) {
        val validExerciseIds = workout.exercises.map { it.id }.toSet()
        val persisted = activeUx?.loadSetDrafts(workout.id).orEmpty()
        persisted
            .filter { it.exerciseInstanceId in validExerciseIds }
            .forEach { draft ->
                val configuration = configurationFor(draft.captureConfigurationId)
                if (configuration == null) {
                    _state.value = _state.value.copy(
                        errorMessage = "Saved draft configuration is unavailable; the exercise was reset to its current logger"
                    )
                } else {
                    drafts[draft.exerciseInstanceId] = draft.toRowDraft(configuration)
                }
            }
        drafts.entries.removeAll { it.key !in validExerciseIds }
    }

    private suspend fun updateDraft(
        exerciseInstanceId: FoundationId,
        transform: (SetRowDraft) -> SetRowDraft
    ) {
        val current = _state.value.workout?.exerciseBlocks?.firstOrNull { it.exerciseInstanceId == exerciseInstanceId }?.draft
            ?: return
        setDraft(transform(current).copy(prefillHint = null))
    }

    private suspend fun setDraft(draft: SetRowDraft) {
        drafts[draft.exerciseInstanceId] = draft
        persistDraft(draft)
        _state.value.workout?.let { view ->
            _state.value = _state.value.copy(
                workout = view.copy(
                    exerciseBlocks = view.exerciseBlocks.map { block ->
                        if (block.exerciseInstanceId == draft.exerciseInstanceId) {
                            block.copy(draft = draft, inlineError = draft.inlineError)
                        } else {
                            block
                        }
                    }
                )
            )
        }
    }

    private fun updateEditDraft(transform: (SetRowDraft) -> SetRowDraft) {
        val edit = _state.value.editDraft ?: return
        _state.value = _state.value.copy(editDraft = edit.copy(rowDraft = transform(edit.rowDraft)))
    }

    private fun validateDraft(draft: SetRowDraft): FoundationError? {
        draft.inputError?.let { return FoundationError.Validation(it) }
        return ExerciseSet(
            id = draft.draftId,
            exerciseInstanceId = draft.exerciseInstanceId,
            position = draft.position,
            setKind = draft.setKind,
            weight = draft.weight,
            reps = draft.reps,
            durationMs = draft.effectiveDurationMs(),
            captureConfigurationId = draft.captureConfigurationId,
            distanceMeters = draft.distanceMeters,
            observedEffort = draft.observedEffort,
            loggedAt = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        ).validateForLogging(draft.loggingConfiguration)
    }

    private fun publish(workout: ActiveWorkout, now: Instant, activeSession: com.jjswigut.oopsallprs.domain.model.ActiveSessionState? = null) {
        val view = workout.toView(
            drafts = drafts,
            focus = focus,
            prFeedbackBySetId = prFeedbackBySetId,
            activeSession = activeSession,
            configurations = configurations,
            saveDefaultExerciseIds = saveDefaultExerciseIds,
            startTimerWithFirstSet = startTimerWithFirstSet,
            now = now,
            errorMessage = _state.value.errorMessage
        )
        view.exerciseBlocks.forEach { block ->
            if (!drafts.containsKey(block.exerciseInstanceId)) {
                drafts[block.exerciseInstanceId] = block.draft
            }
        }
        focus = view.focus
        _state.value = _state.value.copy(
            workout = view,
            focusSnapshot = view.focus,
            errorMessage = view.errorMessage,
            canUndoLastSet = workout.exercises.any { exercise -> exercise.sets.any { it.isLogged } }
        )
    }

    private suspend fun persistVisibleDraftsAndFocus(now: Instant) {
        drafts.values.forEach { draft ->
            persistDraft(draft)
        }
        persistFocus(now)
    }

    private suspend fun persistDraft(draft: SetRowDraft, now: Instant = Clock.System.now()) {
        val workoutId = activeWorkoutId ?: return
        val result = activeUx?.saveSetDraft(
            PersistedSetDraft(
                draftId = draft.draftId,
                activeWorkoutId = workoutId,
                exerciseInstanceId = draft.exerciseInstanceId,
                position = draft.position,
                setKind = draft.setKind,
                reps = draft.reps,
                weight = draft.weight,
                durationMs = draft.durationMs,
                timerStartedAt = draft.timerStartedAt,
                captureConfigurationId = draft.captureConfigurationId,
                distanceMeters = draft.distanceMeters,
                observedEffort = draft.observedEffort,
                updatedAt = now
            )
        )
        if (result is FoundationResult.Failure) {
            _state.value = _state.value.copy(errorMessage = result.error.message)
        }
    }

    private suspend fun persistFocus(now: Instant = Clock.System.now()) {
        val workoutId = activeWorkoutId ?: return
        val current = focus ?: return
        val result = activeUx?.saveUxSession(
            ActiveWorkoutUxSession(
                activeWorkoutId = workoutId,
                focusedExerciseInstanceId = current.exerciseInstanceId,
                focusedDraftId = current.draftId,
                updatedAt = now
            )
        )
        if (result is FoundationResult.Failure) {
            _state.value = _state.value.copy(errorMessage = result.error.message)
        }
    }

    private suspend fun derivePrFeedback(
        workout: ActiveWorkout,
        exerciseInstanceId: FoundationId,
        set: ExerciseSet
    ) {
        val exercise = workout.exercises.firstOrNull { it.id == exerciseInstanceId } ?: return
        val feedback = activePrFeedback?.feedbackFor(workout, exercise, set) ?: return
        prFeedbackBySetId[feedback.setId] = feedback
    }

    private suspend fun recomputeVisiblePrFeedback(workout: ActiveWorkout, now: Instant) {
        val previous = prFeedbackBySetId.toMap()
        val next = linkedMapOf<FoundationId, ActivePrFeedback>()
        workout.exercises.forEach { exercise ->
            exercise.sets.filter { it.isLogged }.forEach { set ->
                val feedback = activePrFeedback?.feedbackFor(workout, exercise, set)
                val existing = previous[set.id]
                if (existing != null) {
                    next[set.id] = existing
                } else if (feedback != null) {
                    next[set.id] = feedback
                }
            }
        }
        prFeedbackBySetId.clear()
        prFeedbackBySetId.putAll(next)
        if (previous != prFeedbackBySetId) {
            publish(workout, now, lifecycle.restoreActiveSession(now))
        }
    }

    private fun ActiveWorkoutUxSession.toFocus(): ActiveWorkoutFocus? {
        val exerciseId = focusedExerciseInstanceId ?: return null
        val draftId = focusedDraftId ?: return null
        return ActiveWorkoutFocus(exerciseId, draftId, updatedAt)
    }

    private fun PersistedSetDraft.toRowDraft(configuration: LoggingConfiguration): SetRowDraft =
        SetRowDraft(
            draftId = draftId,
            exerciseInstanceId = exerciseInstanceId,
            position = position,
            setKind = setKind,
            captureConfigurationId = captureConfigurationId,
            loggingConfiguration = configuration,
            reps = reps,
            weight = weight,
            durationMs = durationMs,
            distanceMeters = distanceMeters,
            observedEffort = observedEffort,
            timerStartedAt = timerStartedAt
        )

    private suspend fun hydrateConfigurations(workout: ActiveWorkout) {
        configurations.clear()
        workout.exercises.forEach { exercise ->
            val active = exercise.resolvedLoggingConfiguration.configuration
            configurations[active.id] = active
            exercise.sets.map { it.captureConfigurationId }.distinct().forEach { id ->
                configurationFor(id)
            }
        }
    }

    private suspend fun configurationFor(id: LoggingConfigurationId): LoggingConfiguration? {
        configurations[id]?.let { return it }
        val resolved = configurationManagement?.loggingConfiguration(id)
            ?: LegacyLoggingConfigurations.all.firstOrNull { it.id == id }
            ?: return null
        configurations[id] = resolved
        return resolved
    }

    private suspend fun hydrateConfigurationPreferences(workout: ActiveWorkout) {
        saveDefaultExerciseIds.clear()
        val management = configurationManagement ?: return
        workout.exercises.forEach { exercise ->
            val result = management.activeConfigurationPreferenceState(workout.id, exercise.id)
            if (
                result is FoundationResult.Success &&
                exercise.resolvedLoggingConfiguration.source == LoggingConfigurationSource.WORKOUT_OVERRIDE &&
                !result.value.matchesSavedDefault
            ) {
                saveDefaultExerciseIds += exercise.id
            }
        }
    }

    private suspend fun updateLoggingConfiguration(
        exerciseInstanceId: FoundationId,
        update: suspend (FoundationId, ExerciseLoggingConfigurationUseCases) -> FoundationResult<*>
    ): FoundationResult<Unit> {
        val workoutId = activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout loaded"))
        val management = configurationManagement
            ?: return foundationFailure(FoundationError.Persistence("Exercise logging configuration is unavailable"))
        return when (val result = update(workoutId, management)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                hydrate(workoutId, focus)
                foundationSuccess(Unit)
            }
        }
    }
}
