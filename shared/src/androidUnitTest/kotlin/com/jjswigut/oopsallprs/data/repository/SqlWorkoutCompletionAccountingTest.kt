package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.platform.RestAlertScheduleResult
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import com.jjswigut.oopsallprs.ui.workout.WorkoutHomeStateHolder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlWorkoutCompletionAccountingTest {
    @Test
    fun firstCompletionWithoutPersistedAllowanceConsumesExactlyOneWorkout() = runTest {
        withAccountingFixture {
            val sourceId = startLoggedWorkout()
            assertNull(harness.database.workoutQueriesQueries.selectFullAccessState().executeAsOneOrNull())

            val outcome = routines.finishWorkout(sourceId, instant(2_000)).successValue()

            assertOneCompletion(sourceId)
            assertEquals(outcome.workout.id.value, completedRows().single().id)
            assertTrue(outcome.newlyCompleted)
            assertPersistedCount(1)
        }
    }

    @Test
    fun retryingTheSameSourceDoesNotDuplicateHistoryOrChargeAgain() = runTest {
        withAccountingFixture {
            initializeAccess(count = 0)
            val sourceId = startLoggedWorkout()
            val first = routines.finishWorkout(sourceId, instant(2_000)).successValue()

            val retry = routines.finishWorkout(sourceId, instant(3_000)).successValue()

            assertOneCompletion(sourceId)
            assertTrue(first.newlyCompleted)
            assertFalse(retry.newlyCompleted)
            assertEquals(first.workout.id, retry.workout.id)
            assertEquals(first.workout.finishedAt, retry.workout.finishedAt)
            assertEquals(first.workout.id.value, completedRows().single().id)
            assertPersistedCount(1)
        }
    }

    @Test
    fun thrownPostcommitNotificationCancelCannotLeaveSavedWorkoutUncharged() = runTest {
        withAccountingFixture {
            initializeAccess(count = 0)
            val sourceId = startLoggedWorkout()
            var cancelCalls = 0
            var completedRowsAtCancel = -1
            val notifications = object : RestAlertScheduler {
                override fun schedule(
                    restEndsAt: Instant,
                    soundEnabled: Boolean,
                    persistentSurfaceEnabled: Boolean
                ) = RestAlertScheduleResult.UNSUPPORTED

                override fun cancel() {
                    cancelCalls += 1
                    completedRowsAtCancel = completedRows().size
                    throw AccountingNotificationFailure()
                }
            }

            val outcome = routineUseCases(notifications).finishWorkout(sourceId, instant(2_000)).successValue()

            assertEquals(1, cancelCalls)
            assertEquals(1, completedRowsAtCancel, "The injected failure must occur after the ledger save")
            assertOneCompletion(sourceId)
            assertEquals(outcome.workout.id.value, completedRows().single().id)
            assertTrue(outcome.newlyCompleted)
            assertTrue(outcome.warnings.isNotEmpty())
            assertPersistedCount(1)
        }
    }

    @Test
    fun discardingALoggedWorkoutDoesNotConsumeAllowance() = runTest {
        withAccountingFixture {
            initializeAccess(count = 0)
            val sourceId = startLoggedWorkout()

            repos.lifecycle.discard(sourceId, instant(2_000)).successValue()

            assertTrue(completedRows().isEmpty())
            assertNull(repos.workouts.currentActiveWorkout())
            assertPersistedCount(0)
        }
    }

    @Test
    fun emptyCompletionIsRejectedWithoutWritingHistoryOrChargingAllowance() = runTest {
        withAccountingFixture {
            initializeAccess(count = 0)
            val source = repos.lifecycle.startEmpty(instant(1_000)).successValue()

            val result = routines.finishWorkout(source.id, instant(2_000))

            assertIs<FoundationResult.Failure>(result)
            assertTrue(completedRows().isEmpty())
            assertNotNull(repos.workouts.activeWorkout(source.id))
            assertPersistedCount(0)
        }
    }

    @Test
    fun tenthCompletionExhaustsAllowanceAndTheNextStartIsGated() = runTest {
        withAccountingFixture {
            initializeAccess(count = 9)
            assertTrue(access.loadState().canStartWorkout())
            val sourceId = startLoggedWorkout()

            val outcome = routines.finishWorkout(sourceId, instant(2_000)).successValue()

            assertOneCompletion(sourceId)
            assertEquals(outcome.workout.id.value, completedRows().single().id)
            assertTrue(outcome.newlyCompleted)
            assertPersistedCount(10)
            assertFalse(access.loadState().canStartWorkout())
            val home = WorkoutHomeStateHolder(repos.lifecycle, routines, access)
            assertIs<FoundationResult.Failure>(home.startEmpty())
            assertNull(repos.workouts.currentActiveWorkout())
            assertOneCompletion(sourceId)
            assertPersistedCount(10)
        }
    }

    @Test
    fun lifetimeCompletionPreservesPreviouslyConsumedFreeAllowance() = runTest {
        withAccountingFixture {
            initializeAccess(count = 4, lifetime = true)
            val sourceId = startLoggedWorkout()

            val outcome = routines.finishWorkout(sourceId, instant(2_000)).successValue()

            assertOneCompletion(sourceId)
            assertEquals(outcome.workout.id.value, completedRows().single().id)
            assertTrue(outcome.newlyCompleted)
            assertPersistedCount(4)
            assertTrue(access.loadState().hasFullAccess)
            assertTrue(access.loadState().canStartWorkout())
        }
    }
}

private class AccountingNotificationFailure : RuntimeException("Injected rest-notification cancel failure")

private class CompletionAccountingFixture {
    val harness = SqlFoundationStoreTestHarness()
    val repos = harness.repositories()
    val access = FullAccessUseCases(repos.store)
    val routines = routineUseCases()

    fun routineUseCases(notifications: RestAlertScheduler? = null) = RoutineUseCases(
        workouts = repos.workouts,
        routines = repos.routines,
        activeUx = repos.workouts,
        personalRecords = PersonalRecordDerivationUseCase(repos.progress, repos.store),
        preferences = repos.store,
        restNotifications = notifications
    )

    suspend fun initializeAccess(count: Int, lifetime: Boolean = false) {
        repos.store.updateFullAccess {
            FullAccessState(
                completedFreeWorkouts = count,
                lifetimeUnlocked = lifetime,
                updatedAt = instant(100)
            )
        }.successValue()
    }

    suspend fun startLoggedWorkout(): FoundationId {
        seedBackupExercises(repos)
        val source = repos.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = repos.setLogging.addExercise(
            source.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
            instant(1_100)
        ).successValue()
        repos.setLogging.confirmSet(
            source.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)
        ).successValue()
        return source.id
    }

    fun completedRows() = harness.database.routineQueriesQueries.selectCompletedWorkouts().executeAsList()

    fun assertOneCompletion(sourceId: FoundationId) {
        val rows = completedRows()
        assertEquals(1, rows.size, "Exactly one durable completed workout is expected")
        assertEquals(sourceId.value, rows.single().source_active_workout_id)
    }

    fun assertPersistedCount(expected: Int) {
        val row = assertNotNull(harness.database.workoutQueriesQueries.selectFullAccessState().executeAsOneOrNull())
        assertEquals(expected.toLong(), row.completed_free_workouts, "Persisted allowance must match the ledger effect")
    }
}

private suspend fun withAccountingFixture(block: suspend CompletionAccountingFixture.() -> Unit) {
    val fixture = CompletionAccountingFixture()
    try {
        fixture.block()
    } finally {
        fixture.harness.driver.close()
    }
}
