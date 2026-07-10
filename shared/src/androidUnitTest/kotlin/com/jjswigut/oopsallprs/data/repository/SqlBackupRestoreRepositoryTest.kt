package com.jjswigut.oopsallprs.data.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlBackupRestoreRepositoryTest {
    @Test
    fun restorePackageIntoEmptyDatabaseHydratesCompletedWorkouts() = runTest {
        val sourceHarness = SqlFoundationStoreTestHarness()
        val sourceRepos = sourceHarness.repositories()
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
    }
}
