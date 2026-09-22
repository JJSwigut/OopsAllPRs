package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoutineBuilderEditTest {
    @Test
    fun editRoutineDoesNotMutateCompletedWorkoutHistory() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        val routine = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)

        holder.beginEditRoutine(routine.id).successValue()
        holder.updateEditorName("Push Day")
        val draft = assertNotNull(holder.state.value.editorDraft)
        val exercise = draft.exercises.single()
        val set = exercise.plannedSets.single()
        holder.updateEditorSetReps(exercise.draftId, set.draftId, 3)
        holder.updateEditorSetWeight(exercise.draftId, set.draftId, WeightKg(110.0))
        holder.saveEditor(instant(4_000)).successValue()

        val edited = harness.store.routine(routine.id)!!
        val unchangedCompleted = harness.store.completedWorkout(completed.id)!!

        assertEquals("Push Day", edited.name)
        assertEquals(3, edited.exercises.single().plannedSets.single().targetReps)
        assertEquals(WeightKg(110.0), edited.exercises.single().plannedSets.single().targetWeight)
        assertEquals("Bench Press", unchangedCompleted.exercises.single().displayNameSnapshot)
        assertEquals(5, unchangedCompleted.exercises.single().loggedSets.single().reps)
        assertEquals(SetKind.WEIGHTED, unchangedCompleted.exercises.single().loggedSets.single().setKind)
    }

    @Test
    fun removingGroupedExerciseClearsOneExerciseGroupWithoutDroppingSets() = runTest {
        val harness = FoundationHarness()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        holder.beginCreateRoutine()
        holder.updateEditorName("Push")
        holder.addEditorExercise(row("exercise-bench", "Bench Press"))
        holder.addEditorExercise(row("exercise-row", "Seated Row"))
        val draft = assertNotNull(holder.state.value.editorDraft)
        holder.groupEditorExercises(draft.exercises.map { it.draftId })

        val grouped = assertNotNull(holder.state.value.editorDraft)
        holder.removeEditorExercise(grouped.exercises.last().draftId)
        val remaining = assertNotNull(holder.state.value.editorDraft).exercises.single()

        assertNull(remaining.groupId)
        assertEquals(1, remaining.plannedSets.size)
    }

    @Test
    fun ungroupExercisePreservesExercisesAndPlannedSets() = runTest {
        val harness = FoundationHarness()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        holder.beginCreateRoutine()
        holder.updateEditorName("Push")
        holder.addEditorExercise(row("exercise-bench", "Bench Press"))
        holder.addEditorExercise(row("exercise-row", "Seated Row"))
        val draft = assertNotNull(holder.state.value.editorDraft)
        holder.groupEditorExercises(draft.exercises.map { it.draftId })

        val grouped = assertNotNull(holder.state.value.editorDraft)
        holder.ungroupEditorExercise(grouped.exercises.first().draftId)
        val ungrouped = assertNotNull(holder.state.value.editorDraft)

        assertEquals(2, ungrouped.exercises.size)
        assertEquals(listOf(null, null), ungrouped.exercises.map { it.groupId })
        assertEquals(listOf(1, 1), ungrouped.exercises.map { it.plannedSets.size })
    }

    private fun row(id: String, name: String) =
        com.jjswigut.oopsallprs.ui.exercise.ExercisePickerResultRow(
            exerciseCatalogId = com.jjswigut.oopsallprs.domain.model.FoundationId(id),
            displayName = name,
            subtitle = "Back",
            isBodyweight = false,
            isUserCreated = false
        )
}
