package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.testAppState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppShellFinishSummaryTest {
    @Test
    fun appStateCanRouteFinishedWorkoutToHistorySummary() = runTest {
        val appState = testAppState()
        val workout = appState.workoutLifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = appState.setLogging.addExercise(
            workout.id,
            com.jjswigut.oopsallprs.domain.model.ExerciseReference(
                com.jjswigut.oopsallprs.domain.model.FoundationId("exercise-bench"),
                "Bench Press",
                isBodyweight = false
            ),
            instant(1_100)
        ).successValue()
        appState.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        val completed = appState.routines.finishWorkout(workout.id, instant(2_000)).successValue()

        appState.history.presentCompletedWorkout(completed.id)
        appState.navigation.selectDestination(TopLevelDestination.HISTORY, instant(2_200))

        assertEquals(TopLevelDestination.HISTORY, appState.navigation.state.value.selectedDestination)
        assertNotNull(appState.history.state.value.selectedSummary)
    }
}
