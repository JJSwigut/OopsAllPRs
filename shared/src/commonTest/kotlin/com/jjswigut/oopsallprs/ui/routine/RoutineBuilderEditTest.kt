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

class RoutineBuilderEditTest {
    @Test
    fun editRoutineDoesNotMutateCompletedWorkoutHistory() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
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
}
