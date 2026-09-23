package com.jjswigut.oopsallprs

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FinishWorkoutOutcome
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.testing.testAppState
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.history.HistoryStateHolder
import com.jjswigut.oopsallprs.ui.history.mixedCompletedWorkout
import com.jjswigut.oopsallprs.ui.navigation.TopLevelDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppStateWorkoutCompletionTest {
    @Test
    fun successfulRetryClearsFinishConfirmationAndErrorBeforeNextWorkout() = runTest {
        val app = testAppState()
        val first = app.workoutHome.startEmpty().successValue()
        val exercise = app.setLogging.addExercise(
            first.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false)
        ).successValue()
        app.setLogging.confirmSet(
            first.id, exercise.id, SetKind.WEIGHTED, reps = 5, weight = WeightKg(100.0), position = 0
        ).successValue()
        app.activeWorkout.hydrate(first.id)
        app.activeWorkout.requestFinish()
        app.activeWorkout.reportFinishFailure()

        val outcome = app.routines.finishWorkout(first.id).successValue()
        app.presentCompletedWorkout(outcome)
        val next = app.workoutHome.startEmpty().successValue()
        app.activeWorkout.hydrate(next.id)

        assertEquals(next.id, app.activeWorkout.state.value.workout?.workoutId)
        assertFalse(app.activeWorkout.state.value.isFinishConfirmationVisible)
        assertNull(app.activeWorkout.state.value.errorMessage)
    }

    @Test
    fun failedHistoryReadDoesNotHideTheCommittedWorkoutOrTrapNavigation() = runTest {
        val store = InMemoryFoundationStore()
        val history = HistoryStateHolder(object : WorkoutRepository by store {
            override suspend fun completedWorkouts() = error("Unavailable")
        })
        val app = withHistory(store, history)
        val workout = mixedCompletedWorkout()

        app.presentCompletedWorkout(FinishWorkoutOutcome(workout, true))

        assertEquals(TopLevelDestination.HISTORY, app.navigation.state.value.selectedDestination)
        assertFalse(app.navigation.state.value.isActiveWorkoutPresented)
        assertNull(app.activeSession.value)
        assertEquals(workout.id, history.state.value.selectedSummary?.workoutId)
        assertTrue(history.state.value.completionNotice.orEmpty().startsWith("Workout saved."))
    }

    @Test
    fun cancelledRefreshPropagatesButCannotUndoSavedPresentation() = runTest {
        val store = InMemoryFoundationStore()
        val history = HistoryStateHolder(object : WorkoutRepository by store {
            override suspend fun completedWorkouts(): Nothing = throw CancellationException("Cancelled")
        })
        val app = withHistory(store, history)
        val workout = mixedCompletedWorkout()

        assertFailsWith<CancellationException> {
            app.presentCompletedWorkout(FinishWorkoutOutcome(workout, true))
        }
        assertEquals(TopLevelDestination.HISTORY, app.navigation.state.value.selectedDestination)
        assertEquals(workout.id, history.state.value.selectedSummary?.workoutId)
        assertEquals("Workout saved.", history.state.value.completionNotice)
    }

    private fun withHistory(store: InMemoryFoundationStore, history: HistoryStateHolder): AppState {
        val base = testAppState(store = store)
        return AppState(
            workoutLifecycle = base.workoutLifecycle,
            setLogging = base.setLogging,
            navigation = base.navigation,
            workoutHome = base.workoutHome,
            activeWorkout = base.activeWorkout,
            exercisePicker = base.exercisePicker,
            exerciseManagement = base.exerciseManagement,
            exerciseCatalog = base.exerciseCatalog,
            routines = base.routines,
            progress = base.progress,
            history = history,
            profile = base.profile,
            fullAccess = FullAccessUseCases(store)
        )
    }
}
