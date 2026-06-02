package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimedSetLoggingUseCaseTest {
    @Test
    fun confirmSetLogsTimedDurationWithoutRepsOrWeight() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.timedReference, instant(1_100)).successValue()

        val set = harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.TIMED,
            reps = 0,
            weight = null,
            position = 0,
            loggedAt = instant(1_200),
            durationMs = 75_000L
        ).successValue()

        assertEquals(SetKind.TIMED, set.setKind)
        assertEquals(75_000L, set.durationMs)
        assertNull(set.reps)
        assertNull(set.weight)
    }

    @Test
    fun timedSetRequiresPositiveDuration() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.timedReference, instant(1_100)).successValue()

        val result = harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.TIMED,
            reps = 0,
            weight = null,
            position = 0,
            loggedAt = instant(1_200),
            durationMs = null
        )

        assertTrue(result is FoundationResult.Failure)
    }
}
