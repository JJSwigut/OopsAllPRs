package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.WireCode
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.testing.instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistorySummaryPresentationTest {
    @Test
    fun dateAndDateTimeAgreeAcrossUtcAndLocalMidnightBoundaries() {
        val date = Instant.parse("2024-07-03T00:49:00Z")
        val cases = listOf(
            Triple(TimeZone.UTC, "Wed, Jul 3", "2024-07-03 00:49"),
            Triple(TimeZone.of("America/New_York"), "Tue, Jul 2", "2024-07-02 20:49"),
            Triple(TimeZone.of("Asia/Tokyo"), "Wed, Jul 3", "2024-07-03 09:49")
        )
        for ((zone, expectedDate, expectedDateTime) in cases) {
            assertEquals(expectedDate, date.historyDateLabel(zone))
            assertEquals(expectedDateTime, date.historyDateTimeLabel(zone))
        }
        val nextDay = Instant.parse("2024-07-03T23:49:00Z")
        assertEquals("Thu, Jul 4", nextDay.historyDateLabel(TimeZone.of("Asia/Tokyo")))
        assertEquals("2024-07-04 08:49", nextDay.historyDateTimeLabel(TimeZone.of("Asia/Tokyo")))
    }

    @Test
    fun historyListUsesLocalExerciseAndDateLabels() {
        val workout = mixedCompletedWorkout().copy(finishedAt = Instant.parse("2024-07-03T00:49:00Z"))
        val row = listOf(workout).toHistoryRows().single()
        assertEquals("Bench Press + Pull-Up", row.title)
        assertEquals(workout.finishedAt.historyListDateLabel(), row.dateLabel)
    }

    @Test
    fun historyListDateAndTitleAreCompactAndDeterministic() {
        val date = Instant.parse("2024-07-03T00:49:00Z")
        assertEquals("Tue, Jul 2 at 8:49 PM", date.historyListDateLabel(TimeZone.of("America/New_York")))
        assertEquals("Wed, Jul 3 at 9:49 AM", date.historyListDateLabel(TimeZone.of("Asia/Tokyo")))

        val threeExercises = mixedCompletedWorkout().copy(
            exercises = mixedCompletedWorkout().exercises + mixedCompletedWorkout().exercises.first().copy(
                id = FoundationId("completed-exercise-row"),
                displayNameSnapshot = "Row",
                position = OrderedPosition(2)
            )
        )
        assertEquals("Bench Press + Pull-Up +1", threeExercises.historyWorkoutTitle())
    }

    @Test
    fun countsUseExplicitSingularAndPluralIncludingPersonalRecords() {
        assertEquals("0 exercises", historyCountLabel(0, "exercise"))
        assertEquals("1 exercise", historyCountLabel(1, "exercise"))
        assertEquals("2 exercises", historyCountLabel(2, "exercise"))
        assertEquals("1 set", historyCountLabel(1, "set"))
        assertEquals("1 rep", historyCountLabel(1, "rep"))
        assertEquals("1 PR", historyCountLabel(1, "PR", "PRs"))
        assertEquals("3 PRs", historyCountLabel(3, "PR", "PRs"))
    }

    @Test
    fun performanceLabelPreservesFractionalLoadAndSingularRepInSelectedUnit() {
        val workout = weightedWorkout("fractional", 1.25, 1)
        val row = workout.toSummary().exercises.single().setRows.single()
        assertEquals("Set 1: 1 rep • 1.25 kg", row.historyPerformanceLabel(WeightUnit.KILOGRAMS))
        assertEquals("Set 1: 1 rep • 2.76 lb", row.historyPerformanceLabel(WeightUnit.POUNDS))
        assertEquals(WeightKg(1.25), row.weight)
        val marker = workout.toSummary(listOf(record(workout))).exercises.single().setRows.single().prMarkers.single()
        assertEquals("1-rep PR 1.25 kg x 1", marker.historyLabel(WeightUnit.KILOGRAMS))
        assertEquals("1-rep PR 2.76 lb x 1", marker.historyLabel(WeightUnit.POUNDS))
    }

    @Test
    fun sparseStoredPositionsAreSortedButNeverRenumbered() {
        val workout = weightedWorkout("sparse", 20.0, 8)
        val exercise = workout.exercises.single()
        val base = exercise.loggedSets.single()
        val positions = listOf(3, 1, 6)
        val sets = positions.map { base.copy(id = FoundationId("set-$it"), position = OrderedPosition(it)) }
        val summary = workout.copy(exercises = listOf(exercise.copy(loggedSets = sets))).toSummary()
        val rows = summary.exercises.single().setRows
        assertEquals(listOf(1, 3, 6), rows.map { it.position })
        assertEquals(listOf("Set 2:", "Set 4:", "Set 7:"), rows.map {
            it.historyPerformanceLabel(WeightUnit.KILOGRAMS).substringBefore(":") + ":"
        })
        assertEquals(positions, sets.map { it.position.value })
    }

    @Test
    fun threeDistinctMetricsSurviveDuplicateRecordsWithoutRepeatedDatesInPerformance() {
        val workout = weightedWorkout("bench", 100.0, 5)
        val records = listOf(
            record(workout),
            record(workout, ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX, 116.666666),
            record(workout, ProgressEvidenceMetric.VOLUME, 500.0)
        )
        val summary = workout.toSummary(records + records.map { it.copy(id = FoundationId("duplicate-${it.id.value}")) })
        val row = summary.exercises.single().setRows.single()
        assertEquals(3, summary.prCount)
        assertEquals(records.map { it.metricCode }, row.prMarkers.map { it.metricCode })
        assertEquals("Set 1: 5 reps • 100 kg", row.historyPerformanceLabel(WeightUnit.KILOGRAMS))
        assertFalse(row.historyPerformanceLabel(WeightUnit.KILOGRAMS).contains("PR"))
        assertTrue(row.prMarkers.all { !it.historyLabel(WeightUnit.KILOGRAMS).contains("1970-") })
        assertTrue(row.historyDisplayLabel(WeightUnit.KILOGRAMS).contains("PR"))
        assertEquals(records.map { it.id }, row.prMarkers.map { it.recordId })
        assertTrue(row.prMarkers.all { it.sourceWorkoutId == workout.id && it.sourceSetId == row.setId })
        assertTrue(row.prMarkers.all { it.exerciseCatalogId == workout.exercises.single().exerciseCatalogId })
        assertTrue(row.prMarkers.all { it.captureConfigurationId == workout.exercises.single().loggedSets.single().captureConfigurationId })
        assertEquals(records.map { it.achievedAt }, row.prMarkers.map { it.achievedAt })
    }

    @Test
    fun deduplicationDoesNotCollapseDifferentRepsValuesOrWeights() {
        val workout = weightedWorkout("identity", 20.0, 8)
        val base = record(workout)
        val distinct = listOf(base, base.copy(reps = 7), base.copy(value = 21.0), base.copy(weight = WeightKg(21.0)))
        assertEquals(4, workout.toSummary(distinct + distinct).prCount)
    }

    @Test
    fun equalValuesOnDifferentSourceSetsRemainDistinctAchievements() {
        val workout = weightedWorkout("separate-sets", 20.0, 8)
        val exercise = workout.exercises.single()
        val first = exercise.loggedSets.single()
        val second = first.copy(id = FoundationId("second-set"), position = OrderedPosition(1))
        val base = record(workout)
        val records = listOf(base, base.copy(id = FoundationId("second-record"), sourceSetId = second.id))
        val summary = workout.copy(exercises = listOf(exercise.copy(loggedSets = listOf(first, second))))
            .toSummary(records + records)
        assertEquals(2, summary.prCount)
        assertEquals(listOf(first.id, second.id), summary.exercises.single().setRows.map { it.prMarkers.single().sourceSetId })
    }

    @Test
    fun typedMetricLabelsKeepEstimatesWorkDurationAndDistanceDistinct() {
        val workout = weightedWorkout("typed", 20.0, 8)
        val records = listOf(
            record(workout, ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX, 25.333333),
            record(workout, ProgressEvidenceMetric.VOLUME, 160.0),
            record(workout, ProgressEvidenceMetric.LONGEST_DURATION, 60_000.0),
            record(workout, ProgressEvidenceMetric.LONGEST_DISTANCE, 60_000.0)
        )
        val markers = workout.toSummary(records).exercises.single().setRows.single().prMarkers
        assertEquals(4, markers.size)
        assertEquals(listOf("Estimated 1RM PR 25.3 kg", "PR volume 160 kg·reps", "PR 1:00", "PR 60000 m"),
            markers.map { it.historyLabel(WeightUnit.KILOGRAMS) })
        assertEquals("PR volume 352.7 lb·reps", markers[1].historyLabel(WeightUnit.POUNDS))
    }

    @Test
    fun twentyForEightRemainsARepSpecificRecordAfterTwentyFiveForSeven() = runTest {
        val earlier = weightedWorkout("earlier", 25.0, 7, 1_000)
        val later = weightedWorkout("later", 20.0, 8, 3_000)
        val records = PersonalRecordDerivationUseCase(InMemoryFoundationStore()).snapshotFrom(listOf(earlier, later)).records
        val summary = later.toSummary(records)
        assertEquals(1, summary.prCount)
        val row = summary.exercises.single().setRows.single()
        assertEquals("Set 1: 8 reps • 20 kg", row.historyPerformanceLabel(WeightUnit.KILOGRAMS))
        assertEquals(ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode, row.prMarkers.single().metricCode)
        assertEquals("8-rep PR 20 kg x 8", row.prMarkers.single().historyLabel(WeightUnit.KILOGRAMS))
    }

    @Test
    fun unknownMetricsAndMismatchedSourcesDoNotFabricateAchievements() {
        val workout = weightedWorkout("sources", 20.0, 8)
        val base = record(workout)
        val invalid = listOf(
            base.copy(metricCode = WireCode("future_metric")),
            base.copy(sourceWorkoutId = FoundationId("another-workout")),
            base.copy(exerciseCatalogId = FoundationId("another-exercise")),
            base.copy(sourceSetId = FoundationId("another-set"))
        )
        assertEquals(0, workout.toSummary(invalid).prCount)
        assertTrue(workout.toSummary(invalid).exercises.single().setRows.single().prMarkers.isEmpty())
    }

    @Test
    fun noRecordsAndEmptyWorkoutKeepHonestCounts() {
        val workout = weightedWorkout("none", 20.0, 8)
        val summary = workout.toSummary()
        assertEquals(1, summary.exerciseCount)
        assertEquals(1, summary.setCount)
        assertEquals(0, summary.prCount)
        val row = summary.exercises.single().setRows.single()
        assertEquals(row.historyPerformanceLabel(WeightUnit.KILOGRAMS), row.historyDisplayLabel(WeightUnit.KILOGRAMS))
        val empty = workout.copy(exercises = emptyList()).toSummary(listOf(record(workout)))
        assertEquals(0, empty.exerciseCount)
        assertEquals(0, empty.setCount)
        assertEquals(0, empty.prCount)
    }

    private fun weightedWorkout(id: String, kg: Double, reps: Int, time: Long = 1_000): CompletedWorkout {
        val base = mixedCompletedWorkout()
        val workoutId = FoundationId(id)
        val exercise = base.exercises.first()
        val set = exercise.loggedSets.single().copy(
            id = FoundationId("set-$id"), weight = WeightKg(kg), reps = reps,
            loggedAt = instant(time), createdAt = instant(time), updatedAt = instant(time)
        )
        return base.copy(
            id = workoutId, startedAt = instant(time), finishedAt = instant(time + 1_000),
            durationMs = 1_000, exercises = listOf(exercise.copy(completedWorkoutId = workoutId, loggedSets = listOf(set)))
        )
    }

    private fun record(
        workout: CompletedWorkout,
        metric: ProgressEvidenceMetric = ProgressEvidenceMetric.WEIGHT_FOR_REPS,
        value: Double = workout.exercises.single().loggedSets.single().weight!!.value
    ): PersonalRecord {
        val exercise = workout.exercises.single()
        val set = exercise.loggedSets.single()
        val loggedAt = requireNotNull(set.loggedAt)
        return PersonalRecord(
            id = FoundationId("record-${workout.id.value}-${metric.wireCode.value}"),
            exerciseCatalogId = exercise.exerciseCatalogId, recordKind = metric.legacyRecordKind,
            reps = set.reps, weight = set.weight, value = value,
            sourceWorkoutId = workout.id, sourceSetId = set.id,
            achievedAt = loggedAt, createdAt = loggedAt, metricCode = metric.wireCode
        )
    }
}
