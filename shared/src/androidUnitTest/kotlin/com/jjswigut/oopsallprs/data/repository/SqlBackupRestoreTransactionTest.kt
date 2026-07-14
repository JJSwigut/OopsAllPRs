package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.backup.toDto
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlBackupRestoreTransactionTest {
    @Test
    fun restoreFailureRollsBackExistingRows() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        seedBackupExercises(repos)
        createCompletedMixedWorkout(repos)
        val backup = SqlBackupRepository(
            database = harness.database,
            store = repos.store,
            restoreFaultInjector = { error("simulated restore failure") }
        )
        val existingCount = repos.workouts.completedWorkouts().size
        val validPackage = backup.createPackage().successValue()

        val result = backup.restore(validPackage)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(existingCount, repos.workouts.completedWorkouts().size)
    }

    @Test
    fun completedRestOriginReferenceIsRejectedBeforeClear() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        seedBackupExercises(repos)
        createCompletedMixedWorkout(repos)
        repos.lifecycle.startEmpty(instant(3_000)).successValue()
        val backup = SqlBackupRepository(harness.database, repos.store)
        val valid = backup.createPackage().successValue()
        val completedSetId = valid.completedWorkouts.single().exercises.first().loggedSets.single().id
        val malformed = valid.copy(
            activeSession = requireNotNull(valid.activeSession).copy(restOriginSetId = completedSetId)
        )
        val existingCompletedCount = repos.workouts.completedWorkouts().size

        val result = backup.restore(malformed)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(existingCompletedCount, repos.workouts.completedWorkouts().size)
        assertTrue(repos.workouts.currentActiveWorkout() != null)
    }

    @Test
    fun malformedConfigurationHashIsRejectedBeforeClear() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        seedBackupExercises(repos)
        createCompletedMixedWorkout(repos)
        val backup = SqlBackupRepository(harness.database, repos.store)
        val valid = backup.createPackage().successValue()
        val malformed = valid.copy(
            loggingConfigurations = valid.loggingConfigurations.mapIndexed { index, configuration ->
                if (index == 0) configuration.copy(contentHash = "0".repeat(64)) else configuration
            }
        )
        val existingCount = repos.workouts.completedWorkouts().size

        val result = backup.restore(malformed)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(existingCount, repos.workouts.completedWorkouts().size)
    }

    @Test
    fun immutableConfigurationIdentityConflictIsRejectedBeforeClear() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        seedBackupExercises(repos)
        createCompletedMixedWorkout(repos)
        val backup = SqlBackupRepository(harness.database, repos.store)
        val valid = backup.createPackage().successValue()
        val conflicting = LoggingConfiguration(
            LegacyLoggingConfigurations.weighted.id,
            LegacyLoggingConfigurations.weighted.schemaVersion,
            LegacyLoggingConfigurations.weighted.measures +
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.OPTIONAL)
        ).toDto()
        val malformed = valid.copy(
            loggingConfigurations = valid.loggingConfigurations.map {
                if (it.id == conflicting.id) conflicting else it
            }
        )
        val existingCount = repos.workouts.completedWorkouts().size

        val result = backup.restore(malformed)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(existingCount, repos.workouts.completedWorkouts().size)
    }
}
