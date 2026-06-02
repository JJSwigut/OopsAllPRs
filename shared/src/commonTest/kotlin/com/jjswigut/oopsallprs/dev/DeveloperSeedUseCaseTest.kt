package com.jjswigut.oopsallprs.dev

import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeveloperSeedUseCaseTest {
    @Test
    fun progressDemoCreatesLedgerPrChartAndBodyweightDataOnce() = runTest {
        val harness = FoundationHarness()
        val seeds = harness.developerSeeds()

        val result = seeds.loadProgressDemo()

        assertEquals(DeveloperSeedOutcome.LOADED, result.outcome)
        val completed = harness.store.completedWorkouts()
        val records = harness.store.personalRecords()
        val points = harness.store.progressPoints()
        assertTrue(completed.size >= 5)
        assertTrue(records.size >= 3)
        assertTrue(points.any { it.metric == ProgressMetric.BODYWEIGHT_REPS && it.weight == null })
        assertTrue(points.any { it.metric == ProgressMetric.TIME && it.value >= 60_000.0 })
        assertTrue(
            points
                .filter { it.metric == ProgressMetric.BEST_SET }
                .groupBy { it.exerciseCatalogId }
                .values
                .any { exercisePoints -> exercisePoints.size >= 4 }
        )
        assertTrue(completed.all { workout -> workout.exercises.all { it.loggedSets.isNotEmpty() } })

        val second = seeds.loadProgressDemo()

        assertEquals(DeveloperSeedOutcome.SKIPPED, second.outcome)
        assertEquals(completed.size, harness.store.completedWorkouts().size)
        assertEquals(records.size, harness.store.personalRecords().size)
        assertEquals(points.size, harness.store.progressPoints().size)
    }

    @Test
    fun routineDemoCreatesRestAwareWeightedAndBodyweightRoutinesOnce() = runTest {
        val harness = FoundationHarness()
        val seeds = harness.developerSeeds()

        val result = seeds.loadRoutineDemo()

        assertEquals(DeveloperSeedOutcome.LOADED, result.outcome)
        val routines = harness.routines.listRoutines().filter { it.name.startsWith("Demo:") }
        assertTrue(routines.size >= 2)
        assertTrue(routines.any { routine -> routine.exercises.any { it.rest.durationSeconds >= 180 } })
        assertTrue(routines.any { routine -> routine.exercises.any { exercise -> exercise.plannedSets.any { it.setKind == SetKind.WEIGHTED && it.targetWeight != null } } })
        assertTrue(routines.any { routine -> routine.exercises.any { exercise -> exercise.plannedSets.any { it.setKind == SetKind.BODYWEIGHT && it.targetWeight == null } } })
        assertTrue(routines.any { routine -> routine.exercises.any { exercise -> exercise.plannedSets.any { it.setKind == SetKind.TIMED && it.targetDurationMs != null } } })

        val second = seeds.loadRoutineDemo()

        assertEquals(DeveloperSeedOutcome.SKIPPED, second.outcome)
        assertEquals(routines.size, harness.routines.listRoutines().filter { it.name.startsWith("Demo:") }.size)
    }

    @Test
    fun activeRecoveryDemoCreatesRecoverableWorkoutAndSkipsExistingDemo() = runTest {
        val harness = FoundationHarness()
        val seeds = harness.developerSeeds()

        val result = seeds.loadActiveRecoveryDemo()

        assertEquals(DeveloperSeedOutcome.LOADED, result.outcome)
        val active = harness.lifecycle.currentActiveWorkout()
        assertNotNull(active)
        assertEquals(instant(300_000), active.startedAt)
        assertTrue(active.loggedSets().isNotEmpty())
        assertTrue(active.exercises.flatMap { it.sets }.any { it.loggedAt == null })

        val restored = harness.lifecycle.restoreActiveSession()
        assertEquals(active.id, restored?.activeWorkoutId)

        val second = seeds.loadActiveRecoveryDemo()

        assertEquals(DeveloperSeedOutcome.SKIPPED, second.outcome)
        assertEquals(active.id, harness.lifecycle.currentActiveWorkout()?.id)
    }

    @Test
    fun activeRecoveryDemoDoesNotOverwriteExistingWorkout() = runTest {
        val harness = FoundationHarness()
        val seeds = harness.developerSeeds()
        val existing = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(existing.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(
            activeWorkoutId = existing.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()

        val result = seeds.loadActiveRecoveryDemo()

        assertEquals(DeveloperSeedOutcome.FAILED, result.outcome)
        assertEquals(existing.id, harness.lifecycle.currentActiveWorkout()?.id)
        assertNull(harness.lifecycle.currentActiveWorkout()?.routineSnapshotName)
    }

    private fun FoundationHarness.developerSeeds(): DeveloperSeedUseCase =
        DeveloperSeedUseCase(
            exerciseCatalog = exerciseCatalog,
            workoutLifecycle = lifecycle,
            setLogging = setLogging,
            routines = routines,
            workouts = store,
            progress = store,
            seedCsvProvider = { DEMO_TEST_CSV },
            clock = { instant(1_800_000) }
        )

    private companion object {
        const val DEMO_TEST_CSV: String =
            "Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region\n" +
                "Barbell Bench Press - Medium Grip,Chest,Barbell,Push,Strength,Beginner,Upper\n" +
                "Barbell Squat,Legs,Barbell,Squat,Strength,Beginner,Lower\n" +
                "Barbell Deadlift,Full Body,Barbell,Hinge,Strength,Intermediate,Lower\n" +
                "Wide-Grip Rear Pull-Up,Back,Bodyweight,Pull,Bodyweight,Intermediate,Upper\n" +
                "Plank,Core,Bodyweight,Static Hold,Hypertrophy,Beginner,Upper\n"
    }
}
