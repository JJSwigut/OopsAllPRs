package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.exercise.ExerciseManagementStateHolder
import com.jjswigut.oopsallprs.ui.routine.RoutineStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ManagementFlowSessionRegressionTest {
    @Test
    fun openingAndCancelingManagementFlowsDoesNotClearActiveRestSession() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.lifecycle.startRestTimer(
            activeWorkoutId = workout.id,
            originSetId = FoundationId("set-origin"),
            durationSeconds = 120,
            now = instant(2_000)
        ).successValue()
        val routineHolder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        val exerciseHolder = ExerciseManagementStateHolder(harness.exerciseCatalog)

        routineHolder.beginCreateRoutine()
        routineHolder.cancelEditor()
        exerciseHolder.open()
        exerciseHolder.beginCreate()
        exerciseHolder.cancelDraft()
        exerciseHolder.close()

        val session = harness.store.load()
        assertEquals(workout.id, session?.activeWorkoutId)
        assertEquals(instant(122_000), session?.restEndsAt)
    }
}
