package com.jjswigut.oopsallprs.dev

import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.EvidenceLadderUseCase
import com.jjswigut.oopsallprs.domain.usecase.RecentTrainingReviewUseCase
import com.jjswigut.oopsallprs.ui.progress.ProgressStateHolder
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
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
        assertTrue(completed.size >= 19)
        assertTrue(
            completed.maxOf { it.finishedAt }.toEpochMilliseconds() - completed.minOf { it.finishedAt }.toEpochMilliseconds() >=
                8L * 7L * 86_400_000L
        )
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

        val ladder = EvidenceLadderUseCase().project(completed, records, points)
        val triceps = ladder.exerciseProgressions.single { it.exerciseName == "Cable Triceps Extension" }
        val weighted = triceps.achievements.filter { it.series.metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS }
        assertTrue(weighted.any { it.reps == 7 && it.weight?.displayValue(WeightUnit.POUNDS)?.let { pounds -> kotlin.math.abs(pounds - 25.0) < 0.1 } == true })
        assertTrue(weighted.any { it.reps == 8 && it.weight?.displayValue(WeightUnit.POUNDS)?.let { pounds -> kotlin.math.abs(pounds - 20.0) < 0.1 } == true })
        assertEquals(ProgressTrendState.HOLDING_STEADY, triceps.capability.state)
        assertEquals(
            ProgressTrendState.RECENT_RANGE_HIGHER,
            ladder.exerciseProgressions.single { it.exerciseName == "Bench Press" }.capability.state
        )
        assertEquals(
            ProgressTrendState.BUILDING_TREND,
            ladder.exerciseProgressions.single { it.exerciseName == "Deadlift" }.capability.state
        )
        assertEquals(
            ProgressTrendState.STEADY,
            ladder.overallReadings.single { it.reading == EvidenceReading.CONSISTENCY }.state
        )
        assertEquals(
            ProgressTrendState.HOLDING_STEADY,
            ladder.overallReadings.single { it.reading == EvidenceReading.CAPABILITY }.state
        )
        val workCapacity = ladder.overallReadings.single { it.reading == EvidenceReading.WORK_CAPACITY }
        assertTrue(workCapacity.coverage.isMature)
        assertNotNull(workCapacity.changePercent, "Repeated exercises should yield a reading even in mixed routines")
        assertNotNull(workCapacity.comparisonWindow)
        assertTrue(workCapacity.state != ProgressTrendState.BUILDING_TREND)

        val progressState = ProgressStateHolder(harness.store, harness.store, harness.store)
        progressState.refresh()
        assertEquals("Cable Triceps Extension", progressState.state.value.latestPr?.exerciseName)
        assertEquals("8-rep PR", progressState.state.value.latestPr?.kindLabel)
        assertEquals("20 lb x 8", progressState.state.value.latestPr?.valueLabel)
        assertEquals(
            2,
            RecentTrainingReviewUseCase().project(
                workouts = completed,
                personalRecords = records,
                progressReadings = emptyList(),
                now = instant(1_800_000),
                timeZone = TimeZone.UTC
            ).completedWorkoutCount
        )

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

    @Test
    fun automaticProgressDemoOnlyLoadsIntoEmptyDebugHistory() = runTest {
        val emptyHarness = FoundationHarness()
        val loaded = emptyHarness.developerSeeds().loadProgressDemoIfEmpty()
        assertEquals(DeveloperSeedOutcome.LOADED, loaded.outcome)

        val nonemptyHarness = FoundationHarness()
        nonemptyHarness.lifecycle.startEmpty(instant(1_000)).successValue()
        val skipped = nonemptyHarness.developerSeeds().loadProgressDemoIfEmpty()
        assertEquals(DeveloperSeedOutcome.SKIPPED, skipped.outcome)
        assertTrue(nonemptyHarness.store.completedWorkouts().isEmpty())
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
                "Bench Press,Chest,Barbell,Push,Strength,Beginner,Upper\n" +
                "Squat,Legs,Barbell,Squat,Strength,Beginner,Lower\n" +
                "Deadlift,Full Body,Barbell,Hinge,Strength,Intermediate,Lower\n" +
                "Cable Triceps Extension,Triceps,Cable,Isolation,Hypertrophy,Beginner,Upper\n" +
                "Wide-Grip Pull-Up,Back,Bodyweight,Pull,Bodyweight,Intermediate,Upper\n" +
                "Plank,Core,Bodyweight,Static Hold,Hypertrophy,Beginner,Upper\n"
    }
}
