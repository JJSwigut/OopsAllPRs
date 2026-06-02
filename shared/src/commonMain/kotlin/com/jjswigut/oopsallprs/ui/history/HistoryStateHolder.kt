package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class HistoryState(
    val completedWorkouts: List<CompletedWorkout> = emptyList(),
    val rows: List<HistoryListItem> = emptyList(),
    val selectedSummary: CompletedWorkoutSummary? = null,
    val pendingDeleteSummary: CompletedWorkoutSummary? = null,
    val errorMessage: String? = null
)

class HistoryStateHolder(
    private val workouts: WorkoutRepository,
    private val progress: ProgressRepository? = null,
    private val routines: RoutineUseCases? = null
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
        _state.value = _state.value.copy(selectedSummary = null, errorMessage = null)
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
