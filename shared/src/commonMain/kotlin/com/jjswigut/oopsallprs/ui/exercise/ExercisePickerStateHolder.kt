package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ExercisePickerState(
    val activeWorkoutId: FoundationId? = null,
    val isOpen: Boolean = false,
    val query: String = "",
    val recentResults: List<ExercisePickerResultRow> = emptyList(),
    val results: List<ExercisePickerResultRow> = emptyList(),
    val customDraft: CustomExerciseDraft = CustomExerciseDraft(),
    val isCreatingCustom: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class ExercisePickerStateHolder(
    private val catalog: ExerciseCatalogUseCases,
    private val activeWorkout: ActiveWorkoutStateHolder
) {
    private val _state = MutableStateFlow(ExercisePickerState())
    val state: StateFlow<ExercisePickerState> = _state

    suspend fun open(activeWorkoutId: FoundationId) {
        val recentRows = catalog.recentResults().map { it.toPickerRow() }
        val recentIds = recentRows.map { it.exerciseCatalogId }.toSet()
        val rows = catalog.defaultResults()
            .filterNot { it.id in recentIds }
            .map { it.toPickerRow() }
        _state.value = ExercisePickerState(
            activeWorkoutId = activeWorkoutId,
            isOpen = true,
            recentResults = recentRows,
            results = rows
        )
    }

    suspend fun search(query: String) {
        val activeWorkoutId = _state.value.activeWorkoutId
        val trimmed = query.trim()
        if (trimmed.isEmpty() && activeWorkoutId != null) {
            open(activeWorkoutId)
            _state.value = _state.value.copy(query = query)
            return
        }
        val rows = catalog.search(trimmed).map { it.toPickerRow() }
        _state.value = _state.value.copy(
            activeWorkoutId = activeWorkoutId,
            query = query,
            recentResults = emptyList(),
            results = rows,
            isCreatingCustom = false,
            errorMessage = null
        )
    }

    fun updateCustomName(name: String) {
        _state.value = _state.value.copy(
            customDraft = _state.value.customDraft.copy(name = name),
            errorMessage = null
        )
    }

    fun updateCustomBodyweight(isBodyweight: Boolean) {
        updateCustomLoggingMode(if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED)
    }

    fun updateCustomLoggingMode(loggingMode: ExerciseLoggingMode) {
        _state.value = _state.value.copy(
            customDraft = _state.value.customDraft.copy(loggingMode = loggingMode),
            errorMessage = null
        )
    }

    fun showCustomCreation() {
        val initialName = _state.value.query.takeIf { it.isNotBlank() }.orEmpty()
        _state.value = _state.value.copy(
            isCreatingCustom = true,
            customDraft = _state.value.customDraft.copy(name = initialName),
            errorMessage = null
        )
    }

    suspend fun select(row: ExercisePickerResultRow): FoundationResult<FoundationId> {
        if (_state.value.isSaving) {
            return foundationFailure(FoundationError.Conflict("Exercise is already being added"))
        }
        _state.value = _state.value.copy(isSaving = true, errorMessage = null)
        return append(row)
    }

    suspend fun createCustom(): FoundationResult<FoundationId> {
        if (_state.value.isSaving) {
            return foundationFailure(FoundationError.Conflict("Exercise is already being added"))
        }
        _state.value = _state.value.copy(isSaving = true, errorMessage = null)
        val draft = _state.value.customDraft
        val created = catalog.createCustomExercise(draft.name, draft.isBodyweight, loggingMode = draft.loggingMode)
        return when (created) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = created.error.message, isSaving = false)
                foundationFailure(created.error)
            }
            is FoundationResult.Success -> {
                val row = created.value.toPickerRow()
                append(row)
            }
        }
    }

    fun dismiss() {
        _state.value = ExercisePickerState()
    }

    private suspend fun append(row: ExercisePickerResultRow): FoundationResult<FoundationId> {
        val workoutId = _state.value.activeWorkoutId
        if (workoutId == null) {
            val error = FoundationError.Validation("No active workout selected")
            _state.value = _state.value.copy(isSaving = false, errorMessage = error.message)
            return foundationFailure(error)
        }
        val result = activeWorkout.addExercise(workoutId, row.toReference())
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(isSaving = false, errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                dismiss()
                foundationSuccess(result.value)
            }
        }
    }
}
