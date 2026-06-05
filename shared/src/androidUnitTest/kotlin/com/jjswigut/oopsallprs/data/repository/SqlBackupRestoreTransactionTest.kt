package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlBackupRestoreTransactionTest {
    @Test
    fun restoreFailureRollsBackExistingRows() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
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
}
