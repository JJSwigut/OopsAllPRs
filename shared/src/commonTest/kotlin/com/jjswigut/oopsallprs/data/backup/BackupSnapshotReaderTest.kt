package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupSnapshotReaderTest {
    @Test
    fun snapshotIncludesCompletedLedgerExercisesPreferencesAndProgressContainers() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
        val reader = BackupSnapshotReader(
            workouts = harness.store,
            sessions = harness.store,
            activeUx = harness.store,
            routines = harness.store,
            exercises = harness.store,
            preferences = harness.store,
            progress = harness.store
        )

        val pkg = reader.createPackage(instant(3_000)).successValue()

        assertEquals(1, pkg.summary.workoutCount)
        assertEquals(1, pkg.summary.setCount)
        assertFalse(pkg.summary.hasActiveWorkout)
        assertTrue(pkg.exercises.isNotEmpty())
        assertEquals("POUNDS", pkg.preferences.weightUnit)
        assertEquals(pkg.personalRecords.size + pkg.progressPoints.size, pkg.summary.progressRecordCount)
    }

    @Test
    fun snapshotIncludesCircuitMetadataForRoutinesAndActiveWorkout() = runTest {
        val harness = FoundationHarness()
        val groupId = FoundationId("routine-group-1")
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Circuit day",
            exercises = listOf(
                groupedExercise("routine-exercise-1", "Bench Press", groupId, 0),
                groupedExercise("routine-exercise-2", "Seated Row", groupId, 1)
            ),
            now = instant(1_000)
        ).successValue()
        harness.lifecycle.startFromRoutine(routine.id, instant(2_000)).successValue()
        val reader = BackupSnapshotReader(
            workouts = harness.store,
            sessions = harness.store,
            activeUx = harness.store,
            routines = harness.store,
            exercises = harness.store,
            preferences = harness.store,
            progress = harness.store
        )

        val pkg = reader.createPackage(instant(3_000)).successValue()

        val routineExercises = pkg.routines.single().exercises
        assertEquals(listOf("routine-group-1", "routine-group-1"), routineExercises.map { it.groupId })
        assertEquals(listOf(0, 0), routineExercises.map { it.groupPosition })
        assertEquals(listOf(3, 3), routineExercises.map { it.groupRounds })
        val activeExercises = pkg.activeWorkout!!.exercises
        assertEquals(listOf("routine-group-1", "routine-group-1"), activeExercises.map { it.groupId })
        assertEquals(listOf("Circuit", "Circuit"), activeExercises.map { it.groupLabel })
        assertEquals(listOf(3, 3), activeExercises.map { it.groupRounds })
    }

    private fun groupedExercise(id: String, name: String, groupId: FoundationId, position: Int): RoutineExercise =
        RoutineExercise(
            id = FoundationId(id),
            routineId = FoundationId("pending"),
            exerciseCatalogId = FoundationId("exercise-$position"),
            displayNameSnapshot = name,
            position = OrderedPosition(position),
            groupId = groupId,
            groupPosition = OrderedPosition(0),
            groupRounds = 3,
            plannedSets = listOf(
                RoutineSetTemplate(
                    id = FoundationId("routine-set-$position"),
                    routineExerciseId = FoundationId(id),
                    position = OrderedPosition(0),
                    targetWeight = WeightKg(100.0),
                    targetReps = 5,
                    setKind = SetKind.WEIGHTED
                )
            )
        )
}
