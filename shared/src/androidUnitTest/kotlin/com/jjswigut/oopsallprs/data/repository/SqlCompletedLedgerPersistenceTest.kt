package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlCompletedLedgerPersistenceTest {
    @Test
    fun completedWorkoutReloadsWithLoggedLedgerRows() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val completedId = createCompletedMixedWorkout(repos)

        val recovered = harness.repositories()
        val completed = assertNotNull(recovered.workouts.completedWorkout(completedId))

        assertEquals(2, completed.exercises.size)
        assertEquals("Bench Press", completed.exercises.first().displayNameSnapshot)
        assertEquals(WeightKg(100.0), completed.exercises.first().loggedSets.single().weight)
        assertEquals("Pull-Up", completed.exercises.last().displayNameSnapshot)
        assertEquals(12, completed.exercises.last().loggedSets.single().reps)
    }

    @Test
    fun templateCreatedFromCompletedWorkoutSurvivesAndLaunches() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val completedId = createCompletedMixedWorkout(repos)
        val template = repos.routineUseCases.saveCompletedWorkoutAsRoutine(completedId, "Full Body", instant(3_000)).successValue()

        val recovered = harness.repositories()
        val restored = assertNotNull(recovered.routines.routine(template.id))
        val active = recovered.lifecycle.startFromRoutine(restored.id, instant(4_000)).successValue()

        assertEquals("Full Body", restored.name)
        assertEquals(2, restored.exercises.size)
        assertEquals(2, active.exercises.size)
        assertEquals(completedId, restored.sourceCompletedWorkoutId)
    }

    @Test
    fun circuitTemplateSurvivesCompletedWorkoutReload() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val completedId = createCompletedMixedWorkout(repos, asCircuit = true)

        val recovered = harness.repositories()
        val completed = assertNotNull(recovered.workouts.completedWorkout(completedId))
        assertEquals(1, completed.exercises.mapNotNull { it.groupContext?.groupId }.distinct().size)
        assertEquals(listOf(3, 3), completed.exercises.map { it.groupContext?.rounds })

        val template = recovered.routineUseCases.saveCompletedWorkoutAsRoutine(
            completedId,
            "Full Body Circuit",
            instant(3_000)
        ).successValue()
        val launched = recovered.lifecycle.startFromRoutine(template.id, instant(4_000)).successValue()

        assertEquals(listOf("Circuit", "Circuit"), launched.exercises.map { it.groupContext?.label })
        assertEquals(listOf(3, 3), launched.exercises.map { it.groupContext?.rounds })
    }
}

internal suspend fun createCompletedMixedWorkout(
    repos: SqlRepositoryBundle,
    asCircuit: Boolean = false
): FoundationId {
    val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
    val weighted = repos.setLogging.addExercise(
        workout.id,
        ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
        instant(1_100)
    ).successValue()
    val bodyweight = repos.setLogging.addExercise(
        workout.id,
        ExerciseReference(FoundationId("exercise-pullup"), "Pull-Up", isBodyweight = true),
        instant(1_150)
    ).successValue()
    if (asCircuit) {
        repos.setLogging.groupExercisesAsCircuit(
            workout.id,
            listOf(weighted.id, bodyweight.id),
            instant(1_175)
        ).successValue()
    }
    repos.setLogging.confirmSet(workout.id, weighted.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
    repos.setLogging.confirmSet(workout.id, bodyweight.id, SetKind.BODYWEIGHT, 12, null, 0, instant(1_250)).successValue()
    val completed = repos.routineUseCases.finishWorkout(workout.id, instant(2_000)).successValue().workout
    assertTrue(repos.lifecycle.currentActiveWorkout() == null)
    return completed.id
}
