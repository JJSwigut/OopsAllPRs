package com.jjswigut.oopsallprs.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.buildCompletedWorkout
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlLocalCompletionAtomicityTest {
    @Test
    fun emptyActiveWorkoutCannotFinishOrConsumeFreeAllowance() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val active = repos.lifecycle.startEmpty(instant(1_000)).successValue()

            val result = repos.store.finishActiveWorkout(active.id, instant(2_000))

            assertIs<FoundationResult.Failure>(result)
            assertNotNull(repos.store.activeWorkout(active.id))
            assertTrue(repos.store.completedWorkouts().isEmpty())
            assertEquals(0, repos.store.loadFullAccess().completedFreeWorkouts)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun missingStateCountsPrecompletionHistoryExactlyOnceAndRetryReturnsOriginalReceipt() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val historical = startLogged(repos)
            val storedHistory = buildCompletedWorkout(
                assertNotNull(repos.store.activeWorkout(historical)), FoundationId("imported-history"), instant(2_000), true
            )
            repos.store.finishWorkout(storedHistory).successValue()
            assertEquals(0, repos.store.loadFullAccess().completedFreeWorkouts, "History persistence is uncharged")
            harness.driver.execute(null, "DELETE FROM full_access_state", 0).value
            val source = startLogged(repos)

            val first = repos.store.finishActiveWorkout(source, instant(3_000)).successValue()
            val retry = SqlFoundationStore(harness.database).finishActiveWorkout(source, instant(4_000)).successValue()

            assertTrue(first.newlyCompleted)
            assertFalse(retry.newlyCompleted)
            assertEquals(first.workout, retry.workout)
            assertEquals(2, repos.store.loadFullAccess().completedFreeWorkouts)
            assertEquals(2, repos.store.completedWorkouts().size)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun failureAfterLedgerReceiptAndAllowanceWritesRollsEverythingBack() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val source = startLogged(repos)
            val active = repos.store.currentActiveWorkout()
            val session = repos.store.load()
            assertNull(harness.database.workoutQueriesQueries.selectFullAccessState().executeAsOneOrNull())
            var reachedBoundary = false
            val failing = SqlFoundationStore(harness.database, completionFaultInjector = {
                reachedBoundary = true
                assertEquals(1L, harness.database.workoutQueriesQueries.selectFullAccessState().executeAsOne().completed_free_workouts)
                assertNotNull(harness.database.localCompletionQueriesQueries.selectCompletionReceipt(source.value).executeAsOneOrNull())
                error("Injected after all completion writes")
            })

            assertIs<FoundationResult.Failure>(failing.finishActiveWorkout(source, instant(2_000)))

            assertTrue(reachedBoundary)
            assertEquals(active, repos.store.currentActiveWorkout())
            assertEquals(session, repos.store.load())
            assertTrue(repos.store.completedWorkouts().isEmpty())
            assertNull(harness.database.workoutQueriesQueries.selectFullAccessState().executeAsOneOrNull())
            assertNull(harness.database.localCompletionQueriesQueries.selectCompletionReceipt(source.value).executeAsOneOrNull())
            assertTrue(repos.store.finishActiveWorkout(source, instant(3_000)).successValue().newlyCompleted)
            assertEquals(1, repos.store.loadFullAccess().completedFreeWorkouts)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun jobCancellationBeforeCommitRollsBackWithoutThrowingFromTheHook() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val source = startLogged(repos)
            lateinit var context: CoroutineContext
            var cancellationObserved = false
            val store = SqlFoundationStore(harness.database, completionFaultInjector = { context.cancel() })

            val completion = launch(start = CoroutineStart.UNDISPATCHED) {
                context = currentCoroutineContext()
                try {
                    store.finishActiveWorkout(source, instant(2_000))
                } catch (_: CancellationException) {
                    cancellationObserved = true
                }
            }
            completion.join()

            assertTrue(cancellationObserved)
            assertNotNull(repos.store.currentActiveWorkout())
            assertTrue(repos.store.completedWorkouts().isEmpty())
            assertNull(harness.database.workoutQueriesQueries.selectFullAccessState().executeAsOneOrNull())
            assertNull(harness.database.localCompletionQueriesQueries.selectCompletionReceipt(source.value).executeAsOneOrNull())
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun receiptsSurviveDeletionAndRestoreOfAnOlderActiveWorkoutWithoutChargingAgain() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val source = startLogged(repos)
            val backup = SqlBackupRepository(harness.database, repos.store)
            val activeBackup = backup.createPackage().successValue()
            val first = repos.store.finishActiveWorkout(source, instant(2_000)).successValue()
            repos.store.deleteCompletedWorkout(first.workout.id, instant(3_000)).successValue()

            val absentRetry = repos.store.finishActiveWorkout(source, instant(4_000))
            assertIs<FoundationError.NotFound>(assertIs<FoundationResult.Failure>(absentRetry).error)
            assertEquals(1, repos.store.loadFullAccess().completedFreeWorkouts)
            assertNotNull(harness.database.localCompletionQueriesQueries.selectCompletionReceipt(source.value).executeAsOneOrNull())
            backup.restore(activeBackup).successValue()
            val restoredCompletion = SqlFoundationStore(harness.database).finishActiveWorkout(source, instant(5_000)).successValue()

            assertTrue(restoredCompletion.newlyCompleted)
            assertEquals(1, repos.store.loadFullAccess().completedFreeWorkouts)
            assertEquals(1, repos.store.completedWorkouts().size)
            assertFalse(repos.store.finishActiveWorkout(source, instant(6_000)).successValue().newlyCompleted)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun deletingLegacyHistoryFreezesMissingAllowanceBeforeRowsDisappear() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val source = startLogged(repos)
            val completion = repos.store.finishActiveWorkout(source, instant(2_000)).successValue()
            harness.driver.execute(null, "DELETE FROM full_access_state", 0).value

            repos.store.deleteCompletedWorkout(completion.workout.id, instant(3_000)).successValue()

            assertEquals(1, SqlFoundationStore(harness.database).loadFullAccess().completedFreeWorkouts)
            assertTrue(repos.store.completedWorkouts().isEmpty())
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun restoreInitializesMissingAllowanceFromLocalHistoryNotImportedHistory() = runTest {
        val sourceHarness = SqlFoundationStoreTestHarness()
        val localHarness = SqlFoundationStoreTestHarness()
        try {
            val source = sourceHarness.repositories()
            repeat(3) {
                source.store.finishActiveWorkout(startLogged(source), instant(2_000)).successValue()
            }
            val incoming = SqlBackupRepository(sourceHarness.database, source.store).createPackage().successValue()
            val local = localHarness.repositories()
            val localId = startLogged(local)
            local.store.finishActiveWorkout(localId, instant(2_000)).successValue()
            localHarness.driver.execute(null, "DELETE FROM full_access_state", 0).value

            SqlBackupRepository(localHarness.database, local.store).restore(incoming).successValue()

            assertEquals(3, local.store.completedWorkouts().size)
            assertEquals(1, local.store.loadFullAccess().completedFreeWorkouts)
            assertNotNull(localHarness.database.localCompletionQueriesQueries.selectCompletionReceipt(localId.value).executeAsOneOrNull())
            incoming.completedWorkouts.forEach {
                assertNull(localHarness.database.localCompletionQueriesQueries.selectCompletionReceipt(it.sourceActiveWorkoutId).executeAsOneOrNull())
            }
        } finally {
            sourceHarness.driver.close()
            localHarness.driver.close()
        }
    }

    @Test
    fun paidCompletionReceiptPreventsAChargeAfterRevocationAndActiveRestore() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            repos.store.updateFullAccess { it.copy(completedFreeWorkouts = 4, lifetimeUnlocked = true) }.successValue()
            val id = startLogged(repos)
            val backup = SqlBackupRepository(harness.database, repos.store)
            val active = backup.createPackage().successValue()
            repos.store.finishActiveWorkout(id, instant(2_000)).successValue()
            repos.store.updateFullAccess { it.copy(lifetimeUnlocked = false) }.successValue()

            backup.restore(active).successValue()
            repos.store.finishActiveWorkout(id, instant(3_000)).successValue()

            assertEquals(4, repos.store.loadFullAccess().completedFreeWorkouts)
            assertFalse(repos.store.loadFullAccess().hasFullAccess)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun ambiguousExistingSourceDoesNotChooseOrOverwriteADuplicate() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            val id = startLogged(repos)
            val first = repos.store.finishActiveWorkout(id, instant(2_000)).successValue()
            harness.database.routineQueriesQueries.insertCompletedWorkout(
                "duplicate-history", id.value, 1_000L, 2_000L, 1_000L, null, 2_000L
            )

            val retry = repos.store.finishActiveWorkout(id, instant(3_000))

            assertIs<FoundationError.Conflict>(assertIs<FoundationResult.Failure>(retry).error)
            assertEquals(setOf(first.workout.id.value, "duplicate-history"),
                harness.database.routineQueriesQueries.selectCompletedWorkouts().executeAsList().map { it.id }.toSet())
            assertEquals(1, repos.store.loadFullAccess().completedFreeWorkouts)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun crossConnectionUpdatesAndDuplicateFinishesCannotClobberCommittedState() = runTest {
        for (scenario in listOf("entitlement-first", "completion-first", "same-source")) {
            val peerCompletes = scenario != "entitlement-first"
            val primaryCompletes = scenario != "completion-first"
            CompletionWalFixture().use { fixture ->
                val repos = fixture.harness.repositories()
                val id = startLogged(repos)
                repos.store.loadFullAccess()
                val peer = SqlFoundationStore(fixture.peerDatabase)
                val beginPeer = CountDownLatch(1)
                val peerDone = CountDownLatch(1)
                val peerResult = AtomicReference<FoundationResult<*>?>()
                val peerError = AtomicReference<Throwable?>()
                // Coroutines run only at the public API boundary on this separate thread.
                val worker = thread(name = "completion-accounting-peer") {
                    try {
                        check(beginPeer.await(5, TimeUnit.SECONDS))
                        runBlocking {
                            peerResult.set(if (peerCompletes) peer.finishActiveWorkout(id, instant(2_000))
                            else peer.updateFullAccess { it.copy(lifetimeUnlocked = true) })
                        }
                    } catch (error: Throwable) {
                        peerError.set(error)
                    } finally {
                        peerDone.countDown()
                    }
                }
                var interleaved = false
                fixture.driver.afterQuery = { sql ->
                    if (!interleaved && sql.contains("FROM full_access_state")) {
                        interleaved = true
                        beginPeer.countDown()
                        check(peerDone.await(5, TimeUnit.SECONDS))
                    }
                }
                try {
                    val firstAttempt = if (primaryCompletes) repos.store.finishActiveWorkout(id, instant(2_000))
                    else repos.store.updateFullAccess { it.copy(lifetimeUnlocked = true) }
                    fixture.driver.afterQuery = null
                    worker.join(5_000)
                    assertFalse(worker.isAlive)
                    assertNull(peerError.get())
                    assertIs<FoundationResult.Success<*>>(peerResult.get())
                    assertTrue(interleaved)
                    assertIs<FoundationResult.Failure>(firstAttempt, "A stale WAL snapshot must not overwrite the peer commit")

                    if (primaryCompletes) {
                        val retry = repos.store.finishActiveWorkout(id, instant(2_000)).successValue()
                        assertEquals(scenario != "same-source", retry.newlyCompleted)
                    } else repos.store.updateFullAccess { it.copy(lifetimeUnlocked = true) }.successValue()

                    val final = repos.store.loadFullAccess()
                    assertEquals(scenario != "same-source", final.hasFullAccess)
                    assertEquals(if (peerCompletes) 1 else 0, final.completedFreeWorkouts)
                    assertEquals(1, repos.store.completedWorkouts().size)
                    assertFalse(peer.finishActiveWorkout(id, instant(3_000)).successValue().newlyCompleted)
                } finally {
                    fixture.driver.afterQuery = null
                    beginPeer.countDown()
                    worker.join(5_000)
                }
            }
        }
    }

    private suspend fun startLogged(repos: SqlRepositoryBundle): FoundationId {
        seedBackupExercises(repos)
        val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = repos.setLogging.addExercise(
            workout.id, ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false), instant(1_100)
        ).successValue()
        repos.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(50.0), 0, instant(1_200)).successValue()
        return workout.id
    }
}

private class CompletionWalFixture : AutoCloseable {
    private val directory = Files.createTempDirectory("local-completion-wal-").toFile()
    private val url = "jdbc:sqlite:${directory.resolve("workouts.db").absolutePath}"
    private val properties = Properties().apply { setProperty("busy_timeout", "1000") }
    val driver = BackupQueryHookDriver(JdbcSqliteDriver(url, properties))
    val harness = SqlFoundationStoreTestHarness(driver)
    private val peerDriver = JdbcSqliteDriver(url, properties)
    val peerDatabase = WorkoutDatabase(peerDriver)

    init {
        driver.executeQuery(null, "PRAGMA journal_mode = WAL", { cursor ->
            check(cursor.next().value)
            check(cursor.getString(0).equals("wal", ignoreCase = true))
            QueryResult.Value(Unit)
        }, 0, null).value
    }

    override fun close() {
        try {
            peerDriver.close()
        } finally {
            try {
                driver.close()
            } finally {
                directory.deleteRecursively()
            }
        }
    }
}
