package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActivePrFeedbackUseCaseTest {
    @Test
    fun derivesWeightedFeedbackOnlyWhenSetImprovesPreviousBestForReps() = runTest {
        val harness = FoundationHarness()
        val useCase = ActivePrFeedbackUseCase(harness.store)
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val first = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        val afterFirst = harness.lifecycle.activeWorkout(workout.id)!!

        val feedback = useCase.feedbackFor(afterFirst, afterFirst.exercises.single(), first)

        assertNotNull(feedback)
        assertEquals("New PR: 100 kg x 5", feedback.label)

        val second = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(90.0),
            position = 1,
            loggedAt = instant(1_300)
        ).successValue()
        val afterSecond = harness.lifecycle.activeWorkout(workout.id)!!

        assertNull(useCase.feedbackFor(afterSecond, afterSecond.exercises.single(), second))
    }

    @Test
    fun derivesBodyweightRepsFeedback() = runTest {
        val harness = FoundationHarness()
        val useCase = ActivePrFeedbackUseCase(harness.store)
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_100)).successValue()
        val set = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.BODYWEIGHT,
            reps = 10,
            weight = null,
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        val active = harness.lifecycle.activeWorkout(workout.id)!!

        assertEquals("New PR: 10 reps", useCase.feedbackFor(active, active.exercises.single(), set)?.label)
    }

    @Test
    fun derivesTimedFeedback() = runTest {
        val harness = FoundationHarness()
        val useCase = ActivePrFeedbackUseCase(harness.store)
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
        val active = harness.lifecycle.activeWorkout(workout.id)!!

        assertEquals("New PR: 1:15", useCase.feedbackFor(active, active.exercises.single(), set)?.label)
    }
}
