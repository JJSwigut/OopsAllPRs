package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedbackKind
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
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
    fun weightedFeedbackRejectsDominatedSetButAllowsHeavierLowerRepSet() = runTest {
        val harness = FoundationHarness()
        val useCase = ActivePrFeedbackUseCase(harness.store)
        val previousWorkout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val previousExercise = harness.setLogging
            .addExercise(previousWorkout.id, harness.weightedReference, instant(1_100))
            .successValue()
        harness.setLogging.confirmSet(
            previousWorkout.id,
            previousExercise.id,
            SetKind.WEIGHTED,
            reps = 16,
            weight = WeightKg(25.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        harness.routines.finishWorkout(previousWorkout.id, instant(2_000)).successValue()

        val workout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val exercise = harness.setLogging
            .addExercise(workout.id, harness.weightedReference, instant(3_100))
            .successValue()
        val dominated = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 14,
            weight = WeightKg(25.0),
            position = 0,
            loggedAt = instant(3_200)
        ).successValue()
        val afterDominated = harness.lifecycle.activeWorkout(workout.id)!!

        assertNull(useCase.feedbackFor(afterDominated, afterDominated.exercises.single(), dominated))

        val heavier = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 10,
            weight = WeightKg(30.0),
            position = 1,
            loggedAt = instant(3_300)
        ).successValue()
        val afterHeavier = harness.lifecycle.activeWorkout(workout.id)!!

        assertEquals(
            "New PR: 30 kg x 10",
            useCase.feedbackFor(afterHeavier, afterHeavier.exercises.single(), heavier)?.label
        )
    }

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

    @Test
    fun activeFeedbackAndRebuildShareLoadedBodyweightSemantics() = runTest {
        val store = InMemoryFoundationStore()
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("active-loaded-bodyweight"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.ADDED_TO_BODYWEIGHT)
            )
        )
        store.saveLoggingConfiguration(configuration)
        val set = ExerciseSet(
            id = FoundationId("active-loaded-set"),
            exerciseInstanceId = FoundationId("active-exercise"),
            position = OrderedPosition(0),
            setKind = SetKind.BODYWEIGHT,
            weight = WeightKg(20.0),
            reps = 6,
            loggedAt = instant(1_200),
            createdAt = instant(1_100),
            updatedAt = instant(1_200),
            captureConfigurationId = configuration.id
        )
        val reference = ExerciseReference(
            exerciseCatalogId = FoundationId("loaded-bodyweight-exercise"),
            displayNameSnapshot = "Loaded bodyweight",
            isBodyweight = true,
            resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                configuration = configuration,
                source = LoggingConfigurationSource.WORKOUT_OVERRIDE
            )
        )
        val exercise = ActiveExercise(
            id = FoundationId("active-exercise"),
            activeWorkoutId = FoundationId("active-workout"),
            reference = reference,
            position = OrderedPosition(0),
            sets = listOf(set)
        )
        val workout = ActiveWorkout(
            id = FoundationId("active-workout"),
            startedAt = instant(1_000),
            exercises = listOf(exercise),
            createdAt = instant(1_000),
            updatedAt = instant(1_200)
        )

        val feedback = assertNotNull(ActivePrFeedbackUseCase(store).feedbackFor(workout, exercise, set))
        assertEquals(ActivePrFeedbackKind.WEIGHT_FOR_REPS, feedback.kind)
        assertEquals(20.0, feedback.newValue)

        val completed = CompletedWorkout(
            id = FoundationId("completed-workout"),
            sourceActiveWorkoutId = workout.id,
            startedAt = workout.startedAt,
            finishedAt = instant(2_000),
            durationMs = 1_000,
            routineId = null,
            exercises = listOf(
                CompletedExercise(
                    id = FoundationId("completed-exercise"),
                    completedWorkoutId = FoundationId("completed-workout"),
                    exerciseCatalogId = reference.exerciseCatalogId,
                    displayNameSnapshot = reference.displayNameSnapshot,
                    position = OrderedPosition(0),
                    loggedSets = listOf(set)
                )
            ),
            createdAt = instant(2_000)
        )
        PersonalRecordDerivationUseCase(store).rebuildFrom(listOf(completed))

        val rebuiltBestSet = assertNotNull(
            store.progressPoints().singleOrNull {
                it.metricCode == ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode
            }
        )
        assertEquals(feedback.newValue, rebuiltBestSet.value)
    }
}
