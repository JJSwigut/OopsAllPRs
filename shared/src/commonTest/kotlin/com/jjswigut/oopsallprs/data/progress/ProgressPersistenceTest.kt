package com.jjswigut.oopsallprs.data.progress

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressPersistenceTest {
    @Test
    fun progressDataIsRebuildableFromCompletedWorkouts() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        val derivation = PersonalRecordDerivationUseCase(harness.store)
        derivation.rebuildFrom(listOf(completed))
        derivation.rebuildFrom(listOf(completed))
        assertEquals(3, harness.store.personalRecords().size)
        assertEquals(3, harness.store.progressPoints().size)
    }
}
