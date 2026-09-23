package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.history.mixedCompletedWorkout
import com.jjswigut.oopsallprs.ui.history.timedCompletedWorkout
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressEvidenceTest {
    @Test
    fun opensSourceWorkoutAndSetEvidenceForRecord() = runTest {
        val harness = FoundationHarness()
        val completed = harness.store.finishWorkout(mixedCompletedWorkout()).successValue()
        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))
        val record = harness.store.personalRecords().first {
            it.recordKind == PersonalRecordKind.WEIGHT_FOR_REPS && it.sourceSetId == FoundationId("set-weighted")
        }
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()

        holder.openEvidence(record.id)

        val evidence = assertNotNull(holder.state.value.selectedEvidence)
        assertTrue(evidence.isAvailable)
        assertEquals("Bench Press", evidence.sourceExerciseName)
        assertEquals("Set 1: 5 reps • 220.46 lb", evidence.sourceSetLabel)
        assertEquals(completed.id, evidence.sourceWorkoutId)
        assertEquals(record.achievedAt.shortDateLabel(), evidence.achievedDateLabel)
    }

    @Test
    fun missingEvidenceDoesNotFabricateSourceData() = runTest {
        val harness = FoundationHarness()
        val missingRecord = progressRecord(
            id = FoundationId("pr-missing-source"),
            sourceWorkoutId = FoundationId("missing-workout"),
            sourceSetId = FoundationId("missing-set")
        )
        harness.store.replaceRecords(listOf(missingRecord), emptyList()).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()

        holder.openEvidence(missingRecord.id)

        val evidence = assertNotNull(holder.state.value.selectedEvidence)
        assertFalse(evidence.isAvailable)
        assertNull(evidence.sourceExerciseName)
        assertNull(evidence.sourceSetLabel)
        assertTrue(evidence.message.contains("no longer available"))
    }

    @Test
    fun timeRecordEvidenceUsesTimedSourceSetLabel() = runTest {
        val harness = FoundationHarness()
        val completed = harness.store.finishWorkout(timedCompletedWorkout()).successValue()
        val record = progressRecord(
            id = FoundationId("pr-time"),
            exerciseCatalogId = FoundationId("exercise-plank"),
            recordKind = PersonalRecordKind.TIME,
            reps = null,
            weight = null,
            value = 75_000.0,
            sourceWorkoutId = completed.id,
            sourceSetId = FoundationId("set-plank")
        )
        harness.store.replaceRecords(listOf(record), emptyList()).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()

        holder.openEvidence(record.id)

        val evidence = assertNotNull(holder.state.value.selectedEvidence)
        assertEquals("1:15", evidence.recordValueLabel)
        assertEquals("Set 1: 1:15", evidence.sourceSetLabel)
    }
}
