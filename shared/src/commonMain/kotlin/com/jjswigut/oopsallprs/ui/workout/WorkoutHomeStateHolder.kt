package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.history.TemplateListItem
import com.jjswigut.oopsallprs.ui.history.toTemplateListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val WORKOUT_LIMIT_REACHED_MESSAGE = "You've used your free workouts."

data class WorkoutHomeState(
    val activeSession: ActiveSessionState? = null,
    val templates: List<TemplateListItem> = emptyList(),
    val pendingDeleteTemplate: TemplateListItem? = null,
    val fullAccess: FullAccessState = FullAccessState(),
    val isFullAccessPaywallVisible: Boolean = false,
    val fullAccessMessage: String? = null,
    val errorMessage: String? = null
)

class WorkoutHomeStateHolder(
    private val lifecycle: WorkoutLifecycleUseCases,
    private val routines: RoutineUseCases? = null,
    private val fullAccess: FullAccessUseCases? = null
) {
    private val _state = MutableStateFlow(WorkoutHomeState())
    val state: StateFlow<WorkoutHomeState> = _state

    suspend fun hydrate() {
        val accessState = fullAccess?.loadState() ?: _state.value.fullAccess
        _state.value = _state.value.copy(
            activeSession = lifecycle.restoreActiveSession().resumable(),
            templates = routines?.listRoutinesByRecentUse().orEmpty().map { it.toTemplateListItem() },
            pendingDeleteTemplate = null,
            fullAccess = accessState,
            isFullAccessPaywallVisible = if (accessState.canStartWorkout()) {
                false
            } else {
                _state.value.isFullAccessPaywallVisible
            }
        )
    }

    suspend fun startEmpty(): FoundationResult<ActiveWorkout> {
        requireWorkoutStartAccess()?.let { return it }
        val result = lifecycle.startEmpty()
        _state.value = when (result) {
            is FoundationResult.Failure -> _state.value.copy(errorMessage = result.error.message)
            is FoundationResult.Success -> _state.value.copy(
                activeSession = ActiveSessionState(result.value.id, result.value.startedAt, updatedAt = result.value.updatedAt),
                isFullAccessPaywallVisible = false,
                fullAccessMessage = null,
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
        requireWorkoutStartAccess()?.let { return it }
        val result = lifecycle.startFromRoutine(templateId)
        _state.value = when (result) {
            is FoundationResult.Failure -> _state.value.copy(errorMessage = result.error.message)
            is FoundationResult.Success -> _state.value.copy(
                activeSession = ActiveSessionState(result.value.id, result.value.startedAt, updatedAt = result.value.updatedAt),
                isFullAccessPaywallVisible = false,
                fullAccessMessage = null,
                errorMessage = null
            )
        }
        return result
    }

    suspend fun refreshFullAccess() {
        fullAccess?.let { access ->
            val accessState = access.loadState()
            _state.value = _state.value.copy(
                fullAccess = accessState,
                isFullAccessPaywallVisible = if (accessState.canStartWorkout()) {
                    false
                } else {
                    _state.value.isFullAccessPaywallVisible
                }
            )
        }
    }

    suspend fun requireWorkoutDataWriteAccess(): FoundationResult<Unit> =
        workoutDataWriteAccessError()?.let { foundationFailure(it) } ?: foundationSuccess(Unit)

    fun dismissFullAccessPaywall() {
        _state.value = _state.value.copy(isFullAccessPaywallVisible = false, fullAccessMessage = null)
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

    private suspend fun requireWorkoutStartAccess(): FoundationResult<ActiveWorkout>? {
        return workoutDataWriteAccessError()?.let { foundationFailure(it) }
    }

    private suspend fun workoutDataWriteAccessError(): FoundationError? {
        val access = fullAccess ?: return null
        val accessState = access.loadState()
        _state.value = _state.value.copy(fullAccess = accessState)
        if (accessState.canStartWorkout()) return null
        val message = WORKOUT_LIMIT_REACHED_MESSAGE
        _state.value = _state.value.copy(
            isFullAccessPaywallVisible = true,
            fullAccessMessage = message,
            errorMessage = null
        )
        return FoundationError.Validation(message)
    }
}

private fun ActiveSessionState?.resumable(): ActiveSessionState? =
    this?.takeIf { it.activeWorkoutId != null }
