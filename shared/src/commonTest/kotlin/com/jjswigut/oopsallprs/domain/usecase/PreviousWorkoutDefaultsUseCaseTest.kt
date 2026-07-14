package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreviousWorkoutDefaultsUseCaseTest {
    @Test
    fun most_recent_completed_workout_for_exercise_wins() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store)
        harness.store.finishWorkout(
            completedWorkout(
                id = "completed-old",
                exerciseId = harness.weightedReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(loggedSet("old-set", reps = 5, weight = WeightKg(90.0)))
            )
        ).successValue()
        harness.store.finishWorkout(
            completedWorkout(
                id = "completed-new",
                exerciseId = harness.weightedReference.exerciseCatalogId,
                finishedAtMs = 4_000,
                sets = listOf(loggedSet("new-set", reps = 6, weight = WeightKg(110.0)))
            )
        ).successValue()

        val value = defaults.valueFor(harness.weightedReference.exerciseCatalogId, isBodyweight = false, setIndex = 0)

        assertEquals(6, value?.reps)
        assertEquals(WeightKg(110.0), value?.weight)
        assertEquals(FoundationId("completed-new"), value?.sourceCompletedWorkoutId)
    }

    @Test
    fun set_order_is_used_and_last_valid_set_repeats_for_extra_drafts() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store)
        harness.store.finishWorkout(
            completedWorkout(
                id = "completed",
                exerciseId = harness.weightedReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(
                    loggedSet("set-1", position = 0, reps = 8, weight = WeightKg(100.0)),
                    loggedSet("set-2", position = 1, reps = 6, weight = WeightKg(105.0))
                )
            )
        ).successValue()

        val first = defaults.valueFor(harness.weightedReference.exerciseCatalogId, isBodyweight = false, setIndex = 0)
        val second = defaults.valueFor(harness.weightedReference.exerciseCatalogId, isBodyweight = false, setIndex = 1)
        val extra = defaults.valueFor(harness.weightedReference.exerciseCatalogId, isBodyweight = false, setIndex = 3)

        assertEquals(8, first?.reps)
        assertEquals(WeightKg(100.0), first?.weight)
        assertEquals(6, second?.reps)
        assertEquals(WeightKg(105.0), second?.weight)
        assertEquals(second, extra)
    }

    @Test
    fun invalid_latest_sets_are_ignored_for_next_valid_completed_workout() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store)
        harness.store.finishWorkout(
            completedWorkout(
                id = "valid-older",
                exerciseId = harness.weightedReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(loggedSet("valid-set", reps = 5, weight = WeightKg(80.0)))
            )
        ).successValue()
        harness.store.finishWorkout(
            completedWorkout(
                id = "invalid-newer",
                exerciseId = harness.weightedReference.exerciseCatalogId,
                finishedAtMs = 4_000,
                sets = listOf(loggedSet("invalid-set", reps = 7, weight = null))
            )
        ).successValue()

        val value = defaults.valueFor(harness.weightedReference.exerciseCatalogId, isBodyweight = false, setIndex = 0)

        assertEquals(5, value?.reps)
        assertEquals(WeightKg(80.0), value?.weight)
        assertEquals(FoundationId("valid-older"), value?.sourceCompletedWorkoutId)
    }

    @Test
    fun bodyweight_previous_values_are_reps_only() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store)
        harness.store.finishWorkout(
            completedWorkout(
                id = "bodyweight",
                exerciseId = harness.bodyweightReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(loggedSet("pullup-set", kind = SetKind.BODYWEIGHT, reps = 10, weight = WeightKg(20.0)))
            )
        ).successValue()

        val value = defaults.valueFor(harness.bodyweightReference.exerciseCatalogId, isBodyweight = true, setIndex = 0)

        assertEquals(SetKind.BODYWEIGHT, value?.setKind)
        assertEquals(10, value?.reps)
        assertNull(value?.weight)
    }

    @Test
    fun timed_previous_values_are_duration_only() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store)
        harness.store.finishWorkout(
            completedWorkout(
                id = "timed",
                exerciseId = harness.timedReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(loggedSet("plank-set", kind = SetKind.TIMED, reps = null, weight = null, durationMs = 90_000L))
            )
        ).successValue()

        val value = defaults.valueFor(
            exerciseCatalogId = harness.timedReference.exerciseCatalogId,
            isBodyweight = true,
            loggingMode = ExerciseLoggingMode.TIMED,
            setIndex = 0
        )

        assertEquals(SetKind.TIMED, value?.setKind)
        assertEquals(90_000L, value?.durationMs)
        assertNull(value?.reps)
        assertNull(value?.weight)
    }

    @Test
    fun missing_history_returns_null() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store)

        val value = defaults.valueFor(harness.weightedReference.exerciseCatalogId, isBodyweight = false, setIndex = 0)

        assertNull(value)
    }

    @Test
    fun ambiguousLegacyBodyweightLoadIsNotPrefilledAsModernAddedLoad() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store, harness.store)
        harness.store.finishWorkout(
            completedWorkout(
                id = "legacy-bodyweight-load",
                exerciseId = harness.bodyweightReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(
                    loggedSet(
                        "legacy-loaded-pullup",
                        kind = SetKind.BODYWEIGHT,
                        reps = 8,
                        weight = WeightKg(20.0)
                    )
                )
            )
        ).successValue()
        val modernAddedLoad = LoggingConfiguration(
            id = LoggingConfigurationId("modern-added-bodyweight-v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.ADDED_TO_BODYWEIGHT)
            )
        )

        val value = defaults.valueFor(
            exerciseCatalogId = harness.bodyweightReference.exerciseCatalogId,
            isBodyweight = true,
            setIndex = 0,
            loggingConfiguration = modernAddedLoad
        )

        assertEquals(8, value?.reps)
        assertNull(value?.weight)
    }

    @Test
    fun exactDistanceCaptureRecoversDistanceWithoutReps() = runTest {
        val harness = FoundationHarness()
        val defaults = PreviousWorkoutDefaultsUseCase(harness.store, harness.store)
        val distance = LoggingConfiguration(
            id = LoggingConfigurationId("distance-default-v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED))
        )
        harness.store.saveLoggingConfiguration(distance).successValue()
        harness.store.finishWorkout(
            completedWorkout(
                id = "distance-history",
                exerciseId = harness.bodyweightReference.exerciseCatalogId,
                finishedAtMs = 2_000,
                sets = listOf(
                    loggedSet(
                        id = "distance-set",
                        kind = SetKind.BODYWEIGHT,
                        reps = null,
                        weight = null,
                        distanceMeters = 400.0,
                        captureConfigurationId = distance.id
                    )
                )
            )
        ).successValue()

        val value = defaults.valueFor(
            exerciseCatalogId = harness.bodyweightReference.exerciseCatalogId,
            isBodyweight = true,
            setIndex = 0,
            loggingConfiguration = distance
        )

        assertNull(value?.reps)
        assertEquals(400.0, value?.distanceMeters)
    }
}

private fun completedWorkout(
    id: String,
    exerciseId: FoundationId,
    finishedAtMs: Long,
    sets: List<ExerciseSet>
): CompletedWorkout {
    val completedId = FoundationId(id)
    return CompletedWorkout(
        id = completedId,
        sourceActiveWorkoutId = FoundationId("source-$id"),
        startedAt = instant(finishedAtMs - 1_000),
        finishedAt = instant(finishedAtMs),
        durationMs = 1_000,
        routineId = null,
        exercises = listOf(
            CompletedExercise(
                id = FoundationId("completed-exercise-$id"),
                completedWorkoutId = completedId,
                exerciseCatalogId = exerciseId,
                displayNameSnapshot = exerciseId.value,
                position = OrderedPosition(0),
                loggedSets = sets
            )
        ),
        createdAt = instant(finishedAtMs)
    )
}

private fun loggedSet(
    id: String,
    position: Int = 0,
    kind: SetKind = SetKind.WEIGHTED,
    reps: Int?,
    weight: WeightKg?,
    durationMs: Long? = null,
    distanceMeters: Double? = null,
    captureConfigurationId: LoggingConfigurationId? = null
): ExerciseSet {
    val set = ExerciseSet(
        id = FoundationId(id),
        exerciseInstanceId = FoundationId("active-exercise"),
        position = OrderedPosition(position),
        setKind = kind,
        weight = weight,
        reps = reps,
        loggedAt = instant(1_500L + position),
        createdAt = instant(1_400L + position),
        updatedAt = instant(1_500L + position),
        durationMs = durationMs,
        distanceMeters = distanceMeters
    )
    return captureConfigurationId?.let { set.copy(captureConfigurationId = it) } ?: set
}
