package com.jjswigut.oopsallprs.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NativeWorkoutCompletionAccountingTest {
    @Test(timeout = 30_000)
    fun firstCompletionCommitsOneTrialUseWithTheWorkout() = runBlocking {
        NativeBackupDatabaseFixture().use { fixture ->
            val local = fixture.primary
            val activeId = seedPendingWorkout(local)
            assertEquals(0, local.store.loadFullAccess().completedFreeWorkouts)

            val receipt = local.store.finishActiveWorkout(activeId, nativeInstant(2_000)).nativeSuccess()

            assertTrue(receipt.newlyCompleted)
            assertEquals(activeId, receipt.workout.sourceActiveWorkoutId)
            assertEquals(1, receipt.workout.exercises.single().loggedSets.size)
            assertEquals(receipt.workout.id, local.store.completedWorkouts().single().id)
            assertEquals(1, local.store.loadFullAccess().completedFreeWorkouts)
            assertNotNull(local.database.localCompletionQueriesQueries.selectCompletionReceipt(activeId.value).executeAsOneOrNull())
            assertNull(local.store.currentActiveWorkout())
        }
    }

    @Test(timeout = 30_000)
    fun sameSourceRetryKeepsOriginalCompletedIdAndSingleTrialUse() = runBlocking {
        NativeBackupDatabaseFixture().use { fixture ->
            val local = fixture.primary
            val activeId = seedPendingWorkout(local)
            val first = local.store.finishActiveWorkout(activeId, nativeInstant(2_000)).nativeSuccess()
            val accessAfterFirst = local.store.loadFullAccess()

            val retry = local.store.finishActiveWorkout(activeId, nativeInstant(9_000)).nativeSuccess()

            assertFalse(retry.newlyCompleted)
            assertEquals(first.workout, retry.workout)
            assertEquals(listOf(first.workout.id), local.store.completedWorkouts().map { it.id })
            assertEquals(1, accessAfterFirst.completedFreeWorkouts)
            assertEquals(accessAfterFirst, local.store.loadFullAccess())
        }
    }

    @Test(timeout = 30_000)
    fun paidCompletionDoesNotSpendTrialAllowanceOrChargeOnLaterRetry() = runBlocking {
        NativeBackupDatabaseFixture().use { fixture ->
            val local = fixture.primary
            local.store.updateFullAccess {
                it.copy(lifetimeUnlocked = true, completedFreeWorkouts = 3, updatedAt = nativeInstant(500))
            }.nativeSuccess()
            val activeId = seedPendingWorkout(local)

            val first = local.store.finishActiveWorkout(activeId, nativeInstant(2_000)).nativeSuccess()

            assertTrue(first.newlyCompleted)
            assertTrue(local.store.loadFullAccess().lifetimeUnlocked)
            assertEquals(3, local.store.loadFullAccess().completedFreeWorkouts)
            local.store.updateFullAccess { it.copy(lifetimeUnlocked = false) }.nativeSuccess()
            val retry = local.store.finishActiveWorkout(activeId, nativeInstant(3_000)).nativeSuccess()
            assertFalse(retry.newlyCompleted)
            assertEquals(first.workout.id, retry.workout.id)
            assertEquals(3, local.store.loadFullAccess().completedFreeWorkouts)
        }
    }

    @Test(timeout = 30_000)
    fun anotherConnectionRetriesCommittedFinishWithoutAnotherCharge() = runBlocking {
        NativeBackupDatabaseFixture().use { fixture ->
            val local = fixture.primary
            val activeId = seedPendingWorkout(local)
            val peer = fixture.openConnection()
            val first = fixture.onWorker {
                peer.store.finishActiveWorkout(activeId, nativeInstant(2_000)).nativeSuccess()
            }

            val retry = local.store.finishActiveWorkout(activeId, nativeInstant(3_000)).nativeSuccess()

            assertTrue(first.newlyCompleted)
            assertFalse(retry.newlyCompleted)
            assertEquals(first.workout, retry.workout)
            assertEquals(1, local.store.completedWorkouts().size)
            assertEquals(1, peer.store.completedWorkouts().size)
            assertEquals(1, local.store.loadFullAccess().completedFreeWorkouts)
            assertEquals(local.store.loadFullAccess(), peer.store.loadFullAccess())
        }
    }

    @Test(timeout = 30_000)
    fun deletingCompletedHistoryDoesNotRefundAllowance() = runBlocking {
        NativeBackupDatabaseFixture().use { fixture ->
            val local = fixture.primary
            val activeId = seedPendingWorkout(local)
            val first = local.store.finishActiveWorkout(activeId, nativeInstant(2_000)).nativeSuccess()
            val accessAfterFirst = local.store.loadFullAccess()

            local.store.deleteCompletedWorkout(first.workout.id, nativeInstant(3_000)).nativeSuccess()

            assertNull(local.store.completedWorkout(first.workout.id))
            assertEquals(accessAfterFirst, local.store.loadFullAccess())
            assertNotNull(local.database.localCompletionQueriesQueries.selectCompletionReceipt(activeId.value).executeAsOneOrNull())
            val secondId = seedPendingWorkout(local, startedAt = 4_000)
            val second = local.store.finishActiveWorkout(secondId, nativeInstant(5_000)).nativeSuccess()
            assertTrue(second.newlyCompleted)
            assertNotEquals(first.workout.id, second.workout.id)
            assertEquals(2, local.store.loadFullAccess().completedFreeWorkouts)
        }
    }

    @Test(timeout = 30_000)
    fun routineFinishOutcomeDoesNotDoubleChargeTheAtomicCompletion() = runBlocking {
        NativeBackupDatabaseFixture().use { fixture ->
            val local = fixture.primary
            val activeId = seedPendingWorkout(local)
            val routines = RoutineUseCases(local.store, local.store, local.store, preferences = local.store)

            val first = routines.finishWorkout(activeId, nativeInstant(2_000)).nativeSuccess()
            val retry = routines.finishWorkout(activeId, nativeInstant(3_000)).nativeSuccess()

            assertTrue(first.newlyCompleted)
            assertFalse(retry.newlyCompleted)
            assertTrue(first.warnings.isEmpty())
            assertTrue(retry.warnings.isEmpty())
            assertEquals(first.workout, retry.workout)
            assertEquals(1, local.store.loadFullAccess().completedFreeWorkouts)
        }
    }

    @Test(timeout = 30_000)
    fun receiptsSurviveBackupRestoreAndImportedHistoryDoesNotSpendTrialAllowance() = runBlocking {
        NativeBackupDatabaseFixture().use { sourceFixture ->
            val source = sourceFixture.primary
            val sourceId = seedPendingWorkout(source)
            val imported = source.store.finishActiveWorkout(sourceId, nativeInstant(2_000)).nativeSuccess()
            val secondSourceId = seedPendingWorkout(source, startedAt = 3_000)
            source.store.finishActiveWorkout(secondSourceId, nativeInstant(4_000)).nativeSuccess()
            assertEquals(2, source.store.loadFullAccess().completedFreeWorkouts)
            val incoming = source.backup.createPackage().nativeSuccess()
            assertEquals(2, incoming.completedWorkouts.size)
            val encoded = source.backup.encodePackage(incoming).nativeSuccess()

            NativeBackupDatabaseFixture().use { localFixture ->
                val local = localFixture.primary
                val localId = seedPendingWorkout(local)
                val localCompletion = local.store.finishActiveWorkout(localId, nativeInstant(2_000)).nativeSuccess()
                val accessBefore = local.store.loadFullAccess()
                val receiptBefore = assertNotNull(local.database.localCompletionQueriesQueries
                    .selectCompletionReceipt(localId.value).executeAsOneOrNull())
                val decoded = local.backup.decodePackage(encoded).nativeSuccess()

                local.backup.restore(decoded, expectedLocalRevision = local.backup.currentRevision().value).nativeSuccess()

                assertNull(local.store.completedWorkout(localCompletion.workout.id))
                assertEquals(incoming.completedWorkouts.map { it.id }.toSet(), local.store.completedWorkouts().map { it.id.value }.toSet())
                assertEquals(accessBefore, local.store.loadFullAccess())
                assertEquals(receiptBefore, local.database.localCompletionQueriesQueries
                    .selectCompletionReceipt(localId.value).executeAsOneOrNull())
                val importedRetry = local.store.finishActiveWorkout(sourceId, nativeInstant(9_000)).nativeSuccess()
                assertFalse(importedRetry.newlyCompleted)
                assertEquals(imported.workout.id, importedRetry.workout.id)
                assertEquals(1, local.store.loadFullAccess().completedFreeWorkouts)
            }

            NativeBackupDatabaseFixture().use { freshFixture ->
                val fresh = freshFixture.primary
                // Do not initialize access through a read before import: restore owns preserving local accounting.
                fresh.backup.restore(fresh.backup.decodePackage(encoded).nativeSuccess()).nativeSuccess()
                assertEquals(2, fresh.store.completedWorkouts().size)
                assertEquals(0, fresh.store.loadFullAccess().completedFreeWorkouts)
                assertFalse(fresh.store.loadFullAccess().lifetimeUnlocked)
                val retry = fresh.store.finishActiveWorkout(sourceId, nativeInstant(9_000)).nativeSuccess()
                assertFalse(retry.newlyCompleted)
                assertEquals(imported.workout.id, retry.workout.id)
                assertEquals(0, fresh.store.loadFullAccess().completedFreeWorkouts)
            }
        }
    }

    private suspend fun seedPendingWorkout(local: NativeBackupConnection, startedAt: Long = 1_000): FoundationId {
        val exerciseId = FoundationId("native-accounting-bench")
        if (local.store.exercise(exerciseId) == null) {
            local.store.saveUserExercise(ExerciseCatalogItem(
                id = exerciseId, canonicalName = "native accounting bench", displayName = "Native accounting bench",
                muscleGroup = "Chest", equipment = "Barbell", movementPattern = "Push", exerciseType = "Strength",
                experienceLevel = "Intermediate", bodyRegion = "Upper Body", isBodyweight = false,
                isUserCreated = true, createdAt = nativeInstant(100), updatedAt = nativeInstant(100)
            )).nativeSuccess()
        }
        val lifecycle = WorkoutLifecycleUseCases(local.store, local.store, local.store, local.store, local.store)
        val logging = SetLoggingUseCases(local.store, local.store, local.store)
        val active = lifecycle.startEmpty(nativeInstant(startedAt)).nativeSuccess()
        val exercise = logging.addExercise(
            active.id, ExerciseReference(exerciseId, "Native accounting bench", isBodyweight = false), nativeInstant(startedAt + 100)
        ).nativeSuccess()
        logging.confirmSet(
            active.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(50.0), 0, nativeInstant(startedAt + 200)
        ).nativeSuccess()
        assertNotNull(local.store.currentActiveWorkout())
        return active.id
    }
}
