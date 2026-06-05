package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerResultRow

data class RoutineEditorDraft(
    val routineId: FoundationId? = null,
    val name: String = "",
    val sourceCompletedWorkoutId: FoundationId? = null,
    val exercises: List<RoutineExerciseDraft> = emptyList(),
    val exerciseQuery: String = "",
    val exerciseResults: List<ExercisePickerResultRow> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean = name.isNotBlank() && exercises.isNotEmpty() && !isSaving
}

data class RoutineExerciseDraft(
    val draftId: FoundationId,
    val routineExerciseId: FoundationId? = null,
    val exerciseCatalogId: FoundationId,
    val displayName: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode,
    val position: OrderedPosition,
    val groupId: FoundationId? = null,
    val groupPosition: OrderedPosition? = null,
    val groupRounds: Int? = null,
    val rest: RestConfiguration = RestConfiguration.default(),
    val plannedSets: List<RoutineSetDraft> = emptyList()
)

data class RoutineSetDraft(
    val draftId: FoundationId,
    val routineSetId: FoundationId? = null,
    val position: OrderedPosition,
    val setKind: SetKind,
    val targetWeight: WeightKg?,
    val targetReps: Int?,
    val targetDurationMs: Long?
)

fun ExercisePickerResultRow.toRoutineExerciseDraft(position: Int): RoutineExerciseDraft =
    RoutineExerciseDraft(
        draftId = newFoundationId("routine-exercise-draft"),
        exerciseCatalogId = exerciseCatalogId,
        displayName = displayName,
        isBodyweight = isBodyweight,
        loggingMode = loggingMode,
        position = OrderedPosition(position),
        plannedSets = listOf(defaultRoutineSetDraft(loggingMode, 0))
    )

fun ReusableRoutineToEditorDraft(routine: com.jjswigut.oopsallprs.domain.model.ReusableRoutine): RoutineEditorDraft =
    RoutineEditorDraft(
        routineId = routine.id,
        name = routine.name,
        sourceCompletedWorkoutId = routine.sourceCompletedWorkoutId,
        exercises = routine.exercises
            .sortedBy { it.position.value }
            .map { exercise ->
                val loggingMode = exercise.plannedSets.loggingMode()
                RoutineExerciseDraft(
                    draftId = FoundationId("draft-${exercise.id.value}"),
                    routineExerciseId = exercise.id,
                    exerciseCatalogId = exercise.exerciseCatalogId,
                    displayName = exercise.displayNameSnapshot,
                    isBodyweight = loggingMode == ExerciseLoggingMode.BODYWEIGHT || loggingMode == ExerciseLoggingMode.TIMED,
                    loggingMode = loggingMode,
                    position = exercise.position,
                    groupId = exercise.groupId,
                    groupPosition = exercise.groupPosition,
                    groupRounds = exercise.groupRounds,
                    rest = exercise.rest,
                    plannedSets = exercise.plannedSets
                        .sortedBy { it.position.value }
                        .map { set ->
                            RoutineSetDraft(
                                draftId = FoundationId("draft-${set.id.value}"),
                                routineSetId = set.id,
                                position = set.position,
                                setKind = set.setKind,
                                targetWeight = set.targetWeight,
                                targetReps = set.targetReps,
                                targetDurationMs = set.targetDurationMs
                            )
                        }
                        .ifEmpty { listOf(defaultRoutineSetDraft(loggingMode, 0)) }
                )
            }
    )

fun RoutineEditorDraft.toRoutineExercises(): List<RoutineExercise> {
    val placeholderRoutineId = routineId ?: FoundationId("routine-draft")
    return exercises.mapIndexed { exerciseIndex, exercise ->
        val exerciseId = exercise.routineExerciseId ?: newFoundationId("routine-exercise")
        RoutineExercise(
            id = exerciseId,
            routineId = placeholderRoutineId,
            exerciseCatalogId = exercise.exerciseCatalogId,
            displayNameSnapshot = exercise.displayName,
            position = OrderedPosition(exerciseIndex),
            groupId = exercise.groupId,
            groupPosition = exercise.groupPosition,
            groupRounds = exercise.groupRounds,
            rest = exercise.rest,
            plannedSets = exercise.plannedSets.mapIndexed { setIndex, set ->
                RoutineSetTemplate(
                    id = set.routineSetId ?: newFoundationId("routine-set"),
                    routineExerciseId = exerciseId,
                    position = OrderedPosition(setIndex),
                    targetWeight = set.targetWeight,
                    targetReps = set.targetReps,
                    targetDurationMs = set.targetDurationMs,
                    setKind = set.setKind
                )
            }
        )
    }
}

fun defaultRoutineSetDraft(loggingMode: ExerciseLoggingMode, position: Int): RoutineSetDraft {
    val setKind = when (loggingMode) {
        ExerciseLoggingMode.WEIGHTED -> SetKind.WEIGHTED
        ExerciseLoggingMode.BODYWEIGHT -> SetKind.BODYWEIGHT
        ExerciseLoggingMode.TIMED -> SetKind.TIMED
    }
    return RoutineSetDraft(
        draftId = newFoundationId("routine-set-draft"),
        position = OrderedPosition(position),
        setKind = setKind,
        targetWeight = null,
        targetReps = null,
        targetDurationMs = null
    )
}

private fun List<RoutineSetTemplate>.loggingMode(): ExerciseLoggingMode =
    when {
        any { it.setKind == SetKind.TIMED } -> ExerciseLoggingMode.TIMED
        any { it.setKind == SetKind.BODYWEIGHT } -> ExerciseLoggingMode.BODYWEIGHT
        else -> ExerciseLoggingMode.WEIGHTED
    }

fun routineGroupLabel(size: Int): String? =
    when {
        size == 2 -> "Superset"
        size >= 3 -> "Circuit"
        else -> null
    }

fun List<RoutineExerciseDraft>.groupLabelFor(exercise: RoutineExerciseDraft): String? {
    val groupId = exercise.groupId ?: return null
    return routineGroupLabel(count { it.groupId == groupId })
}

fun List<RoutineExerciseDraft>.groupSummaryFor(exercise: RoutineExerciseDraft): String? {
    val label = groupLabelFor(exercise) ?: return null
    val rounds = exercise.groupRounds ?: DEFAULT_GROUP_ROUNDS
    return "$label x$rounds"
}

const val DEFAULT_GROUP_ROUNDS: Int = 3
