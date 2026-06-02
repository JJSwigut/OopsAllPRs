package com.jjswigut.oopsallprs.data.session

import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutUxPersistenceTest {
    @Test
    fun storesFocusAndDraftsSeparatelyFromLedgerRows() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val draft = PersistedSetDraft(
            draftId = FoundationId("draft-1"),
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            position = OrderedPosition(0),
            setKind = SetKind.WEIGHTED,
            reps = 8,
            weight = WeightKg(100.0),
            updatedAt = instant(1_200)
        )

        harness.store.saveUxSession(
            ActiveWorkoutUxSession(workout.id, exercise.id, draft.draftId, instant(1_200))
        ).successValue()
        harness.store.saveSetDraft(draft).successValue()

        assertEquals(draft, harness.store.loadSetDrafts(workout.id).single())
        assertEquals(exercise.id, harness.store.loadUxSession(workout.id)?.focusedExerciseInstanceId)
        assertEquals(0, harness.lifecycle.activeWorkout(workout.id)?.loggedSets()?.size)
    }

    @Test
    fun clearWorkoutUxRemovesFocusAndDrafts() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.store.saveUxSession(
            ActiveWorkoutUxSession(workout.id, FoundationId("exercise-1"), FoundationId("draft-1"), instant(1_100))
        ).successValue()

        harness.store.clearWorkoutUx(workout.id, instant(1_200)).successValue()

        assertNull(harness.store.loadUxSession(workout.id))
        assertEquals(emptyList(), harness.store.loadSetDrafts(workout.id))
    }
}
