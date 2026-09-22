package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.AppState
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.testAppState
import com.jjswigut.oopsallprs.ui.history.HistoryState
import com.jjswigut.oopsallprs.ui.progress.ProgressState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DestinationBackPolicyTest {
    @Test
    fun sourceWorkoutBackClosesVisibleHistoryWithoutClearingRetainedReading() = runTest {
        val app = appWithCompletedWorkout()
        app.navigation.selectDestination(TopLevelDestination.PROGRESS)
        app.progress.selectReading(EvidenceReading.CAPABILITY)
        val reading = assertNotNull(app.progress.state.value.selectedReading)

        app.history.presentCompletedWorkout(reading.evidence.first().sourceWorkoutId)
        app.navigation.selectDestination(TopLevelDestination.HISTORY)

        assertEquals(DestinationBackAction.CLOSE_HISTORY_DETAIL, app.destinationBackAction())
        app.history.clearSelection()
        assertNull(app.destinationBackAction())
        assertEquals(reading, app.progress.state.value.selectedReading)

        app.navigation.selectDestination(TopLevelDestination.PROGRESS)
        assertEquals(DestinationBackAction.CLOSE_PROGRESS_READING, app.destinationBackAction())
    }

    @Test
    fun exerciseSourceWorkoutBackClosesVisibleHistoryBeforeRetainedExercise() = runTest {
        val app = appWithCompletedWorkout()
        val exercise = app.progress.state.value.exerciseGroups.single()
        app.progress.selectExercise(exercise.exerciseCatalogId)
        app.navigation.selectDestination(TopLevelDestination.PROGRESS)

        app.history.presentCompletedWorkout(assertNotNull(exercise.capability).evidence.first().sourceWorkoutId)
        app.navigation.selectDestination(TopLevelDestination.HISTORY)

        assertEquals(DestinationBackAction.CLOSE_HISTORY_DETAIL, app.destinationBackAction())
        assertEquals(exercise.exerciseCatalogId, app.progress.state.value.selectedExerciseId)
    }

    @Test
    fun progressBackClosesEvidenceThenExerciseAndIgnoresRetainedHistory() = runTest {
        val app = appWithCompletedWorkout()
        app.navigation.selectDestination(TopLevelDestination.PROGRESS)
        app.progress.selectExercise(app.progress.state.value.exerciseGroups.single().exerciseCatalogId)
        app.progress.openEvidence(app.progress.state.value.personalRecords.first().id)

        assertNotNull(app.history.state.value.selectedSummary)
        assertNotNull(app.progress.state.value.selectedExercise)
        assertEquals(DestinationBackAction.CLOSE_PROGRESS_EVIDENCE, app.destinationBackAction())
        app.progress.clearEvidence()
        assertEquals(DestinationBackAction.CLOSE_PROGRESS_EXERCISE, app.destinationBackAction())
        app.progress.clearExerciseSelection()
        assertNull(app.destinationBackAction())
        assertNotNull(app.history.state.value.selectedSummary)
    }

    @Test
    fun progressBackClosesReadingThenReleasesBackDespiteRetainedHistory() = runTest {
        val app = appWithCompletedWorkout()
        app.navigation.selectDestination(TopLevelDestination.PROGRESS)
        app.progress.selectReading(EvidenceReading.WORK_CAPACITY)

        assertEquals(DestinationBackAction.CLOSE_PROGRESS_READING, app.destinationBackAction())
        app.progress.clearReading()
        assertNull(app.destinationBackAction())
        assertNotNull(app.history.state.value.selectedSummary)
    }

    @Test
    fun progressBackClosesRecentTrainingRecordsOnlyWhenProgressIsVisible() {
        val progress = ProgressState(isRecentTrainingRecordsOpen = true)

        assertEquals(
            DestinationBackAction.CLOSE_PROGRESS_RECENT_TRAINING_RECORDS,
            resolveDestinationBackAction(TopLevelDestination.PROGRESS, progress, HistoryState())
        )
        assertNull(resolveDestinationBackAction(TopLevelDestination.TRAIN, progress, HistoryState()))
    }

    @Test
    fun trainAndProfileDoNotConsumeBackForHiddenDetails() = runTest {
        val app = appWithCompletedWorkout()
        app.progress.selectExercise(app.progress.state.value.exerciseGroups.single().exerciseCatalogId)
        app.progress.openEvidence(app.progress.state.value.personalRecords.first().id)

        listOf(TopLevelDestination.TRAIN, TopLevelDestination.PROFILE).forEach { destination ->
            app.navigation.selectDestination(destination)
            assertNull(app.destinationBackAction(), destination.name)
        }
        assertNotNull(app.progress.state.value.selectedEvidence)
        assertNotNull(app.progress.state.value.selectedExercise)
        assertNotNull(app.history.state.value.selectedSummary)
    }

    @Test
    fun historyOverviewDoesNotConsumeBackForHiddenProgressEvidence() = runTest {
        val app = appWithCompletedWorkout()
        app.progress.selectExercise(app.progress.state.value.exerciseGroups.single().exerciseCatalogId)
        app.progress.openEvidence(app.progress.state.value.personalRecords.first().id)
        app.history.clearSelection()
        app.navigation.selectDestination(TopLevelDestination.HISTORY)

        assertNull(app.destinationBackAction())
    }

    @Test
    fun allDestinationRootsReleaseBackWhenNoDetailIsSelected() {
        TopLevelDestination.entries.forEach { destination ->
            assertNull(resolveDestinationBackAction(destination, ProgressState(), HistoryState()), destination.name)
        }
    }

    private fun AppState.destinationBackAction() = resolveDestinationBackAction(
        navigation.state.value.selectedDestination,
        progress.state.value,
        history.state.value
    )

    private suspend fun appWithCompletedWorkout(): AppState {
        val app = testAppState()
        val workout = app.workoutLifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = app.setLogging.addExercise(
            workout.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
            instant(1_100)
        ).successValue()
        app.setLogging.confirmSet(
            workout.id, exercise.id, SetKind.WEIGHTED, 8, WeightKg(20.0), 0, instant(1_200)
        ).successValue()
        val completed = app.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
        app.progress.refresh()
        app.history.presentCompletedWorkout(completed.id)
        return app
    }
}
