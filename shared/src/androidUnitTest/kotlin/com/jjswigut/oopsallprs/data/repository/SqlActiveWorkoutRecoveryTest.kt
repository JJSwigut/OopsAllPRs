package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlActiveWorkoutRecoveryTest {
    @Test
    fun activeWorkoutSessionUxAndDraftsRecoverAfterRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = repos.setLogging.addExercise(
            workout.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
            instant(1_100)
        ).successValue()
        repos.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        val draft = PersistedSetDraft(
            draftId = FoundationId("draft-1"),
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            position = OrderedPosition(1),
            setKind = SetKind.WEIGHTED,
            reps = 6,
            weight = WeightKg(102.5),
            updatedAt = instant(1_300)
        )
        repos.workouts.saveSetDraft(draft).successValue()
        repos.workouts.saveUxSession(
            ActiveWorkoutUxSession(
                activeWorkoutId = workout.id,
                focusedExerciseInstanceId = exercise.id,
                focusedDraftId = draft.draftId,
                updatedAt = instant(1_350)
            )
        ).successValue()
        repos.lifecycle.updateRestTimer(
            activeWorkoutId = workout.id,
            originSetId = FoundationId("set-origin"),
            restEndsAt = instant(91_200),
            now = instant(1_400)
        ).successValue()

        val recovered = harness.repositories()
        val active = assertNotNull(recovered.lifecycle.currentActiveWorkout())
        val session = assertNotNull(recovered.workouts.load())
        val ux = assertNotNull(recovered.workouts.loadUxSession(workout.id))
        val drafts = recovered.workouts.loadSetDrafts(workout.id)

        assertEquals(workout.id, active.id)
        assertEquals("Bench Press", active.exercises.single().reference.displayNameSnapshot)
        assertEquals(WeightKg(100.0), active.exercises.single().sets.single().weight)
        assertEquals(instant(91_200), session.restEndsAt)
        assertEquals(exercise.id, ux.focusedExerciseInstanceId)
        assertEquals(listOf(draft), drafts)
    }

    @Test
    fun discardClearsActiveRecoveryStateWithoutCreatingCompletedWorkout() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = repos.setLogging.addExercise(
            workout.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
            instant(1_100)
        ).successValue()
        repos.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        repos.workouts.saveSetDraft(
            PersistedSetDraft(
                draftId = FoundationId("draft-1"),
                activeWorkoutId = workout.id,
                exerciseInstanceId = exercise.id,
                position = OrderedPosition(1),
                setKind = SetKind.WEIGHTED,
                reps = 6,
                weight = WeightKg(102.5),
                updatedAt = instant(1_300)
            )
        ).successValue()

        repos.lifecycle.discard(workout.id, instant(2_000)).successValue()

        val recovered = harness.repositories()
        assertNull(recovered.lifecycle.currentActiveWorkout())
        assertNull(recovered.workouts.load())
        assertTrue(recovered.workouts.loadSetDrafts(workout.id).isEmpty())
        assertTrue(recovered.workouts.completedWorkouts().isEmpty())
    }
}
