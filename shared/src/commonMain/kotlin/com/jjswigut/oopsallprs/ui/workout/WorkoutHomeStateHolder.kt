package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.history.TemplateListItem
import com.jjswigut.oopsallprs.ui.history.toTemplateListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class WorkoutHomeState(
    val activeSession: ActiveSessionState? = null,
    val templates: List<TemplateListItem> = emptyList(),
    val pendingDeleteTemplate: TemplateListItem? = null,
    val errorMessage: String? = null
)

class WorkoutHomeStateHolder(
    private val lifecycle: WorkoutLifecycleUseCases,
    private val routines: RoutineUseCases? = null
) {
    private val _state = MutableStateFlow(WorkoutHomeState())
    val state: StateFlow<WorkoutHomeState> = _state

    suspend fun hydrate() {
        _state.value = _state.value.copy(
            activeSession = lifecycle.restoreActiveSession().resumable(),
            templates = routines?.listRoutinesByRecentUse().orEmpty().map { it.toTemplateListItem() },
            pendingDeleteTemplate = null
        )
    }

    suspend fun startEmpty(): FoundationResult<ActiveWorkout> {
        val result = lifecycle.startEmpty()
        _state.value = when (result) {
            is FoundationResult.Failure -> _state.value.copy(errorMessage = result.error.message)
            is FoundationResult.Success -> _state.value.copy(
                activeSession = ActiveSessionState(result.value.id, result.value.startedAt, updatedAt = result.value.updatedAt),
                errorMessage = null
            )
        }
        return result
    }

    suspend fun discardActive(): FoundationResult<Unit> {
        val session = _state.value.activeSession.resumable()
            ?: return foundationFailure(FoundationError.Validation("No active workout to discard"))
        val workoutId = session.activeWorkoutId
            ?: return foundationFailure(FoundationError.Validation("No active workout to discard"))
        return when (val result = lifecycle.discard(workoutId)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                hydrate()
                _state.value = _state.value.copy(activeSession = null, errorMessage = null)
                result
            }
        }
    }

    suspend fun launchTemplate(templateId: FoundationId): FoundationResult<ActiveWorkout> {
        if (_state.value.activeSession.resumable() != null) {
            val error = FoundationError.Conflict("An active workout is already in progress")
            return foundationFailure(error)
        }
        val result = lifecycle.startFromRoutine(templateId)
        _state.value = when (result) {
            is FoundationResult.Failure -> _state.value.copy(errorMessage = result.error.message)
            is FoundationResult.Success -> _state.value.copy(
                activeSession = ActiveSessionState(result.value.id, result.value.startedAt, updatedAt = result.value.updatedAt),
                errorMessage = null
            )
        }
        return result
    }

    fun requestTemplateDelete(templateId: FoundationId) {
        val template = _state.value.templates.firstOrNull { it.templateId == templateId } ?: return
        _state.value = _state.value.copy(pendingDeleteTemplate = template, errorMessage = null)
    }

    fun cancelTemplateDelete() {
        _state.value = _state.value.copy(pendingDeleteTemplate = null, errorMessage = null)
    }

    suspend fun confirmTemplateDelete(): FoundationResult<Unit> {
        val useCases = routines
            ?: return foundationFailure(FoundationError.Persistence("Template deletion is unavailable"))
        val template = _state.value.pendingDeleteTemplate
            ?: return foundationFailure(FoundationError.Validation("No template selected for deletion"))
        return when (val result = useCases.deleteRoutine(template.templateId)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                hydrate()
                _state.value = _state.value.copy(pendingDeleteTemplate = null, errorMessage = null)
                result
            }
        }
    }
}

private fun ActiveSessionState?.resumable(): ActiveSessionState? =
    this?.takeIf { it.activeWorkoutId != null }
