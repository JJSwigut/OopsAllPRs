package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TimedPersonalRecordDerivationTest {
    @Test
    fun derivesLongestTimedDurationRecordAndTrendPoints() = runTest {
        val harness = FoundationHarness()
        harness.completeTimedWorkout(durationMs = 45_000L, startedAtMs = 1_000)
        val second = harness.completeTimedWorkout(durationMs = 75_000L, startedAtMs = 3_000)

        val records = harness.store.personalRecords().filter { it.recordKind == PersonalRecordKind.TIME }
        val points = harness.store.progressPoints().filter { it.metric == ProgressMetric.TIME }

        assertEquals(1, records.size)
        assertEquals(75_000.0, records.single().value)
        assertEquals(second, records.single().sourceWorkoutId)
        assertEquals(listOf(45_000.0, 75_000.0), points.map { it.value })
    }

    @Test
    fun tiedTimedDurationsDoNotCreateDuplicateRecords() = runTest {
        val harness = FoundationHarness()
        harness.completeTimedWorkout(durationMs = 60_000L, startedAtMs = 1_000)
        harness.completeTimedWorkout(durationMs = 60_000L, startedAtMs = 3_000)

        val records = harness.store.personalRecords().filter { it.recordKind == PersonalRecordKind.TIME }

        assertEquals(1, records.size)
        assertEquals(60_000.0, records.single().value)
    }
}

private suspend fun FoundationHarness.completeTimedWorkout(durationMs: Long, startedAtMs: Long) =
    lifecycle.startEmpty(instant(startedAtMs)).successValue().let { workout ->
        val exercise = setLogging.addExercise(workout.id, timedReference, instant(startedAtMs + 100)).successValue()
        setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.TIMED,
            reps = 0,
            weight = null,
            position = 0,
            loggedAt = instant(startedAtMs + 200),
            durationMs = durationMs
        ).successValue()
        routines.finishWorkout(workout.id, instant(startedAtMs + 1_000)).successValue().id
    }
