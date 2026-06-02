package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ExerciseManagementState(
    val isOpen: Boolean = false,
    val query: String = "",
    val rows: List<ManagedExerciseRow> = emptyList(),
    val draft: ExerciseEditDraft? = null,
    val pendingArchive: ManagedExerciseRow? = null,
    val errorMessage: String? = null
)

class ExerciseManagementStateHolder(
    private val catalog: ExerciseCatalogUseCases
) {
    private val _state = MutableStateFlow(ExerciseManagementState())
    val state: StateFlow<ExerciseManagementState> = _state

    suspend fun open() {
        val rows = catalog.userCreatedExercises().map { it.toManagedExerciseRow() }
        _state.value = ExerciseManagementState(isOpen = true, rows = rows)
    }

    suspend fun refresh() {
        if (!_state.value.isOpen) return
        val query = _state.value.query
        val rows = if (query.isBlank()) {
            catalog.userCreatedExercises()
        } else {
            catalog.search(query).filter { it.isUserCreated }
        }.map { it.toManagedExerciseRow() }
        _state.value = _state.value.copy(rows = rows, errorMessage = null)
    }

    suspend fun search(query: String) {
        val rows = if (query.isBlank()) {
            catalog.userCreatedExercises()
        } else {
            catalog.search(query).filter { it.isUserCreated }
        }.map { it.toManagedExerciseRow() }
        _state.value = _state.value.copy(query = query, rows = rows, errorMessage = null)
    }

    fun close() {
        _state.value = ExerciseManagementState()
    }

    fun beginCreate() {
        _state.value = _state.value.copy(draft = ExerciseEditDraft(), pendingArchive = null, errorMessage = null)
    }

    fun beginEdit(row: ManagedExerciseRow) {
        if (!row.canEdit) {
            _state.value = _state.value.copy(errorMessage = "Seeded exercises cannot be edited")
            return
        }
        _state.value = _state.value.copy(draft = row.toEditDraft(), pendingArchive = null, errorMessage = null)
    }

    fun updateDraftName(name: String) {
        val draft = _state.value.draft ?: return
        _state.value = _state.value.copy(draft = draft.copy(name = name, errorMessage = null), errorMessage = null)
    }

    fun updateDraftBodyweight(isBodyweight: Boolean) {
        updateDraftLoggingMode(if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED)
    }

    fun updateDraftLoggingMode(loggingMode: ExerciseLoggingMode) {
        val draft = _state.value.draft ?: return
        _state.value = _state.value.copy(draft = draft.copy(loggingMode = loggingMode, errorMessage = null), errorMessage = null)
    }

    fun cancelDraft() {
        _state.value = _state.value.copy(draft = null, errorMessage = null)
    }

    suspend fun saveDraft(now: Instant = Clock.System.now()): FoundationResult<FoundationId> {
        val draft = _state.value.draft
            ?: return foundationFailure(FoundationError.Validation("No exercise draft open"))
        if (draft.name.isBlank()) {
            val error = FoundationError.Validation("Exercise name cannot be blank")
            _state.value = _state.value.copy(draft = draft.copy(errorMessage = error.message))
            return foundationFailure(error)
        }
        _state.value = _state.value.copy(draft = draft.copy(isSaving = true, errorMessage = null), errorMessage = null)
        val result = if (draft.exerciseCatalogId == null) {
            catalog.createCustomExercise(draft.name, draft.isBodyweight, now = now, loggingMode = draft.loggingMode)
        } else {
            catalog.updateCustomExercise(draft.exerciseCatalogId, draft.name, draft.isBodyweight, now = now, loggingMode = draft.loggingMode)
        }
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(draft = draft.copy(isSaving = false, errorMessage = result.error.message))
                foundationFailure(result.error)
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(draft = null, errorMessage = null)
                refresh()
                foundationSuccess(result.value.id)
            }
        }
    }

    fun requestArchive(row: ManagedExerciseRow) {
        if (!row.canArchive) {
            _state.value = _state.value.copy(errorMessage = "Seeded exercises cannot be archived")
            return
        }
        _state.value = _state.value.copy(pendingArchive = row, draft = null, errorMessage = null)
    }

    fun cancelArchive() {
        _state.value = _state.value.copy(pendingArchive = null, errorMessage = null)
    }

    suspend fun confirmArchive(now: Instant = Clock.System.now()): FoundationResult<Unit> {
        val row = _state.value.pendingArchive
            ?: return foundationFailure(FoundationError.Validation("No exercise selected"))
        return when (val result = catalog.archiveCustomExercise(row.exerciseCatalogId, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(pendingArchive = null, errorMessage = null)
                refresh()
                foundationSuccess(Unit)
            }
        }
    }
}
