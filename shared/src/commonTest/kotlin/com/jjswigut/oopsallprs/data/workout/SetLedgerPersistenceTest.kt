package com.jjswigut.oopsallprs.data.workout

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SetLedgerPersistenceTest {
    @Test
    fun confirmSetPersistsLoggedTuple() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val logged = harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        val restored = harness.store.activeWorkout(workout.id)
        assertNotNull(restored?.loggedSets()?.single())
        assertEquals(logged.id, restored.loggedSets().single().id)
    }
}
