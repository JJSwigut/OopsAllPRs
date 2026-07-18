package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.usecase.CompletedWorkoutCorrectionUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.workout.MeasureInputUpdate
import com.jjswigut.oopsallprs.ui.workout.SetRowDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class CompletedWorkoutEditDraft(
    val workout: CompletedWorkout,
    val setEditor: CompletedSetEditor? = null,
    val pendingDeleteSetId: FoundationId? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class CompletedSetEditor(
    val exerciseId: FoundationId,
    val originalSetId: FoundationId?,
    val draft: SetRowDraft
)

data class HistoryState(
    val completedWorkouts: List<CompletedWorkout> = emptyList(),
    val rows: List<HistoryListItem> = emptyList(),
    val selectedSummary: CompletedWorkoutSummary? = null,
    val editDraft: CompletedWorkoutEditDraft? = null,
    val pendingDeleteSummary: CompletedWorkoutSummary? = null,
    val errorMessage: String? = null
)

class HistoryStateHolder(
    private val workouts: WorkoutRepository,
    private val progress: ProgressRepository? = null,
    private val routines: RoutineUseCases? = null,
    private val corrections: CompletedWorkoutCorrectionUseCase? = null,
    private val configurations: LoggingConfigurationRepository? = null
) {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state

    suspend fun refresh() {
        val completed = workouts.completedWorkouts().sortedByDescending { it.finishedAt }
        val records = progress?.personalRecords().orEmpty()
        val selectedId = _state.value.selectedSummary?.workoutId
        _state.value = HistoryState(
            completedWorkouts = completed,
            rows = completed.toHistoryRows(records),
            selectedSummary = selectedId?.let { id ->
                completed.firstOrNull { it.id == id }?.toSummary(records)
            },
            editDraft = _state.value.editDraft,
            pendingDeleteSummary = null,
            errorMessage = null
        )
    }

    suspend fun selectWorkout(workoutId: FoundationId) {
        val completed = workouts.completedWorkout(workoutId)
        val records = progress?.personalRecords().orEmpty()
        _state.value = if (completed == null) {
            _state.value.copy(errorMessage = "Completed workout not found")
        } else {
            _state.value.copy(
                selectedSummary = completed.toSummary(records),
                errorMessage = null
            )
        }
    }

    suspend fun presentCompletedWorkout(workoutId: FoundationId) {
        refresh()
        selectWorkout(workoutId)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedSummary = null, editDraft = null, errorMessage = null)
    }

    suspend fun beginEditing() {
        val summary = _state.value.selectedSummary ?: return
        val workout = workouts.completedWorkout(summary.workoutId)
        if (workout == null || corrections == null || configurations == null) {
            _state.value = _state.value.copy(errorMessage = "Workout editing is unavailable")
            return
        }
        _state.value = _state.value.copy(
            editDraft = CompletedWorkoutEditDraft(workout),
            errorMessage = null
        )
    }

    fun cancelEditing() {
        _state.value = _state.value.copy(editDraft = null, errorMessage = null)
    }

    suspend fun editSet(exerciseId: FoundationId, setId: FoundationId) {
        val editing = _state.value.editDraft ?: return
        val set = editing.workout.exercises.firstOrNull { it.id == exerciseId }
            ?.loggedSets?.firstOrNull { it.id == setId } ?: return
        openSetEditor(editing, exerciseId, set, set.id)
    }

    suspend fun addSet(exerciseId: FoundationId) {
        val editing = _state.value.editDraft ?: return
        val exercise = editing.workout.exercises.firstOrNull { it.id == exerciseId } ?: return
        val previous = exercise.loggedSets.lastOrNull()
        if (previous == null) {
            updateEditError("A missed set needs an existing captured logging configuration")
            return
        }
        val enabled = configurations?.loggingConfiguration(previous.captureConfigurationId)?.measures
            ?.map { it.kind }?.toSet().orEmpty()
        val loadRequirement = configurations?.loggingConfiguration(previous.captureConfigurationId)
            ?.measures?.firstOrNull { it.kind == MeasureKind.LOAD }?.requirement
        val template = previous.copy(
            id = newFoundationId("corrected-set"),
            position = OrderedPosition(exercise.loggedSets.size),
            reps = previous.reps?.takeIf { MeasureKind.REPETITIONS in enabled },
            weight = previous.weight?.takeIf { MeasureKind.LOAD in enabled && loadRequirement != MeasureRequirement.OPTIONAL },
            durationMs = previous.durationMs?.takeIf { MeasureKind.DURATION in enabled },
            distanceMeters = previous.distanceMeters?.takeIf { MeasureKind.DISTANCE in enabled },
            observedEffort = null,
            loggedAt = editing.workout.finishedAt,
            editedAt = null
        )
        openSetEditor(editing, exerciseId, template, null)
    }

    private suspend fun openSetEditor(
        editing: CompletedWorkoutEditDraft,
        exerciseId: FoundationId,
        set: ExerciseSet,
        originalSetId: FoundationId?
    ) {
        val configuration = configurations?.loggingConfiguration(set.captureConfigurationId)
        if (configuration == null) {
            updateEditError("The set's captured logging configuration is unavailable")
            return
        }
        _state.value = _state.value.copy(
            editDraft = editing.copy(
                setEditor = CompletedSetEditor(
                    exerciseId = exerciseId,
                    originalSetId = originalSetId,
                    draft = SetRowDraft(
                        draftId = set.id,
                        exerciseInstanceId = exerciseId,
                        position = set.position,
                        setKind = set.setKind,
                        captureConfigurationId = set.captureConfigurationId,
                        loggingConfiguration = configuration,
                        reps = set.reps,
                        weight = set.weight,
                        durationMs = set.durationMs,
                        distanceMeters = set.distanceMeters,
                        observedEffort = set.observedEffort
                    )
                ),
                errorMessage = null
            )
        )
    }

    fun updateReps(value: Int?) = updateSetDraft { it.copy(reps = value, inputError = null) }
    fun updateWeight(value: WeightKg?) = updateSetDraft { it.copy(weight = value, weightInput = null, inputError = null) }
    fun updateWeightInput(update: MeasureInputUpdate<WeightKg>) = updateSetDraft {
        it.copy(weight = update.value, weightInput = update.rawValue, inputError = update.errorMessage)
    }
    fun updateDuration(value: Long?) = updateSetDraft { it.copy(durationMs = value, inputError = null) }
    fun updateDistance(value: Double?) = updateSetDraft { it.copy(distanceMeters = value, distanceInput = null, inputError = null) }
    fun updateDistanceInput(update: MeasureInputUpdate<Double>) = updateSetDraft {
        it.copy(distanceMeters = update.value, distanceInput = update.rawValue, inputError = update.errorMessage)
    }
    fun updateEffort(update: MeasureInputUpdate<Effort>) = updateSetDraft {
        it.copy(observedEffort = update.value, effortInput = update.rawValue, inputError = update.errorMessage)
    }

    private fun updateSetDraft(transform: (SetRowDraft) -> SetRowDraft) {
        val editing = _state.value.editDraft ?: return
        val editor = editing.setEditor ?: return
        _state.value = _state.value.copy(
            editDraft = editing.copy(setEditor = editor.copy(draft = transform(editor.draft)), errorMessage = null)
        )
    }

    fun cancelSetEdit() {
        val editing = _state.value.editDraft ?: return
        _state.value = _state.value.copy(editDraft = editing.copy(setEditor = null, errorMessage = null))
    }

    fun applySetEdit(now: Instant = Clock.System.now()) {
        val editing = _state.value.editDraft ?: return
        val editor = editing.setEditor ?: return
        val draft = editor.draft
        if (draft.inputError != null) return
        val validationProbe = ExerciseSet(
            id = draft.draftId,
            exerciseInstanceId = editor.exerciseId,
            position = draft.position,
            setKind = draft.setKind,
            weight = draft.weight,
            reps = draft.reps,
            loggedAt = editing.workout.finishedAt,
            createdAt = now,
            updatedAt = now,
            durationMs = draft.durationMs,
            captureConfigurationId = draft.captureConfigurationId,
            distanceMeters = draft.distanceMeters,
            observedEffort = draft.observedEffort
        )
        validationProbe.validateForLogging(draft.loggingConfiguration)?.let { error ->
            updateSetDraft { it.copy(inlineError = error.message) }
            return
        }
        val exercise = editing.workout.exercises.first { it.id == editor.exerciseId }
        val original = editor.originalSetId?.let { id -> exercise.loggedSets.first { it.id == id } }
        val replacement = validationProbe.copy(
            loggedAt = original?.loggedAt ?: editing.workout.finishedAt,
            createdAt = original?.createdAt ?: now,
            updatedAt = now,
            editedAt = original?.let { now }
        )
        val sets = if (original == null) {
            exercise.loggedSets + replacement
        } else {
            exercise.loggedSets.map { if (it.id == original.id) replacement else it }
        }
        replaceExerciseSets(editing, exercise.id, sets)
    }

    fun requestDeleteSet(setId: FoundationId) {
        val editing = _state.value.editDraft ?: return
        _state.value = _state.value.copy(editDraft = editing.copy(pendingDeleteSetId = setId, errorMessage = null))
    }

    fun cancelDeleteSet() {
        val editing = _state.value.editDraft ?: return
        _state.value = _state.value.copy(editDraft = editing.copy(pendingDeleteSetId = null, errorMessage = null))
    }

    fun confirmDeleteSet() {
        val editing = _state.value.editDraft ?: return
        val setId = editing.pendingDeleteSetId ?: return
        val exercise = editing.workout.exercises.firstOrNull { item -> item.loggedSets.any { it.id == setId } } ?: return
        if (exercise.loggedSets.size <= 1) {
            updateEditError("Keep at least one set; removing the exercise is outside workout corrections")
            return
        }
        replaceExerciseSets(editing, exercise.id, exercise.loggedSets.filterNot { it.id == setId })
    }

    private fun replaceExerciseSets(
        editing: CompletedWorkoutEditDraft,
        exerciseId: FoundationId,
        sets: List<ExerciseSet>
    ) {
        val normalized = sets.mapIndexed { index, set -> set.copy(position = OrderedPosition(index)) }
        val workout = editing.workout.copy(exercises = editing.workout.exercises.map { exercise ->
            if (exercise.id == exerciseId) exercise.copy(loggedSets = normalized) else exercise
        })
        _state.value = _state.value.copy(
            editDraft = editing.copy(
                workout = workout,
                setEditor = null,
                pendingDeleteSetId = null,
                errorMessage = null
            )
        )
    }

    suspend fun saveEditing(): FoundationResult<CompletedWorkout> {
        val useCase = corrections
            ?: return FoundationResult.Failure(FoundationError.Persistence("Workout editing is unavailable"))
        val editing = _state.value.editDraft
            ?: return FoundationResult.Failure(FoundationError.Validation("No completed workout edit is active"))
        if (editing.setEditor != null) {
            return FoundationResult.Failure(FoundationError.Validation("Apply or cancel the open set edit first"))
        }
        _state.value = _state.value.copy(editDraft = editing.copy(isSaving = true, errorMessage = null))
        return when (val result = useCase.save(editing.workout)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(
                    editDraft = editing.copy(isSaving = false, errorMessage = result.error.message)
                )
                result
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(editDraft = null)
                refresh()
                selectWorkout(result.value.id)
                result
            }
        }
    }

    private fun updateEditError(message: String) {
        val editing = _state.value.editDraft
        _state.value = _state.value.copy(
            editDraft = editing?.copy(errorMessage = message, pendingDeleteSetId = null),
            errorMessage = if (editing == null) message else null
        )
    }

    fun requestDeleteSelectedWorkout() {
        val summary = _state.value.selectedSummary ?: return
        _state.value = _state.value.copy(pendingDeleteSummary = summary, errorMessage = null)
    }

    fun cancelDeleteWorkout() {
        _state.value = _state.value.copy(pendingDeleteSummary = null, errorMessage = null)
    }

    suspend fun confirmDeleteWorkout(now: Instant = Clock.System.now()): FoundationResult<Unit> {
        val useCases = routines
            ?: return FoundationResult.Failure(FoundationError.Persistence("Workout deletion is unavailable"))
        val summary = _state.value.pendingDeleteSummary
            ?: return FoundationResult.Failure(FoundationError.Validation("No completed workout selected for deletion"))
        return when (val result = useCases.deleteCompletedWorkout(summary.workoutId, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                refresh()
                _state.value = _state.value.copy(
                    selectedSummary = null,
                    pendingDeleteSummary = null,
                    errorMessage = null
                )
                result
            }
        }
    }
}
