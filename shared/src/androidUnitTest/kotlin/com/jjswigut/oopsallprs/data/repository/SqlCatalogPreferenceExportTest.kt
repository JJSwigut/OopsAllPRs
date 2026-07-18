package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.exercise.ExerciseSeedIngestion
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqlCatalogPreferenceExportTest {
    @Test
    fun seedIngestionIsIdempotentAndPreservesUserExercises() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val seed = "Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region\n" +
            "Bench Press,Chest,Barbell,Push,Strength,Beginner,Upper Body\n" +
            "Pull-Up,Back,Bodyweight,Pull,Bodyweight,Intermediate,Upper Body\n"

        ExerciseSeedIngestion(repos.exercises).ingest(seed).successValue()
        repos.exercises.saveUserExercise(userExercise("Bench Press")).successValue()
        ExerciseSeedIngestion(repos.exercises).ingest(seed).successValue()

        val recovered = harness.repositories()
        val all = recovered.exercises.all()
        val bench = all.single { it.canonicalName == "bench press" }
        assertEquals(2, all.size)
        assertTrue(bench.isUserCreated)
        assertEquals("User override", bench.userNotes)
    }

    @Test
    fun weightUnitPreferenceSurvivesRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()

        repos.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()

        val recovered = harness.repositories()
        assertEquals(WeightUnit.KILOGRAMS, recovered.store.weightUnit())
    }

    @Test
    fun weightStepPreferenceSurvivesRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()

        repos.store.setWeightStep(WeightUnit.POUNDS, 2.5).successValue()
        repos.store.setWeightStep(WeightUnit.KILOGRAMS, 1.25).successValue()

        val recovered = harness.repositories()
        assertEquals(2.5, recovered.store.weightStep(WeightUnit.POUNDS))
        assertEquals(1.25, recovered.store.weightStep(WeightUnit.KILOGRAMS))
    }

    @Test
    fun firstSetTimerPreferenceDefaultsOnAndSurvivesRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()

        assertTrue(repos.store.startWorkoutTimerWithFirstSet())
        repos.store.setStartWorkoutTimerWithFirstSet(false).successValue()

        val recovered = harness.repositories()
        assertFalse(recovered.store.startWorkoutTimerWithFirstSet())
    }

    @Test
    fun exportUsesDurableStateAndRecordsSnapshot() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        createCompletedMixedWorkout(repos)

        val recovered = harness.repositories()
        val export = recovered.store.export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()
        val snapshots = harness.database.progressQueriesQueries.selectExportSnapshots().executeAsList()

        assertTrue(export.content.contains("Bench Press"))
        assertTrue(export.content.contains("Pull-Up"))
        assertEquals(2, export.snapshot.rowCount)
        assertEquals(2, export.snapshot.formatVersion)
        assertEquals(1, snapshots.size)
        assertEquals(2L, snapshots.single().format_version)
    }
}

private fun userExercise(name: String): ExerciseCatalogItem =
    ExerciseCatalogItem(
        id = FoundationId("user-bench"),
        canonicalName = "bench press",
        displayName = name,
        muscleGroup = "Chest",
        equipment = "Barbell",
        movementPattern = "Push",
        exerciseType = "Strength",
        experienceLevel = "Beginner",
        bodyRegion = "Upper Body",
        isBodyweight = false,
        isUserCreated = true,
        createdAt = instant(1_000),
        updatedAt = instant(1_000),
        userNotes = "User override"
    )
