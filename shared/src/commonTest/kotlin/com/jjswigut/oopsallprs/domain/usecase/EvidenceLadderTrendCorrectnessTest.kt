package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.EvidenceLadderSnapshot
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EvidenceLadderTrendCorrectnessTest {
    @Test
    fun oldHistoryCannotTurnARecentPlateauIntoImprovementOrLockConsistency() {
        val old = sessions(listOf(0, 7, 14, 21, 28, 35), weight = 10.0)
        val recent = sessions(listOf(365, 372, 379, 386, 393, 400), weight = 25.0)
        val snapshot = project(old + recent)

        assertEquals(ProgressTrendState.HOLDING_STEADY, snapshot.reading(EvidenceReading.CAPABILITY).state)
        assertEquals(ProgressTrendState.HOLDING_STEADY, snapshot.reading(EvidenceReading.WORK_CAPACITY).state)
        assertEquals(ProgressTrendState.STEADY, snapshot.reading(EvidenceReading.CONSISTENCY).state)
        snapshot.overallReadings.forEach { reading ->
            assertEquals(6, reading.coverage.qualifyingSessions)
            assertEquals(35, reading.coverage.spanDays)
            assertTrue(reading.evidence.all { it.recordedAt >= atDay(365) })
            assertEquals(atDay(365), reading.comparisonWindow?.earlierStart)
        }
        assertEquals(12, snapshot.achievements.size)
    }

    @Test
    fun recentDeclineIsNotHiddenByYearsOfLighterSets() {
        val snapshot = project(
            sessions(listOf(0, 7, 14, 21, 28, 35), weight = 10.0) +
                sessions(listOf(365, 372, 379), weight = 30.0) +
                sessions(listOf(386, 393, 400), weight = 20.0)
        )

        assertEquals(ProgressTrendState.REBUILDING, snapshot.reading(EvidenceReading.CAPABILITY).state)
        assertTrue(requireNotNull(snapshot.reading(EvidenceReading.CAPABILITY).changePercent) < 0.0)
    }

    @Test
    fun staleExerciseCannotRemainAMatureContributorWhenOtherTrainingContinues() {
        val snapshot = project(
            sessions(listOf(0, 7, 14, 21, 28, 35)) +
                sessions(listOf(100), exercise = "new-exercise")
        )

        assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.CAPABILITY).state)
        assertEquals(0, snapshot.exerciseProgressions.single { it.exerciseCatalogId.value == "exercise" }
            .capability.coverage.qualifyingSessions)
        assertEquals(7, snapshot.achievements.size)
    }

    @Test
    fun explicitAsOfTimeCanExpireStaleEvidenceAndExcludesFutureWorkouts() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35)) + sessions(listOf(200))
        val snapshot = project(workouts, asOf = atDay(100))

        snapshot.overallReadings.forEach {
            assertEquals(ProgressTrendState.BUILDING_TREND, it.state)
            assertTrue(it.evidence.isEmpty())
        }
        assertEquals(7, snapshot.achievements.size)
    }

    @Test
    fun windowExplanationShowsUtcDateWithoutChangingAsOfEvidence() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35))
        val snapshot = project(workouts, asOf = Instant.parse("1970-02-05T18:30:00Z"))
        val defaultSnapshot = project(workouts)

        (snapshot.overallReadings + snapshot.exerciseProgressions.map { it.capability }).forEach { summary ->
            assertTrue(summary.explanation.contains("56-day window ending on 1970-02-05."))
            assertTrue(!summary.explanation.contains("18:30"))
        }
        snapshot.overallReadings.forEach { summary ->
            assertEquals(defaultSnapshot.reading(summary.reading).evidence, summary.evidence)
            assertEquals(defaultSnapshot.reading(summary.reading).comparisonWindow, summary.comparisonWindow)
        }
    }

    @Test
    fun windowIncludesExactlyFiftySixDaysButNotOlderWorkouts() {
        val snapshot = project(sessions(listOf(0, 1, 15, 29, 43, 50, 57)))

        snapshot.overallReadings.forEach {
            assertEquals(6, it.coverage.qualifyingSessions)
            assertEquals(56, it.coverage.spanDays)
            assertEquals(atDay(1), it.comparisonWindow?.earlierStart)
        }
    }

    @Test
    fun sixSessionsWithinTwentySevenDaysStillCannotMakeATrend() {
        val snapshot = project(sessions(listOf(0, 5, 10, 15, 20, 27)))

        snapshot.overallReadings.forEach {
            assertEquals(ProgressTrendState.BUILDING_TREND, it.state)
            assertEquals(6, it.coverage.qualifyingSessions)
            assertNull(it.changePercent)
        }
    }

    @Test
    fun recoveryOfRecentRhythmIsNotLockedByAGapInsideTheEarlierRange() {
        val snapshot = project(sessions(listOf(0, 21, 28, 35, 42, 49)))
        val consistency = snapshot.reading(EvidenceReading.CONSISTENCY)

        assertEquals(ProgressTrendState.STEADY, consistency.state)
        assertTrue(consistency.explanation.contains("Median spacing"))
    }

    @Test
    fun widerSpacingDoesNotInferAReturnOrDeload() {
        val snapshot = project(sessions(listOf(0, 3, 6, 20, 34, 48)))
        val consistency = snapshot.reading(EvidenceReading.CONSISTENCY)

        assertEquals(ProgressTrendState.HOLDING_STEADY, consistency.state)
        assertTrue(consistency.explanation.contains("neutral observation"))
    }

    @Test
    fun alternatingWeeklyScheduleWithMinuteLevelNoiseRemainsSteady() {
        val days = listOf(7, 10, 14, 17, 21, 24, 28, 31, 35, 38, 42, 45, 49, 52, 56, 59, 63)
        val workouts = sessions(days).mapIndexed { index, workout ->
            workout.copy(finishedAt = Instant.fromEpochMilliseconds(
                workout.finishedAt.toEpochMilliseconds() + (index % 3) * 60_000L
            ))
        }

        assertEquals(ProgressTrendState.STEADY, project(workouts).reading(EvidenceReading.CONSISTENCY).state)
    }

    @Test
    fun differentKnownExternalLoadConfigurationsDoNotBecomeASingleStrengthTrend() {
        val other = configuration("other-external", LoadRole.EXTERNAL_RESISTANCE)
        val workouts = sessions(listOf(0, 7, 14), weight = 20.0) +
            sessions(listOf(21, 28, 35), weight = 40.0, configuration = other)
        val snapshot = project(workouts, configurations = listOf(other))

        assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.exerciseProgressions.single().capability.state)
        assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.CAPABILITY).state)
        assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.WORK_CAPACITY).state)
        assertEquals(6, snapshot.achievements.size)
        assertEquals(2, snapshot.currentBenchmarks.size)
        assertEquals(setOf(LegacyLoggingConfigurations.weighted.id, other.id),
            snapshot.currentBenchmarks.map { it.series.captureConfigurationId }.toSet())
    }

    @Test
    fun sameKnownExternalLoadConfigurationCanBuildATrend() {
        val configuration = configuration("custom-external", LoadRole.EXTERNAL_RESISTANCE)
        val snapshot = project(
            sessions(listOf(0, 7, 14, 21, 28, 35), configuration = configuration),
            configurations = listOf(configuration)
        )

        assertEquals(ProgressTrendState.HOLDING_STEADY, snapshot.reading(EvidenceReading.CAPABILITY).state)
        assertEquals(6, snapshot.capabilityPoints.size)
    }

    @Test
    fun unsupportedAndUnknownLoadRolesDoNotProduceStrengthClaimsFromOldDerivedPoints() {
        listOf(LoadRole.ASSISTANCE, LoadRole.ADDED_TO_BODYWEIGHT, LoadRole.LEGACY_UNSPECIFIED).forEach { role ->
            val configuration = configuration("role-${role.wireCode.value}", role)
            val workouts = sessions(listOf(0, 7, 14, 21, 28, 35), configuration = configuration)
            listOf(emptyList(), listOf(configuration)).forEach { configurations ->
                val snapshot = project(workouts, configurations = configurations)
                assertTrue(snapshot.capabilityPoints.isEmpty())
                assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.CAPABILITY).state)
                assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.WORK_CAPACITY).state)
                assertEquals(6, snapshot.achievements.size)
            }
        }
    }

    @Test
    fun changedExerciseSelectionDoesNotLookLikeHigherWorkCapacity() {
        val snapshot = project(
            sessions(listOf(0, 7, 14), weight = 20.0) +
                sessions(listOf(21, 28, 35), weight = 100.0, exercise = "different-exercise")
        )

        assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.WORK_CAPACITY).state)
        assertNull(snapshot.reading(EvidenceReading.WORK_CAPACITY).changePercent)
    }

    @Test
    fun missingSourcesAndHighRepEstimatesDoNotQualifyButAchievementsSurvive() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35), reps = 13)
        val snapshot = project(workouts)
        assertTrue(snapshot.capabilityPoints.isEmpty())
        assertEquals(ProgressTrendState.BUILDING_TREND, snapshot.reading(EvidenceReading.CAPABILITY).state)
        assertEquals(6, snapshot.achievements.size)

        val derived = evidence(sessions(listOf(0, 7, 14, 21, 28, 35)))
        val orphaned = EvidenceLadderUseCase().project(emptyList(), derived.first, derived.second, asOf = atDay(35))
        assertTrue(orphaned.capabilityPoints.isEmpty())
        assertTrue(orphaned.currentBenchmarks.isEmpty())
        assertEquals(6, orphaned.achievements.size)
    }

    @Test
    fun duplicatePointsDoNotDoubleWorkCapacityOrSessionCoverage() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35))
        val (records, points) = evidence(workouts)
        val duplicated = points + points.filter { it.recordedAt >= atDay(21) }
        val snapshot = EvidenceLadderUseCase().project(workouts + workouts, records, duplicated)

        snapshot.overallReadings.forEach { assertEquals(6, it.coverage.qualifyingSessions) }
        assertEquals(ProgressTrendState.HOLDING_STEADY, snapshot.reading(EvidenceReading.WORK_CAPACITY).state)
    }

    @Test
    fun workCapacityRotatingRoutinesCompareEachMatureExerciseWithItself() {
        val workouts = sessions(listOf(0, 7, 14), weight = 20.0, exercise = "press") +
            sessions(listOf(21, 28, 35), weight = 24.0, exercise = "press") +
            sessions(listOf(3, 10, 17), weight = 100.0, exercise = "squat") +
            sessions(listOf(24, 31, 38), weight = 140.0, exercise = "squat")
        val summary = project(workouts).reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.RECENT_RANGE_HIGHER, summary.state)
        assertEquals(30.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(2, summary.coverage.contributingExercises)
        assertEquals(12, summary.coverage.qualifyingSessions)
        assertEquals(38, summary.coverage.spanDays)
        assertEquals(setOf("press", "squat"), summary.evidence.map { it.exerciseCatalogId.value }.toSet())
        assertEquals(12, summary.evidence.size)
        assertEquals(atDay(0), summary.comparisonWindow?.earlierStart)
        assertEquals(atDay(38), summary.comparisonWindow?.recentEnd)
        val comparisons = summary.exerciseComparisons.associateBy { it.exerciseCatalogId.value }
        assertEquals(setOf("press", "squat"), comparisons.keys)
        val press = comparisons.getValue("press")
        val squat = comparisons.getValue("squat")
        assertEquals(20.0, press.changePercent, 0.000001)
        assertEquals(atDay(0), press.comparisonWindow.earlierStart)
        assertEquals(atDay(14), press.comparisonWindow.earlierEnd)
        assertEquals(atDay(21), press.comparisonWindow.recentStart)
        assertEquals(atDay(35), press.comparisonWindow.recentEnd)
        assertEquals(40.0, squat.changePercent, 0.000001)
        assertEquals(atDay(3), squat.comparisonWindow.earlierStart)
        assertEquals(atDay(17), squat.comparisonWindow.earlierEnd)
        assertEquals(atDay(24), squat.comparisonWindow.recentStart)
        assertEquals(atDay(38), squat.comparisonWindow.recentEnd)
        assertTrue(summary.explanation.contains("median", ignoreCase = true))
        assertTrue(summary.explanation.contains("exercise", ignoreCase = true))
    }

    @Test
    fun workCapacitySparseNewAndShortSpanExercisesDoNotVetoOrInfluenceMatureEvidence() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35), exercise = "stable") +
            sessions(listOf(1, 36), weight = 500.0, exercise = "sparse") +
            sessions(listOf(37), weight = 1_000.0, exercise = "new") +
            sessions(listOf(15, 16, 17, 18, 19, 20), weight = 2_000.0, exercise = "short-span")
        val summary = project(workouts).reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.HOLDING_STEADY, summary.state)
        assertEquals(0.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(1, summary.coverage.contributingExercises)
        assertEquals(6, summary.coverage.qualifyingSessions)
        assertEquals(35, summary.coverage.spanDays)
        assertEquals(setOf("stable"), summary.evidence.map { it.exerciseCatalogId.value }.toSet())
        assertEquals(atDay(35), summary.comparisonWindow?.recentEnd)
        assertEquals(listOf("stable"), summary.exerciseComparisons.map { it.exerciseCatalogId.value })
        assertTrue(summary.explanation.contains("exclud", ignoreCase = true))
        assertTrue(summary.explanation.contains("28"))
    }

    @Test
    fun workCapacityUnsupportedExercisesDoNotVetoAnOtherwiseComparableSession() {
        val days = listOf(0, 7, 14, 21, 28, 35)
        val stable = sessions(days, exercise = "stable")
        val assisted = configuration("assisted", LoadRole.ASSISTANCE)
        val added = configuration("added", LoadRole.ADDED_TO_BODYWEIGHT)
        val unresolved = configuration("unresolved", LoadRole.EXTERNAL_RESISTANCE)
        val bodyweight = sessions(days, exercise = "bodyweight", configuration = LegacyLoggingConfigurations.bodyweight)
            .map { workout ->
                workout.copy(exercises = workout.exercises.map { exercise ->
                    exercise.copy(loggedSets = exercise.loggedSets.map { it.copy(setKind = SetKind.BODYWEIGHT, weight = null) })
                })
            }
        val timed = sessions(days, exercise = "timed", configuration = LegacyLoggingConfigurations.timed)
            .map { workout ->
                workout.copy(exercises = workout.exercises.map { exercise ->
                    exercise.copy(loggedSets = exercise.loggedSets.map {
                        it.copy(setKind = SetKind.TIMED, weight = null, reps = null, durationMs = 60_000L)
                    })
                })
            }
        val loaded = mergeSameDayWorkouts(stable +
            sessions(days, exercise = "assisted", configuration = assisted) +
            sessions(days, exercise = "added", configuration = added) +
            sessions(days, exercise = "unresolved", configuration = unresolved))
        // Unsupported roles include stale derived volume; bodyweight and timed sets have no volume points.
        val (records, points) = evidence(loaded)
        val workouts = mergeSameDayWorkouts(loaded + bodyweight + timed)
        val snapshot = EvidenceLadderUseCase().project(workouts, records, points, listOf(assisted, added))
        val summary = snapshot.reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.HOLDING_STEADY, summary.state)
        assertEquals(1, summary.coverage.contributingExercises)
        assertEquals(6, summary.coverage.qualifyingSessions)
        assertEquals(setOf("stable"), summary.evidence.map { it.exerciseCatalogId.value }.toSet())
        assertEquals(6, summary.evidence.size)
        assertTrue(summary.explanation.contains("exclud", ignoreCase = true))
        assertTrue(summary.explanation.contains("unsupported", ignoreCase = true))
        assertEquals(records.size, snapshot.achievements.size)
    }

    @Test
    fun workCapacityChangedConfigurationExcludesOnlyThatExercise() {
        val other = configuration("changed-external", LoadRole.EXTERNAL_RESISTANCE)
        val workouts = mergeSameDayWorkouts(
            sessions(listOf(0, 7, 14, 21, 28, 35), exercise = "stable") +
                sessions(listOf(0, 7, 14), weight = 10.0, exercise = "changing") +
                sessions(listOf(21, 28, 35), weight = 100.0, exercise = "changing", configuration = other)
        )
        val summary = project(workouts, configurations = listOf(other)).reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.HOLDING_STEADY, summary.state)
        assertEquals(0.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(1, summary.coverage.contributingExercises)
        assertEquals(setOf("stable"), summary.evidence.map { it.exerciseCatalogId.value }.toSet())
        assertEquals(6, summary.evidence.size)
        assertTrue(summary.explanation.contains("exclud", ignoreCase = true))
        assertTrue(summary.explanation.contains("configuration", ignoreCase = true))
    }

    @Test
    fun workCapacityConfigurationChangeStopsExcludingExerciseOnceOldConfigurationLeavesWindow() {
        val other = configuration("new-external", LoadRole.EXTERNAL_RESISTANCE)
        val workouts = sessions(listOf(0), weight = 100.0) +
            sessions(listOf(70, 77, 84), weight = 20.0, configuration = other) +
            sessions(listOf(91, 98, 105), weight = 25.0, configuration = other)
        val summary = project(workouts, configurations = listOf(other)).reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.RECENT_RANGE_HIGHER, summary.state)
        assertEquals(25.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(6, summary.coverage.qualifyingSessions)
        assertTrue(summary.evidence.all { it.recordedAt >= atDay(70) })
    }

    @Test
    fun workCapacityRawHighVolumeExerciseCannotOutweighTwoStableExercises() {
        val days = listOf(0, 7, 14, 21, 28, 35)
        val workouts = mergeSameDayWorkouts(
            sessions(days, weight = 10.0, exercise = "small-stable") +
                sessions(days, weight = 20.0, exercise = "medium-stable") +
                sessions(listOf(0, 7, 14), weight = 100.0, exercise = "large-growing") +
                sessions(listOf(21, 28, 35), weight = 200.0, exercise = "large-growing")
        )
        val summary = project(workouts).reading(EvidenceReading.WORK_CAPACITY)

        // Equal votes: median(0%, 0%, 100%) is 0%, not the change in summed raw volume.
        assertEquals(ProgressTrendState.HOLDING_STEADY, summary.state)
        assertEquals(0.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(3, summary.coverage.contributingExercises)
        assertEquals(6, summary.coverage.qualifyingSessions)
        assertEquals(18, summary.evidence.size)
    }

    @Test
    fun workCapacityMaturityCannotBeBorrowedAcrossIndividuallySparseExercises() {
        val snapshot = project(
            sessions(listOf(0, 7, 14, 21, 28), exercise = "first") +
                sessions(listOf(3, 10, 17, 24, 31), exercise = "second")
        )
        val summary = snapshot.reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.BUILDING_TREND, summary.state)
        assertNull(summary.changePercent)
        assertNull(summary.comparisonWindow)
        assertTrue(!summary.coverage.isMature)
        assertTrue(summary.exerciseComparisons.isEmpty())
        assertTrue(summary.explanation.contains("per exercise", ignoreCase = true))
        assertTrue(summary.explanation.contains("28"))
    }

    @Test
    fun workCapacitySumsDistinctSetsWithinAnExerciseSessionWithoutChangingCapability() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35)).map { workout ->
            if (workout.finishedAt < atDay(21)) workout else workout.copy(exercises = workout.exercises.map { exercise ->
                val set = exercise.loggedSets.single()
                exercise.copy(loggedSets = listOf(set, set.copy(
                    id = FoundationId("${set.id.value}-second"), position = OrderedPosition(1)
                )))
            })
        }
        val snapshot = project(workouts)
        val summary = snapshot.reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.RECENT_RANGE_HIGHER, summary.state)
        assertEquals(100.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(6, summary.coverage.qualifyingSessions)
        assertEquals(9, summary.evidence.size)
        assertEquals(ProgressTrendState.HOLDING_STEADY, snapshot.reading(EvidenceReading.CAPABILITY).state)
    }

    @Test
    fun workCapacityDuplicateOrUnverifiableSourcesCannotInfluenceTheTrend() {
        val workouts = sessions(listOf(0, 7, 14, 21, 28, 35), exercise = "stable")
        val (records, points) = evidence(workouts)
        val recentVolume = points.filter {
            it.metricCode == ProgressEvidenceMetric.VOLUME.wireCode && it.recordedAt >= atDay(21)
        }
        val invalidPoints = recentVolume.flatMap { point ->
            listOf(
                point.copy(id = FoundationId("duplicate-${point.id.value}")),
                point.copy(sourceSetId = FoundationId("missing-source"), value = 1_000_000.0),
                point.copy(exerciseCatalogId = FoundationId("wrong-exercise"), value = 1_000_000.0),
                point.copy(sourceWorkoutId = FoundationId("wrong-workout"), value = 1_000_000.0),
                point.copy(weight = WeightKg(9_999.0), value = 1_000_000.0),
                point.copy(reps = 99, value = 1_000_000.0),
                point.copy(recordedAt = atDay(34), value = 1_000_000.0),
                point.copy(value = Double.NaN),
                point.copy(value = Double.POSITIVE_INFINITY),
                point.copy(value = -1.0)
            )
        }
        val snapshot = EvidenceLadderUseCase().project(workouts, records, points + invalidPoints)
        val summary = snapshot.reading(EvidenceReading.WORK_CAPACITY)

        assertEquals(ProgressTrendState.HOLDING_STEADY, summary.state)
        assertEquals(0.0, requireNotNull(summary.changePercent), 0.000001)
        assertEquals(6, summary.coverage.qualifyingSessions)
        assertEquals(6, summary.evidence.size)
        assertTrue(summary.evidence.all {
            (it.measurement as? com.jjswigut.oopsallprs.domain.model.ProgressSourceMeasurement.CompletedWork)?.loadRepTotalKg == 200.0
        })
        assertEquals(setOf("stable"), summary.evidence.map { it.exerciseCatalogId.value }.toSet())
    }
}

private fun mergeSameDayWorkouts(workouts: List<CompletedWorkout>): List<CompletedWorkout> =
    workouts.groupBy { it.finishedAt }.values.map { sameDay ->
        val target = sameDay.first()
        target.copy(exercises = sameDay.flatMap { it.exercises }.mapIndexed { index, exercise ->
            exercise.copy(completedWorkoutId = target.id, position = OrderedPosition(index))
        })
    }

private fun EvidenceLadderSnapshot.reading(reading: EvidenceReading) = overallReadings.single { it.reading == reading }

private fun project(
    workouts: List<CompletedWorkout>,
    configurations: List<LoggingConfiguration> = emptyList(),
    asOf: Instant? = workouts.maxOfOrNull { it.finishedAt }
): EvidenceLadderSnapshot {
    val (records, points) = evidence(workouts)
    return EvidenceLadderUseCase().project(workouts, records, points, configurations, asOf)
}

private fun atDay(day: Int) = Instant.fromEpochMilliseconds(day * 86_400_000L)

private fun configuration(id: String, role: LoadRole) = LegacyLoggingConfigurations.weighted.copy(
    id = LoggingConfigurationId(id),
    measures = LegacyLoggingConfigurations.weighted.measures.map {
        if (it.kind == MeasureKind.LOAD) it.copy(loadRole = role) else it
    }
)

private fun sessions(
    days: List<Int>,
    weight: Double = 25.0,
    reps: Int = 8,
    exercise: String = "exercise",
    configuration: LoggingConfiguration = LegacyLoggingConfigurations.weighted
): List<CompletedWorkout> = days.map { day ->
    val workoutId = FoundationId("workout-$exercise-$day")
    val exerciseId = FoundationId("instance-$exercise-$day")
    val set = ExerciseSet(
        id = FoundationId("set-$exercise-$day"),
        exerciseInstanceId = exerciseId,
        position = OrderedPosition(0),
        setKind = SetKind.WEIGHTED,
        weight = WeightKg(weight),
        reps = reps,
        loggedAt = atDay(day),
        createdAt = atDay(day),
        updatedAt = atDay(day),
        captureConfigurationId = configuration.id
    )
    CompletedWorkout(
        id = workoutId,
        sourceActiveWorkoutId = workoutId,
        startedAt = atDay(day),
        finishedAt = atDay(day),
        durationMs = 0,
        routineId = null,
        exercises = listOf(CompletedExercise(
            id = exerciseId,
            completedWorkoutId = workoutId,
            exerciseCatalogId = FoundationId(exercise),
            displayNameSnapshot = exercise,
            position = OrderedPosition(0),
            loggedSets = listOf(set)
        )),
        createdAt = atDay(day)
    )
}

// Deliberately includes legacy/stale estimates for unsupported roles and >12 reps.
private fun evidence(workouts: List<CompletedWorkout>): Pair<List<PersonalRecord>, List<ProgressPoint>> {
    val points = workouts.flatMap { workout ->
        workout.exercises.flatMap { exercise ->
            exercise.loggedSets.flatMap { set ->
                val weight = requireNotNull(set.weight).value
                val reps = requireNotNull(set.reps)
                listOf(
                    ProgressEvidenceMetric.WEIGHT_FOR_REPS to weight,
                    ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX to weight * (1 + reps / 30.0),
                    ProgressEvidenceMetric.VOLUME to weight * reps
                ).map { (metric, value) ->
                    ProgressPoint(
                        id = FoundationId("${set.id.value}-${metric.wireCode.value}"),
                        exerciseCatalogId = exercise.exerciseCatalogId,
                        sourceWorkoutId = workout.id,
                        sourceSetId = set.id,
                        metric = metric.legacyProgressMetric,
                        value = value,
                        weight = set.weight,
                        reps = reps,
                        recordedAt = requireNotNull(set.loggedAt),
                        metricCode = metric.wireCode
                    )
                }
            }
        }
    }
    val records = points.filter { it.metricCode == ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode }.map {
        PersonalRecord(
            id = it.id,
            exerciseCatalogId = it.exerciseCatalogId,
            recordKind = ProgressEvidenceMetric.WEIGHT_FOR_REPS.legacyRecordKind,
            reps = it.reps,
            weight = it.weight,
            value = it.value,
            sourceWorkoutId = it.sourceWorkoutId,
            sourceSetId = requireNotNull(it.sourceSetId),
            achievedAt = it.recordedAt,
            createdAt = it.recordedAt
        )
    }
    return records to points
}
