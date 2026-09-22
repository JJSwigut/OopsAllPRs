package com.jjswigut.oopsallprs.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlBackupSnapshotBoundaryTest {
    @Test
    fun archivedDefinitionAndOverrideRoundTripWithCompletedHistory() = runTest {
        assertArchivedRoundTrip(withHistory = true)
    }

    @Test
    fun unreferencedArchivedDefinitionAndOverrideAreNotSilentlyOmitted() = runTest {
        assertArchivedRoundTrip(withHistory = false)
    }

    private suspend fun assertArchivedRoundTrip(withHistory: Boolean) {
        val source = SqlFoundationStoreTestHarness()
        val destination = SqlFoundationStoreTestHarness()
        try {
            val repos = source.repositories()
            seedBackupExercises(repos)
            if (withHistory) createCompletedMixedWorkout(repos)
            val exerciseId = FoundationId("exercise-bench")
            val userOverride = UserExerciseConfiguration(
                exerciseDefinitionId = exerciseId,
                configuration = LegacyLoggingConfigurations.bodyweight,
                basedOnDefinitionRevision = ExerciseDefinitionRevision(1),
                configuredAt = instant(3_000)
            )
            repos.store.saveUserExerciseConfiguration(userOverride).successValue()
            val sourceBackup = SqlBackupRepository(source.database, repos.store)
            val beforeArchive = sourceBackup.createPackage().successValue()
            repos.store.archiveUserExercise(exerciseId, instant(4_000)).successValue()
            assertFalse(repos.store.all().any { it.id == exerciseId }, "Ordinary catalog readers still hide archives")

            val pkg = sourceBackup.createPackage().successValue()

            val archived = assertNotNull(pkg.exercises.singleOrNull { it.id == exerciseId.value })
            assertEquals(4_000L, archived.archivedAt)
            assertEquals(beforeArchive.exercises.size, pkg.exercises.size)
            assertEquals(beforeArchive.userExerciseConfigurations, pkg.userExerciseConfigurations)
            assertEquals(beforeArchive.completedWorkouts, pkg.completedWorkouts)
            val destinationRepos = destination.repositories()
            val backup = SqlBackupRepository(destination.database, destinationRepos.store)
            val decoded = backup.decodePackage(sourceBackup.encodePackage(pkg).successValue()).successValue()

            val result = backup.restore(decoded).successValue()
            val restored = backup.createPackage().successValue()

            assertEquals(pkg.exercises, restored.exercises)
            assertEquals(pkg.userExerciseConfigurations, restored.userExerciseConfigurations)
            assertEquals(pkg.completedWorkouts, restored.completedWorkouts)
            assertEquals(pkg.lastLocalRevision, restored.lastLocalRevision)
            assertEquals(restored.lastLocalRevision, assertNotNull(result.restoredLocalRevision).value)
            assertEquals(userOverride, destinationRepos.store.userExerciseConfiguration(exerciseId))
            assertFalse(destinationRepos.store.all().any { it.id == exerciseId })
        } finally {
            source.driver.close()
            destination.driver.close()
        }
    }

    @Test
    fun archivedRoutineRetainsExercisesAndTemplatesWithoutReappearingInPicker() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val destination = SqlFoundationStoreTestHarness()
        try {
            val repos = source.repositories()
            seedBackupExercises(repos)
            val completedId = createCompletedMixedWorkout(repos, asCircuit = true)
            val routine = repos.routineUseCases.saveCompletedWorkoutAsRoutine(
                completedId, "Archived circuit", instant(3_000)
            ).successValue()
            val sourceBackup = SqlBackupRepository(source.database, repos.store)
            val beforeArchive = sourceBackup.createPackage().successValue()
            val beforeRoutine = beforeArchive.routines.single()
            assertTrue(beforeRoutine.exercises.all { it.plannedSets.isNotEmpty() })
            repos.routineUseCases.deleteRoutine(routine.id, instant(4_000)).successValue()
            assertTrue(repos.store.routines().isEmpty())

            val pkg = sourceBackup.createPackage().successValue()

            val archived = assertNotNull(pkg.routines.singleOrNull { it.id == routine.id.value })
            assertEquals(4_000L, archived.archivedAt)
            assertEquals(beforeRoutine.exercises, archived.exercises)
            assertEquals(0, pkg.summary.routineCount)
            val destinationRepos = destination.repositories()
            val backup = SqlBackupRepository(destination.database, destinationRepos.store)
            val decoded = backup.decodePackage(sourceBackup.encodePackage(pkg).successValue()).successValue()

            backup.restore(decoded).successValue()
            val restored = backup.createPackage().successValue()

            assertEquals(pkg.routines, restored.routines)
            assertEquals(pkg.lastLocalRevision, restored.lastLocalRevision)
            assertTrue(destinationRepos.store.routines().isEmpty())
        } finally {
            source.driver.close()
            destination.driver.close()
        }
    }

    @Test
    fun alreadyCancelledJobDoesNotReadSnapshot() = runTest {
        val driver = BackupQueryHookDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        val harness = SqlFoundationStoreTestHarness(driver)
        try {
            val repos = harness.repositories()
            val backup = SqlBackupRepository(harness.database, repos.store)
            var queryCount = 0
            driver.beforeQuery = { queryCount += 1 }
            var cancellationObserved = false

            val operation = launch(start = CoroutineStart.UNDISPATCHED) {
                currentCoroutineContext().cancel()
                try {
                    backup.createPackage()
                } catch (_: CancellationException) {
                    cancellationObserved = true
                }
            }
            operation.join()

            assertTrue(cancellationObserved, "Snapshot entry must check its caller's cancelled job")
            assertEquals(0, queryCount)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun alreadyCancelledJobDoesNotBeginRestoreMutation() = runTest {
        assertJobCancellationRollsBack(cancelDuringRestore = false)
    }

    @Test
    fun cancellationOfJobInsidePrecommitHookRollsBackWithoutManualThrow() = runTest {
        assertJobCancellationRollsBack(cancelDuringRestore = true)
    }

    private suspend fun assertJobCancellationRollsBack(cancelDuringRestore: Boolean) =
        kotlinx.coroutines.coroutineScope {
            val harness = SqlFoundationStoreTestHarness()
            try {
                val repos = harness.repositories()
                seedBackupExercises(repos)
                createCompletedMixedWorkout(repos, asCircuit = true)
                val baseline = SqlBackupRepository(harness.database, repos.store)
                val before = baseline.createPackage().successValue()
                val incoming = before.copy(preferences = before.preferences.copy(defaultRestSeconds = 321))
                val sync = SqlBackupSyncRepository(harness.database)
                val oldLink = boundaryLink()
                sync.saveSyncState(oldLink).successValue()
                lateinit var operationContext: CoroutineContext
                var mutationStarted = false
                var cancellationObserved = false
                val backup = SqlBackupRepository(harness.database, repos.store, restoreFaultInjector = {
                    mutationStarted = true
                    if (cancelDuringRestore) operationContext.cancel()
                })

                val operation = launch(start = CoroutineStart.UNDISPATCHED) {
                    operationContext = currentCoroutineContext()
                    if (!cancelDuringRestore) operationContext.cancel()
                    try {
                        backup.restore(incoming, expectedLocalRevision = before.lastLocalRevision)
                    } catch (_: CancellationException) {
                        cancellationObserved = true
                    }
                }
                operation.join()

                assertTrue(cancellationObserved, "Job cancellation must propagate before restore can commit")
                assertEquals(cancelDuringRestore, mutationStarted)
                assertEquals(oldLink, sync.loadSyncState())
                val after = baseline.createPackage().successValue()
                assertEquals(before.lastLocalRevision, after.lastLocalRevision)
                assertEquals(before.completedWorkouts, after.completedWorkouts)
                assertEquals(before.preferences, after.preferences)
            } finally {
                harness.driver.close()
            }
        }

    @Test
    fun secondActiveWorkoutCannotBeSilentlyOmittedFromSafetySnapshot() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            harness.driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
            val repos = harness.repositories()
            val backup = SqlBackupRepository(harness.database, repos.store)
            val incoming = backup.createPackage().successValue()
            val first = repos.lifecycle.startEmpty(instant(1_000)).successValue()
            // The schema permits this without disabling foreign keys or any other constraint.
            harness.database.workoutQueriesQueries.insertActiveWorkout(
                "second-active", 2_000L, null, null, "ACTIVE", 2_000L, 2_000L
            )
            // Do not let a session pointing at the omitted first workout mask the multiplicity bug.
            harness.database.workoutQueriesQueries.clearSessionState()
            assertNull(harness.database.workoutQueriesQueries.selectSessionState().executeAsOneOrNull())
            assertEquals("second-active", repos.store.currentActiveWorkout()?.id?.value)
            val sync = SqlBackupSyncRepository(harness.database)
            val oldLink = boundaryLink()
            sync.saveSyncState(oldLink).successValue()

            assertIs<FoundationResult.Failure>(backup.createPackage())
            assertIs<FoundationResult.Failure>(backup.restore(incoming))

            assertNotNull(harness.database.workoutQueriesQueries.selectActiveWorkout(first.id.value).executeAsOneOrNull())
            assertNotNull(harness.database.workoutQueriesQueries.selectActiveWorkout("second-active").executeAsOneOrNull())
            assertEquals(oldLink, sync.loadSyncState())
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun orphanLoggedSetCannotBeSilentlyOmittedFromSafetySnapshot() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            harness.driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
            val repos = harness.repositories()
            val backup = SqlBackupRepository(harness.database, repos.store)
            val incoming = backup.createPackage().successValue()
            // Set parent IDs have no foreign keys; this row is legal SQL, not ignored corruption.
            harness.database.setQueriesQueries.upsertExerciseSet(
                "orphan-set", "missing-workout", "missing-exercise", 0L, "WEIGHTED",
                50.0, 5L, null, 1_000L, 1_000L, 1_000L, null
            )

            assertIs<FoundationResult.Failure>(backup.createPackage())
            assertIs<FoundationResult.Failure>(backup.restore(incoming))

            assertNotNull(harness.database.setQueriesQueries.selectSetForWorkout("missing-workout", "orphan-set").executeAsOneOrNull())
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun normallyDiscardedWorkoutLeavesNoRecoverableWorkoutToOmit() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            seedBackupExercises(repos)
            val backup = SqlBackupRepository(harness.database, repos.store)
            val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
            val exercise = repos.setLogging.addExercise(
                workout.id, ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
                instant(1_100)
            ).successValue()
            repos.setLogging.confirmSet(
                workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)
            ).successValue()
            repos.lifecycle.discard(workout.id, instant(2_000)).successValue()

            val pkg = backup.createPackage().successValue()

            assertNull(pkg.activeWorkout)
            assertTrue(pkg.completedWorkouts.isEmpty())
            assertNull(harness.database.workoutQueriesQueries.selectActiveWorkout(workout.id.value).executeAsOneOrNull())
            assertTrue(harness.database.setQueriesQueries.selectActiveExercises(workout.id.value).executeAsList().isEmpty())
        } finally {
            harness.driver.close()
        }
    }

    private fun boundaryLink() = BackupSyncState(
        linkedFile = BackupLinkedFile("old.json", "memory://old", "memory"),
        lastLocalRevision = "old-local",
        lastBackupRevision = "old-remote",
        lastOutcome = BackupSyncOutcome.CLEAN,
        updatedAt = instant(1)
    )
}
