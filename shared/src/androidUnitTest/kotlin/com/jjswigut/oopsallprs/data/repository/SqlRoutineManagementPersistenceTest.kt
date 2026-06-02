package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlRoutineManagementPersistenceTest {
    @Test
    fun routineCreateAndUpdateRecoverAfterRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val saved = repos.routineUseCases.saveRoutine(
            routineId = null,
            name = "Push",
            exercises = listOf(routineExercise("routine-draft", "routine-exercise-1", "Bench Press", 100.0, 120)),
            now = instant(1_000)
        ).successValue()

        repos.routineUseCases.saveRoutine(
            routineId = saved.id,
            name = "Push Day",
            exercises = listOf(routineExercise(saved.id.value, saved.exercises.single().id.value, "Bench Press", 110.0, 180)),
            now = instant(2_000)
        ).successValue()

        val recovered = harness.repositories().routineUseCases.listRoutines().single()

        assertEquals("Push Day", recovered.name)
        assertEquals(WeightKg(110.0), recovered.exercises.single().plannedSets.single().targetWeight)
        assertEquals(RestConfiguration(durationSeconds = 180), recovered.exercises.single().rest)
    }

    private fun routineExercise(
        routineId: String,
        routineExerciseId: String,
        name: String,
        weight: Double,
        restSeconds: Int
    ): RoutineExercise =
        RoutineExercise(
            id = FoundationId(routineExerciseId),
            routineId = FoundationId(routineId),
            exerciseCatalogId = FoundationId("exercise-bench"),
            displayNameSnapshot = name,
            position = OrderedPosition(0),
            rest = RestConfiguration(durationSeconds = restSeconds),
            plannedSets = listOf(
                RoutineSetTemplate(
                    id = FoundationId("routine-set-1"),
                    routineExerciseId = FoundationId(routineExerciseId),
                    position = OrderedPosition(0),
                    targetWeight = WeightKg(weight),
                    targetReps = 5,
                    setKind = SetKind.WEIGHTED
                )
            )
        )
}
