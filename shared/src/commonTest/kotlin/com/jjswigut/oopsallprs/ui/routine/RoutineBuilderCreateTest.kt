package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerResultRow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoutineBuilderCreateTest {
    @Test
    fun createRoutineFromScratchSavesAndLaunchesPlannedSetsAndRest() = runTest {
        val harness = FoundationHarness()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        holder.beginCreateRoutine()
        holder.updateEditorName(" Push ")
        holder.addEditorExercise(weightedRow())
        holder.addEditorExercise(bodyweightRow())
        val draft = assertNotNull(holder.state.value.editorDraft)
        val bench = draft.exercises[0]
        val pullUp = draft.exercises[1]
        holder.updateEditorSetReps(bench.draftId, bench.plannedSets.single().draftId, 8)
        holder.updateEditorSetWeight(bench.draftId, bench.plannedSets.single().draftId, WeightKg(100.0))
        holder.adjustEditorRest(bench.draftId, 60)
        holder.updateEditorSetReps(pullUp.draftId, pullUp.plannedSets.single().draftId, 10)

        val routine = holder.saveEditor(instant(2_000)).successValue()
        val launched = harness.lifecycle.startFromRoutine(routine.id, instant(3_000)).successValue()

        assertEquals("Push", routine.name)
        assertNull(holder.state.value.editorDraft)
        assertEquals(listOf("Bench Press", "Pull-Up"), launched.exercises.map { it.reference.displayNameSnapshot })
        assertEquals(180, launched.exercises.first().rest.durationSeconds)
        assertEquals(WeightKg(100.0), launched.exercises.first().sets.single().weight)
        assertEquals(SetKind.BODYWEIGHT, launched.exercises.last().sets.single().setKind)
        assertNull(launched.exercises.last().sets.single().weight)
    }

    @Test
    fun blankRoutineTargetsSaveAndLaunchWithPreviousValues() = runTest {
        val harness = FoundationHarness()
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        holder.beginCreateRoutine()
        holder.updateEditorName("Bench shell")
        holder.addEditorExercise(weightedRow())
        val draftSet = assertNotNull(holder.state.value.editorDraft)
            .exercises
            .single()
            .plannedSets
            .single()
        assertNull(draftSet.targetReps)
        assertNull(draftSet.targetWeight)

        val routine = holder.saveEditor(instant(3_000)).successValue()
        val launched = harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        assertEquals(completed.exercises.single().loggedSets.single().reps, launched.exercises.single().sets.single().reps)
        assertEquals(completed.exercises.single().loggedSets.single().weight, launched.exercises.single().sets.single().weight)
        assertNull(routine.exercises.single().plannedSets.single().targetReps)
        assertNull(routine.exercises.single().plannedSets.single().targetWeight)
    }

    @Test
    fun groupedRoutineExercisesSaveAndReopenAsCircuit() = runTest {
        val harness = FoundationHarness()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        holder.beginCreateRoutine()
        holder.updateEditorName("Upper")
        holder.addEditorExercise(weightedRow())
        holder.addEditorExercise(bodyweightRow())
        holder.addEditorExercise(rowRow())

        val initial = assertNotNull(holder.state.value.editorDraft)
        holder.groupEditorExercises(initial.exercises.take(2).map { it.draftId })
        val firstCircuitDraft = assertNotNull(holder.state.value.editorDraft)
        assertEquals("Circuit", firstCircuitDraft.exercises.groupLabelFor(firstCircuitDraft.exercises[0]))
        assertEquals("Circuit", firstCircuitDraft.exercises.groupLabelFor(firstCircuitDraft.exercises[1]))
        assertNull(firstCircuitDraft.exercises.groupLabelFor(firstCircuitDraft.exercises[2]))

        holder.groupEditorExercises(firstCircuitDraft.exercises.map { it.draftId })
        val circuitDraft = assertNotNull(holder.state.value.editorDraft)
        assertEquals(listOf("Circuit", "Circuit", "Circuit"), circuitDraft.exercises.map { circuitDraft.exercises.groupLabelFor(it) })

        val routine = holder.saveEditor(instant(2_000)).successValue()
        holder.beginEditRoutine(routine.id).successValue()
        val reopened = assertNotNull(holder.state.value.editorDraft)

        assertEquals(listOf("Circuit", "Circuit", "Circuit"), reopened.exercises.map { reopened.exercises.groupLabelFor(it) })
        assertEquals(1, reopened.exercises.mapNotNull { it.groupId }.distinct().size)
        assertEquals(listOf("Bench Press", "Pull-Up", "Seated Row"), reopened.exercises.map { it.displayName })
    }

    private fun weightedRow(): ExercisePickerResultRow =
        ExercisePickerResultRow(FoundationId("exercise-bench"), "Bench Press", "Chest", isBodyweight = false, isUserCreated = false)

    private fun bodyweightRow(): ExercisePickerResultRow =
        ExercisePickerResultRow(FoundationId("exercise-pullup"), "Pull-Up", "Back", isBodyweight = true, isUserCreated = false)

    private fun rowRow(): ExercisePickerResultRow =
        ExercisePickerResultRow(FoundationId("exercise-row"), "Seated Row", "Back", isBodyweight = false, isUserCreated = false)
}
