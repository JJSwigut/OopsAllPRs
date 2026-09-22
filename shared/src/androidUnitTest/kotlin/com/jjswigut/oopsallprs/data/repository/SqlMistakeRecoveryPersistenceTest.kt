package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlMistakeRecoveryPersistenceTest {
    private val reference = ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false)

    @Test
    fun editAndDeleteLoggedSetPersistThroughSqlStore() = runTest {
        val bundle = SqlFoundationStoreTestHarness().repositories()
        val workout = bundle.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = bundle.setLogging.addExercise(workout.id, reference, instant(1_100)).successValue()
        val logged = bundle.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()

        bundle.setLogging.editLoggedSet(workout.id, logged.id, 6, WeightKg(102.5), instant(1_500)).successValue()
        val edited = bundle.workouts.activeWorkout(workout.id)!!.exercises.single().sets.single()
        assertEquals(6, edited.reps)
        assertEquals(instant(1_200), edited.loggedAt)
        assertEquals(instant(1_500), edited.editedAt)

        bundle.setLogging.deleteLoggedSet(workout.id, logged.id, instant(1_700)).successValue()

        assertTrue(bundle.workouts.activeWorkout(workout.id)!!.exercises.single().sets.isEmpty())
    }

    @Test
    fun completedWorkoutDeletionRebuildsRecordsAndRemovesSqlRows() = runTest {
        val bundle = SqlFoundationStoreTestHarness().repositories()
        val first = bundle.completedWeightedWorkout(weight = 100.0, startedAt = 1_000, finishedAt = 2_000)
        val second = bundle.completedWeightedWorkout(weight = 120.0, startedAt = 3_000, finishedAt = 4_000)

        bundle.routineUseCases.deleteCompletedWorkout(second.id, instant(5_000)).successValue()

        assertNull(bundle.workouts.completedWorkout(second.id))
        assertEquals(first.id, bundle.workouts.completedWorkouts().single().id)
        assertTrue(bundle.progress.personalRecords().all { it.sourceWorkoutId == first.id })
    }
}

private suspend fun SqlRepositoryBundle.completedWeightedWorkout(
    weight: Double,
    startedAt: Long,
    finishedAt: Long
) = run {
    val workout = lifecycle.startEmpty(instant(startedAt)).successValue()
    val exercise = setLogging.addExercise(workout.id, ExerciseReference(FoundationId("exercise-bench"), "Bench Press", false), instant(startedAt + 100)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.WEIGHTED,
        reps = 5,
        weight = WeightKg(weight),
        position = 0,
        loggedAt = instant(startedAt + 200)
    )
    routineUseCases.finishWorkout(workout.id, instant(finishedAt)).successValue().workout
}
