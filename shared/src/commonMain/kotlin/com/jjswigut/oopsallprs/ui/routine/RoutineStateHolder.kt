package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerResultRow
import com.jjswigut.oopsallprs.ui.exercise.toPickerRow
import com.jjswigut.oopsallprs.ui.history.TemplateListItem
import com.jjswigut.oopsallprs.ui.history.TemplateSaveDraft
import com.jjswigut.oopsallprs.ui.history.toTemplateListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class RoutineState(
    val routines: List<ReusableRoutine> = emptyList(),
    val templateRows: List<TemplateListItem> = emptyList(),
    val saveDraft: TemplateSaveDraft? = null,
    val editorDraft: RoutineEditorDraft? = null,
    val lastSavedTemplateId: FoundationId? = null,
    val errorMessage: String? = null
)

class RoutineStateHolder(
    private val useCases: RoutineUseCases,
    private val catalog: ExerciseCatalogUseCases? = null
) {
    private val _state = MutableStateFlow(RoutineState())
    val state: StateFlow<RoutineState> = _state

    suspend fun refresh() {
        val routines = useCases.listRoutines().sortedByDescending { it.updatedAt }
        _state.value = _state.value.copy(
            routines = routines,
            templateRows = routines.map { it.toTemplateListItem() },
            errorMessage = null
        )
    }

    suspend fun finishWorkout(
        activeWorkoutId: FoundationId,
        finishedAt: Instant = Clock.System.now()
    ): FoundationResult<CompletedWorkout> =
        useCases.finishWorkout(activeWorkoutId, finishedAt)

    fun beginTemplateSave(completedWorkoutId: FoundationId) {
        _state.value = _state.value.copy(
            saveDraft = TemplateSaveDraft(completedWorkoutId = completedWorkoutId),
            errorMessage = null
        )
    }

    fun updateTemplateName(name: String) {
        val draft = _state.value.saveDraft ?: return
        _state.value = _state.value.copy(saveDraft = draft.copy(name = name, errorMessage = null))
    }

    fun beginCreateRoutine() {
        _state.value = _state.value.copy(
            editorDraft = RoutineEditorDraft(),
            errorMessage = null
        )
    }

    suspend fun beginEditRoutine(routineId: FoundationId): FoundationResult<Unit> {
        val routine = useCases.listRoutines().firstOrNull { it.id == routineId }
            ?: return foundationFailure(FoundationError.NotFound("Routine not found: $routineId"))
        _state.value = _state.value.copy(
            editorDraft = ReusableRoutineToEditorDraft(routine),
            errorMessage = null
        )
        return foundationSuccess(Unit)
    }

    fun cancelEditor() {
        _state.value = _state.value.copy(editorDraft = null, errorMessage = null)
    }

    fun updateEditorName(name: String) {
        updateEditor { it.copy(name = name, errorMessage = null) }
    }

    suspend fun searchEditorExercises(query: String) {
        val draft = _state.value.editorDraft ?: return
        val results = catalog?.search(query).orEmpty().map { it.toPickerRow() }
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exerciseQuery = query,
                exerciseResults = results,
                errorMessage = null
            )
        )
    }

    fun addEditorExercise(row: ExercisePickerResultRow) {
        val draft = _state.value.editorDraft ?: return
        val nextExercise = row.toRoutineExerciseDraft(draft.exercises.size)
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises + nextExercise,
                exerciseQuery = "",
                exerciseResults = emptyList(),
                errorMessage = null
            )
        )
    }

    fun removeEditorExercise(exerciseDraftId: FoundationId) {
        val draft = _state.value.editorDraft ?: return
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises
                    .filterNot { it.draftId == exerciseDraftId }
                    .mapIndexed { index, exercise -> exercise.copy(position = com.jjswigut.oopsallprs.domain.model.OrderedPosition(index)) }
                    .withoutInvalidGroups(),
                errorMessage = null
            )
        )
    }

    fun groupEditorExercises(exerciseDraftIds: List<FoundationId>) {
        val draft = _state.value.editorDraft ?: return
        if (exerciseDraftIds.size < 2) {
            _state.value = _state.value.copy(editorDraft = draft.copy(errorMessage = "Select at least two adjacent exercises"))
            return
        }
        val positionsById = draft.exercises.mapIndexed { index, exercise -> exercise.draftId to index }.toMap()
        val positions = exerciseDraftIds.mapNotNull { positionsById[it] }.sorted()
        if (positions.size != exerciseDraftIds.size || positions != (positions.first()..positions.last()).toList()) {
            _state.value = _state.value.copy(editorDraft = draft.copy(errorMessage = "Only adjacent exercises can be grouped"))
            return
        }
        val groupId = newFoundationId("routine-group")
        val groupPosition = com.jjswigut.oopsallprs.domain.model.OrderedPosition(positions.first())
        val groupRounds = positions.mapNotNull { draft.exercises[it].groupRounds }.firstOrNull() ?: DEFAULT_GROUP_ROUNDS
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises
                    .mapIndexed { index, exercise ->
                        if (index in positions) {
                            exercise.copy(groupId = groupId, groupPosition = groupPosition, groupRounds = groupRounds)
                        } else {
                            exercise
                        }
                    }
                    .withoutInvalidGroups(),
                errorMessage = null
            )
        )
    }

    fun groupEditorExerciseWithNext(exerciseDraftId: FoundationId) {
        val draft = _state.value.editorDraft ?: return
        val index = draft.exercises.indexOfFirst { it.draftId == exerciseDraftId }
        if (index == -1 || index == draft.exercises.lastIndex) {
            _state.value = _state.value.copy(editorDraft = draft.copy(errorMessage = "Choose an exercise with a next exercise"))
            return
        }
        val existingGroupId = draft.exercises[index].groupId
        val groupedIds = if (existingGroupId != null) {
            draft.exercises.filter { it.groupId == existingGroupId }.map { it.draftId } + draft.exercises[index + 1].draftId
        } else {
            listOf(draft.exercises[index].draftId, draft.exercises[index + 1].draftId)
        }
        groupEditorExercises(groupedIds.distinct())
    }

    fun ungroupEditorExercise(exerciseDraftId: FoundationId) {
        val draft = _state.value.editorDraft ?: return
        val groupId = draft.exercises.firstOrNull { it.draftId == exerciseDraftId }?.groupId ?: return
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises.map { exercise ->
                    if (exercise.groupId == groupId) {
                        exercise.copy(groupId = null, groupPosition = null, groupRounds = null)
                    } else {
                        exercise
                    }
                },
                errorMessage = null
            )
        )
    }

    fun adjustEditorGroupRounds(exerciseDraftId: FoundationId, deltaRounds: Int) {
        val draft = _state.value.editorDraft ?: return
        val groupId = draft.exercises.firstOrNull { it.draftId == exerciseDraftId }?.groupId ?: return
        val current = draft.exercises.firstOrNull { it.groupId == groupId }?.groupRounds ?: DEFAULT_GROUP_ROUNDS
        val next = (current + deltaRounds).coerceIn(1, 12)
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises.map { exercise ->
                    if (exercise.groupId == groupId) {
                        exercise.copy(groupRounds = next)
                    } else {
                        exercise
                    }
                },
                errorMessage = null
            )
        )
    }

    fun addEditorSet(exerciseDraftId: FoundationId) {
        val draft = _state.value.editorDraft ?: return
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises.map { exercise ->
                    if (exercise.draftId == exerciseDraftId) {
                        exercise.copy(plannedSets = exercise.plannedSets + defaultRoutineSetDraft(exercise.loggingMode, exercise.plannedSets.size))
                    } else {
                        exercise
                    }
                }
            )
        )
    }

    fun removeEditorSet(exerciseDraftId: FoundationId, setDraftId: FoundationId) {
        val draft = _state.value.editorDraft ?: return
        _state.value = _state.value.copy(
            editorDraft = draft.copy(
                exercises = draft.exercises.map { exercise ->
                    if (exercise.draftId == exerciseDraftId && exercise.plannedSets.size > 1) {
                        exercise.copy(
                            plannedSets = exercise.plannedSets
                                .filterNot { it.draftId == setDraftId }
                                .mapIndexed { index, set -> set.copy(position = com.jjswigut.oopsallprs.domain.model.OrderedPosition(index)) }
                        )
                    } else {
                        exercise
                    }
                }
            )
        )
    }

    fun updateEditorSetReps(exerciseDraftId: FoundationId, setDraftId: FoundationId, reps: Int?) {
        updateEditorSet(exerciseDraftId, setDraftId) { it.copy(targetReps = reps) }
    }

    fun updateEditorSetWeight(exerciseDraftId: FoundationId, setDraftId: FoundationId, weight: WeightKg?) {
        updateEditorSet(exerciseDraftId, setDraftId) { it.copy(targetWeight = weight) }
    }

    fun updateEditorSetDuration(exerciseDraftId: FoundationId, setDraftId: FoundationId, durationMs: Long?) {
        updateEditorSet(exerciseDraftId, setDraftId) { it.copy(targetDurationMs = durationMs) }
    }

    fun updateEditorSetKind(exerciseDraftId: FoundationId, setDraftId: FoundationId, setKind: SetKind) {
        updateEditorSet(exerciseDraftId, setDraftId) {
            it.copy(
                setKind = setKind,
                targetWeight = it.targetWeight.takeIf { _ -> setKind == SetKind.WEIGHTED },
                targetReps = it.targetReps.takeIf { setKind != SetKind.TIMED },
                targetDurationMs = it.targetDurationMs.takeIf { setKind == SetKind.TIMED }
            )
        }
    }

    fun adjustEditorRest(exerciseDraftId: FoundationId, deltaSeconds: Int) {
        updateEditorExercise(exerciseDraftId) { exercise ->
            val base = if (exercise.rest.durationSeconds > 0) exercise.rest.durationSeconds else RestConfiguration.DEFAULT_SECONDS
            exercise.copy(rest = exercise.rest.copy(durationSeconds = (base + deltaSeconds).coerceAtLeast(0), autoStart = true))
        }
    }

    fun toggleEditorRest(exerciseDraftId: FoundationId) {
        updateEditorExercise(exerciseDraftId) { exercise ->
            exercise.copy(rest = if (exercise.rest.isEnabled) RestConfiguration.disabled() else RestConfiguration.default())
        }
    }

    suspend fun saveEditor(now: Instant = Clock.System.now()): FoundationResult<ReusableRoutine> {
        val draft = _state.value.editorDraft
            ?: return foundationFailure(FoundationError.Validation("No routine draft open"))
        validateEditor(draft)?.let { error ->
            _state.value = _state.value.copy(editorDraft = draft.copy(errorMessage = error.message))
            return foundationFailure(error)
        }
        _state.value = _state.value.copy(editorDraft = draft.copy(isSaving = true, errorMessage = null))
        return when (
            val result = useCases.saveRoutine(
                routineId = draft.routineId,
                name = draft.name,
                exercises = draft.toRoutineExercises(),
                sourceCompletedWorkoutId = draft.sourceCompletedWorkoutId,
                now = now
            )
        ) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(editorDraft = draft.copy(isSaving = false, errorMessage = result.error.message))
                result
            }
            is FoundationResult.Success -> {
                refresh()
                _state.value = _state.value.copy(
                    editorDraft = null,
                    lastSavedTemplateId = result.value.id,
                    errorMessage = null
                )
                result
            }
        }
    }

    suspend fun saveTemplate(now: Instant = Clock.System.now()): FoundationResult<ReusableRoutine> {
        val draft = _state.value.saveDraft
            ?: return foundationFailure(FoundationError.Validation("No completed workout selected"))
        if (draft.name.isBlank()) {
            val error = FoundationError.Validation("Template name is required")
            _state.value = _state.value.copy(saveDraft = draft.copy(errorMessage = error.message))
            return foundationFailure(error)
        }

        _state.value = _state.value.copy(saveDraft = draft.copy(isSaving = true, errorMessage = null))
        return when (val result = useCases.saveCompletedWorkoutAsRoutine(draft.completedWorkoutId, draft.name, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(saveDraft = draft.copy(isSaving = false, errorMessage = result.error.message))
                result
            }
            is FoundationResult.Success -> {
                refresh()
                _state.value = _state.value.copy(
                    saveDraft = null,
                    lastSavedTemplateId = result.value.id,
                    errorMessage = null
                )
                result
            }
        }
    }

    private fun updateEditor(transform: (RoutineEditorDraft) -> RoutineEditorDraft) {
        val draft = _state.value.editorDraft ?: return
        _state.value = _state.value.copy(editorDraft = transform(draft))
    }

    private fun updateEditorExercise(
        exerciseDraftId: FoundationId,
        transform: (RoutineExerciseDraft) -> RoutineExerciseDraft
    ) {
        updateEditor { draft ->
            draft.copy(exercises = draft.exercises.map { exercise ->
                if (exercise.draftId == exerciseDraftId) transform(exercise) else exercise
            })
        }
    }

    private fun updateEditorSet(
        exerciseDraftId: FoundationId,
        setDraftId: FoundationId,
        transform: (RoutineSetDraft) -> RoutineSetDraft
    ) {
        updateEditorExercise(exerciseDraftId) { exercise ->
            exercise.copy(plannedSets = exercise.plannedSets.map { set ->
                if (set.draftId == setDraftId) transform(set) else set
            })
        }
    }

    private fun validateEditor(draft: RoutineEditorDraft): FoundationError? =
        when {
            draft.name.trim().isBlank() -> FoundationError.Validation("Routine name is required")
            draft.exercises.isEmpty() -> FoundationError.Validation("Add at least one exercise")
            draft.exercises.hasInvalidGroups() -> FoundationError.Validation("Groups must contain adjacent exercises")
            else -> draft.exercises
                .asSequence()
                .flatMap { exercise -> exercise.plannedSets.asSequence() }
                .mapNotNull { set ->
                    when {
                        set.targetReps != null && set.targetReps <= 0 -> FoundationError.Validation("Planned reps must be positive")
                        set.targetDurationMs != null && set.targetDurationMs <= 0L -> FoundationError.Validation("Planned time must be positive")
                        set.targetWeight != null && set.targetWeight.value < 0.0 -> FoundationError.Validation("Weights cannot be negative")
                        set.setKind == SetKind.TIMED && (set.targetReps != null || set.targetWeight != null) -> FoundationError.Validation("Timed sets cannot have reps or weight targets")
                        set.setKind != SetKind.TIMED && set.targetDurationMs != null -> FoundationError.Validation("Only timed sets can have time targets")
                        else -> null
                    }
                }
                .firstOrNull()
        }
}

private fun List<RoutineExerciseDraft>.withoutInvalidGroups(): List<RoutineExerciseDraft> {
    val validGroupIds = groupingBy { it.groupId }
        .eachCount()
        .filterKeys { it != null }
        .filterValues { it >= 2 }
        .keys
    return map { exercise ->
        if (exercise.groupId in validGroupIds && exercise.groupPosition != null) {
            exercise
        } else {
            exercise.copy(groupId = null, groupPosition = null, groupRounds = null)
        }
    }.renumberGroupPositions()
}

private fun List<RoutineExerciseDraft>.renumberGroupPositions(): List<RoutineExerciseDraft> {
    val firstPositionByGroup = mapNotNull { exercise ->
        exercise.groupId?.let { it to exercise.position.value }
    }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, positions) -> positions.minOrNull() ?: 0 }
    return map { exercise ->
        val groupId = exercise.groupId
        if (groupId == null) {
            exercise.copy(groupPosition = null, groupRounds = null)
        } else {
            exercise.copy(groupPosition = com.jjswigut.oopsallprs.domain.model.OrderedPosition(firstPositionByGroup[groupId] ?: exercise.position.value))
        }
    }
}

private fun List<RoutineExerciseDraft>.hasInvalidGroups(): Boolean =
    mapIndexed { index, exercise -> index to exercise }
        .groupBy { it.second.groupId }
        .filterKeys { it != null }
        .values
        .any { entries ->
            if (entries.size < 2) return@any true
            val positions = entries.map { it.first }.sorted()
            positions != (positions.first()..positions.last()).toList()
        }
