package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackupSnapshotReaderIdentityTest {
    @Test
    fun repeatedRevisionMatchesTheIdentityOfTheReturnedSnapshot() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        harness.workoutWithLoggedWeightedSet()
        val reader = reader(harness)
        val early = reader.createPackage(instant(3_000)).successValue()
        val later = reader.createPackage(instant(9_000)).successValue()

        assertTrue(BackupSnapshotIdentity.isContentRevision(early.lastLocalRevision))
        assertEquals(BackupSnapshotIdentity.revision(early), early.lastLocalRevision)
        assertEquals(early.lastLocalRevision, later.lastLocalRevision)
        assertEquals(early.lastLocalRevision, reader.revision().value)
        assertEquals(reader.revision(), reader.revision())
        assertEquals(early.lastLocalRevision, reader.revision(early.copy(lastLocalRevision = "forged")).value)
    }

    @Test
    fun preferencesSessionsUxAndDraftEditsChangeRevisionWithoutSummaryChanges() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val workout = harness.store.activeWorkout(workoutId)!!
        val reader = reader(harness)
        val before = reader.revision()

        harness.store.setRestTimerSurfaceEnabled(true).successValue()
        val preferenceEdit = reader.revision()
        assertNotEquals(before.value, preferenceEdit.value)
        assertEquals(before.summary, preferenceEdit.summary)

        val session = harness.store.load()!!
        harness.store.save(session.copy(lastOpenedRoute = "profile")).successValue()
        val sessionEdit = reader.revision()
        assertNotEquals(preferenceEdit.value, sessionEdit.value)
        assertEquals(before.summary, sessionEdit.summary)

        harness.store.saveUxSession(ActiveWorkoutUxSession(
            activeWorkoutId = workoutId, focusedExerciseInstanceId = workout.exercises.single().id,
            focusedDraftId = FoundationId("focused-draft"), updatedAt = instant(1_200)
        )).successValue()
        val uxEdit = reader.revision()
        assertNotEquals(sessionEdit.value, uxEdit.value)
        assertEquals(before.summary, uxEdit.summary)

        harness.store.saveSetDraft(PersistedSetDraft(
            draftId = FoundationId("focused-draft"), activeWorkoutId = workoutId,
            exerciseInstanceId = workout.exercises.single().id, position = OrderedPosition(1),
            setKind = SetKind.WEIGHTED, reps = 8, weight = null, updatedAt = instant(1_200)
        )).successValue()
        val draftEdit = reader.revision()
        assertNotEquals(uxEdit.value, draftEdit.value)
        assertEquals(before.summary, draftEdit.summary)
        assertEquals(draftEdit.value, reader.createPackage().successValue().lastLocalRevision)
    }

    @Test
    fun sameCountSameTimestampSetCorrectionChangesReaderRevision() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val reader = reader(harness)
        val before = reader.revision()
        val workout = harness.store.activeWorkout(workoutId)!!
        harness.store.saveActiveWorkout(workout.copy(exercises = workout.exercises.map { exercise ->
            exercise.copy(sets = exercise.sets.map { it.copy(reps = 8) })
        })).successValue()

        val after = reader.revision()
        assertEquals(before.summary, after.summary)
        assertEquals(before.timestamp, after.timestamp)
        assertNotEquals(before.value, after.value)
        assertEquals(after.value, reader.createPackage().successValue().lastLocalRevision)
    }

    @Test
    fun summaryDoesNotReadFullSnapshotPreferencesOrHashContent() = runTest {
        val harness = FoundationHarness()
        var preferenceReads = 0
        val preferences = object : PreferencesRepository by harness.store {
            override suspend fun weightUnit(): WeightUnit {
                preferenceReads++
                return harness.store.weightUnit()
            }
        }
        val reader = BackupSnapshotReader(
            harness.store, harness.store, harness.store, harness.store,
            harness.store, preferences, harness.store
        )

        reader.currentSummary()
        assertEquals(0, preferenceReads)
        reader.revision()
        assertEquals(1, preferenceReads)
    }

    private fun reader(harness: FoundationHarness): BackupSnapshotReader = BackupSnapshotReader(
        harness.store, harness.store, harness.store, harness.store,
        harness.store, harness.store, harness.store
    )
}
