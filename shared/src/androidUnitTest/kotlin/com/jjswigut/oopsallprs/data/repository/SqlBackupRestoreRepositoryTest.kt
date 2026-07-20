package com.jjswigut.oopsallprs.data.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SqlBackupRestoreRepositoryTest {
    @Test
    fun restorePackageIntoEmptyDatabaseHydratesCompletedWorkouts() = runTest {
        val sourceHarness = SqlFoundationStoreTestHarness()
        val sourceRepos = sourceHarness.repositories()
        seedBackupExercises(sourceRepos)
        sourceRepos.store.setStartWorkoutTimerWithFirstSet(false).successValue()
        sourceRepos.store.setRestTimerSurfaceEnabled(false).successValue()
        createCompletedMixedWorkout(sourceRepos)
        val sourceBackup = SqlBackupRepository(sourceHarness.database, sourceRepos.store)
        val pkg = sourceBackup.createPackage().successValue()

        val destinationHarness = SqlFoundationStoreTestHarness()
        val destinationRepos = destinationHarness.repositories()
        val destinationBackup = SqlBackupRepository(destinationHarness.database, destinationRepos.store)

        val result = destinationBackup.restore(pkg).successValue()

        assertEquals(1, result.restoredSummary.workoutCount)
        assertEquals(1, destinationRepos.workouts.completedWorkouts().size)
        assertEquals(2, destinationRepos.workouts.completedWorkouts().single().exercises.size)
        assertFalse(destinationRepos.store.startWorkoutTimerWithFirstSet())
        assertFalse(destinationRepos.store.restTimerSurfaceEnabled())
    }
}

internal suspend fun seedBackupExercises(repos: SqlRepositoryBundle) {
    repos.exercises.saveUserExercise(backupExercise("exercise-bench", "Bench Press", false)).successValue()
    repos.exercises.saveUserExercise(backupExercise("exercise-pullup", "Pull-Up", true)).successValue()
}

private fun backupExercise(id: String, name: String, bodyweight: Boolean) =
    com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem(
        id = com.jjswigut.oopsallprs.domain.model.FoundationId(id),
        canonicalName = name.lowercase(),
        displayName = name,
        muscleGroup = if (bodyweight) "Back" else "Chest",
        equipment = if (bodyweight) "Bodyweight" else "Barbell",
        movementPattern = if (bodyweight) "Pull" else "Push",
        exerciseType = if (bodyweight) "Bodyweight" else "Strength",
        experienceLevel = "Intermediate",
        bodyRegion = "Upper Body",
        isBodyweight = bodyweight,
        isUserCreated = true,
        createdAt = instant(100),
        updatedAt = instant(100)
    )
