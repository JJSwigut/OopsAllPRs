package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressStateHolderTest {
    @Test
    fun recentTrainingUsesLocalTrailingSevenDayWindowAndKeepsSourceBackedRecords() = runTest {
        val harness = FoundationHarness()
        val now = Instant.parse("2026-09-21T12:00:00Z")
        harness.finishWeightedWorkout(weightKg = 20.0, loggedAtMs = Instant.parse("2026-09-15T00:00:00Z").toEpochMilliseconds() - 500, finishedAtMs = Instant.parse("2026-09-15T00:00:00Z").toEpochMilliseconds())
        harness.finishWeightedWorkout(weightKg = 25.0, loggedAtMs = Instant.parse("2026-09-21T23:58:00Z").toEpochMilliseconds(), finishedAtMs = Instant.parse("2026-09-21T23:59:00Z").toEpochMilliseconds())
        harness.finishWeightedWorkout(weightKg = 30.0, loggedAtMs = Instant.parse("2026-09-22T00:00:00Z").toEpochMilliseconds(), finishedAtMs = Instant.parse("2026-09-22T00:01:00Z").toEpochMilliseconds())
        val holder = ProgressStateHolder(
            harness.store,
            harness.store,
            harness.store,
            clock = { now },
            timeZone = { TimeZone.UTC }
        )

        holder.refresh()

        val review = requireNotNull(holder.state.value.recentTrainingReview)
        assertEquals(2, review.completedWorkoutCount)
        assertEquals(2, review.loggedSetCount)
        assertEquals(60_500L, review.totalDurationMs)
        assertTrue(review.personalRecords.isNotEmpty())
        assertTrue(review.personalRecords.all { record ->
            review.completedWorkouts.any { workout -> workout.id == record.sourceWorkoutId }
        })
        assertEquals("2 workouts \u2022 2 sets \u2022 1m", review.factsLabel())

        holder.openRecentTrainingRecords()
        assertTrue(holder.state.value.isRecentTrainingRecordsOpen)
        holder.openEvidence(review.personalRecords.first().id)
        assertNotNull(holder.state.value.selectedEvidence)
        holder.clearEvidence()
        assertTrue(holder.state.value.isRecentTrainingRecordsOpen)
        holder.clearRecentTrainingRecords()
        assertTrue(!holder.state.value.isRecentTrainingRecordsOpen)
    }

    @Test
    fun recentTrainingUsesLocalMidnightsAcrossDaylightSavingChange() {
        val review = com.jjswigut.oopsallprs.domain.usecase.RecentTrainingReviewUseCase()
            .windowAt(Instant.parse("2026-03-08T16:00:00Z"), TimeZone.of("America/New_York"))

        assertEquals(Instant.parse("2026-03-02T05:00:00Z"), review.startInclusive)
        assertEquals(Instant.parse("2026-03-09T04:00:00Z"), review.endExclusive)
    }

    @Test
    fun recentTrainingFormatsSubMinuteCompletedDurationWithoutClaimingZeroMinutes() {
        assertEquals("<1m", recentTrainingDurationLabel(1L))
        assertEquals("<1m", recentTrainingDurationLabel(59_999L))
        assertEquals("1m", recentTrainingDurationLabel(60_000L))
    }

    @Test
    fun savedCustomConfigurationIsUsedForCapabilityEvidence() = runTest {
        val harness = FoundationHarness()
        val configuration = LegacyLoggingConfigurations.weighted.copy(id = LoggingConfigurationId("custom-weighted"))
        harness.store.saveLoggingConfiguration(configuration).successValue()
        listOf(0L, 7L, 14L, 21L, 28L, 35L).forEachIndexed { index, day ->
            harness.finishWeightedWorkout(
                weightKg = 20.0 + index,
                loggedAtMs = 1_300 + day * 86_400_000L,
                finishedAtMs = 2_000 + day * 86_400_000L
            )
        }
        val capturedWorkouts = harness.store.completedWorkouts().map { workout ->
            workout.copy(exercises = workout.exercises.map { exercise ->
                exercise.copy(loggedSets = exercise.loggedSets.map { set ->
                    set.copy(captureConfigurationId = configuration.id)
                })
            })
        }
        val workouts = object : WorkoutRepository by harness.store {
            override suspend fun completedWorkouts() = capturedWorkouts
        }
        val holder = ProgressStateHolder(harness.store, workouts, harness.store,
            loggingConfigurations = harness.store)

        holder.refresh()

        val capability = holder.state.value.exerciseGroups.single().capability
        assertEquals(ProgressTrendState.RECENT_RANGE_HIGHER, capability?.state)
        assertEquals(6, capability?.evidence?.size)
    }

    @Test
    fun overviewExposesThreeInspectableEvidenceReadings() = runTest {
        val harness = FoundationHarness()
        listOf(0L, 7L, 14L, 21L, 28L, 35L).forEachIndexed { index, day ->
            harness.finishWeightedWorkout(
                weightKg = 20.0 + index,
                loggedAtMs = 1_300 + day * 86_400_000L,
                finishedAtMs = 2_000 + day * 86_400_000L
            )
        }
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()

        assertEquals(EvidenceReading.entries.toSet(), holder.state.value.overallReadings.map { it.reading }.toSet())
        assertTrue(holder.state.value.exerciseGroups.single().capability?.coverage?.isMature == true)

        holder.selectReading(EvidenceReading.WORK_CAPACITY)
        assertEquals(EvidenceReading.WORK_CAPACITY, holder.state.value.selectedReading?.reading)
        assertTrue(holder.state.value.selectedReading?.evidence.orEmpty().all { it.sourceSetId != null })
        val comparisons = holder.state.value.selectedReading?.exerciseComparisons.orEmpty()
        assertEquals(1, comparisons.size)
        assertEquals(harness.weightedReference.exerciseCatalogId, comparisons.single().exerciseCatalogId)
        holder.refresh()
        assertEquals(comparisons, holder.state.value.selectedReading?.exerciseComparisons)

        holder.clearReading()
        assertNull(holder.state.value.selectedReading)
    }

    @Test
    fun dashboardShowsEveryExactRepSeriesAchievement() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 16, WeightKg(25.0), 0, instant(1_200))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 14, WeightKg(25.0), 1, instant(1_300))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 10, WeightKg(30.0), 2, instant(1_400))
        harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()

        val weightedRows = holder.state.value.recentRows.filter { it.kind == PersonalRecordKind.WEIGHT_FOR_REPS }
        assertEquals(
            setOf("55.1 lb x 16", "55.1 lb x 14", "66.1 lb x 10"),
            weightedRows.map { it.valueLabel }.toSet()
        )
        assertEquals(setOf("16-rep PR", "14-rep PR", "10-rep PR"), weightedRows.map { it.kindLabel }.toSet())
    }

    @Test
    fun refreshBuildsEmptyStateAndRecentRowsNewestFirst() = runTest {
        val harness = FoundationHarness()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()

        assertTrue(holder.state.value.recentRows.isEmpty())
        assertEquals("Finish workouts to build PRs here.", holder.state.value.emptyMessage)

        harness.finishWeightedWorkout(loggedAtMs = 1_300, finishedAtMs = 2_000)
        harness.finishBodyweightWorkout(reps = 15, loggedAtMs = 3_300, finishedAtMs = 4_000)
        holder.refresh()

        val state = holder.state.value
        assertEquals(4, state.recentRows.size)
        assertEquals("Pull-Up", state.recentRows.first().exerciseName)
        assertEquals("15 reps", state.recentRows.first().valueLabel)
        assertEquals("Pull-Up", state.latestPr?.exerciseName)
        assertEquals("15 reps", state.latestPr?.valueLabel)
    }

    @Test
    fun groupsRecordsByExerciseAndKeepsSelectionInState() = runTest {
        val harness = FoundationHarness()
        harness.finishWeightedWorkout(loggedAtMs = 1_300, finishedAtMs = 2_000)
        harness.finishBodyweightWorkout(reps = 15, loggedAtMs = 3_300, finishedAtMs = 4_000)
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()

        holder.selectExercise(harness.weightedReference.exerciseCatalogId)

        val selected = holder.state.value.selectedExercise
        assertEquals("Bench Press", selected?.exerciseName)
        assertEquals(3, selected?.records?.size)
        assertEquals("Bench Press", selected?.latestRecord?.exerciseName)
        assertTrue(selected?.records.orEmpty().all { it.exerciseName == "Bench Press" })

        holder.clearExerciseSelection()

        assertNull(holder.state.value.selectedExercise)
        assertNull(holder.state.value.selectedExerciseId)
    }
}

private suspend fun FoundationHarness.finishWeightedWorkout(
    reps: Int = 5,
    weightKg: Double = 100.0,
    loggedAtMs: Long,
    finishedAtMs: Long
) {
    val workout = lifecycle.startEmpty(instant(loggedAtMs - 300)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(loggedAtMs - 200)).successValue()
    setLogging.confirmSet(
        workout.id,
        exercise.id,
        SetKind.WEIGHTED,
        reps,
        WeightKg(weightKg),
        0,
        instant(loggedAtMs)
    ).successValue()
    routines.finishWorkout(workout.id, instant(finishedAtMs)).successValue().workout
}

private suspend fun FoundationHarness.finishBodyweightWorkout(
    reps: Int,
    loggedAtMs: Long,
    finishedAtMs: Long
) {
    val workout = lifecycle.startEmpty(instant(loggedAtMs - 300)).successValue()
    val exercise = setLogging.addExercise(workout.id, bodyweightReference, instant(loggedAtMs - 200)).successValue()
    setLogging.confirmSet(
        workout.id,
        exercise.id,
        SetKind.BODYWEIGHT,
        reps,
        null,
        0,
        instant(loggedAtMs)
    ).successValue()
    routines.finishWorkout(workout.id, instant(finishedAtMs)).successValue().workout
}
