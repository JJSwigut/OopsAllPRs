package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ProgressionSummary
import com.jjswigut.oopsallprs.domain.model.RecentTrainingReview
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceSource
import com.jjswigut.oopsallprs.domain.model.ProgressSourceMeasurement
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WireCode
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.history.CompletedSetSummary
import com.jjswigut.oopsallprs.ui.history.CompletedWorkoutSummary
import com.jjswigut.oopsallprs.ui.history.formatDurationMs
import com.jjswigut.oopsallprs.ui.history.toSummary
import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.pow

internal fun RecentTrainingReview.factsLabel(): String = buildList {
    add("$completedWorkoutCount ${if (completedWorkoutCount == 1) "workout" else "workouts"}")
    add("$loggedSetCount ${if (loggedSetCount == 1) "set" else "sets"}")
    if (totalDurationMs > 0L) add(recentTrainingDurationLabel(totalDurationMs))
}.joinToString(" \u2022 ")

internal fun recentTrainingDurationLabel(durationMs: Long): String {
    val normalizedDuration = durationMs.coerceAtLeast(0L)
    if (normalizedDuration in 1L..<60_000L) return "<1m"
    val totalMinutes = normalizedDuration / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}

data class ProgressPrRow(
    val recordId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val exerciseName: String,
    val kind: PersonalRecordKind,
    val metricCode: WireCode,
    val kindLabel: String,
    val valueLabel: String,
    val detailLabel: String,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId,
    val achievedAt: Instant,
    val achievedDateLabel: String
)

data class ProgressExerciseGroup(
    val exerciseCatalogId: FoundationId,
    val exerciseName: String,
    val records: List<ProgressPrRow>,
    val latestRecord: ProgressPrRow?,
    val trendRows: List<ProgressTrendRow>,
    val chart: ProgressChartState,
    val latestAchievedAt: Instant?,
    val capability: ProgressionSummary? = null
)

data class ProgressTrendRow(
    val pointId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val metric: ProgressEvidenceMetric,
    val metricLabel: String,
    val valueLabel: String,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId?,
    val sourceRecordId: FoundationId?,
    val recordedAt: Instant
)

data class ProgressEvidence(
    val recordId: FoundationId,
    val title: String,
    val recordKindLabel: String,
    val recordValueLabel: String,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId,
    val workoutSummary: CompletedWorkoutSummary?,
    val sourceExerciseName: String?,
    val sourceSetLabel: String?,
    val achievedDateLabel: String,
    val message: String,
    val isAvailable: Boolean
)

internal fun ProgressionSummary.displayStateLabel(): String =
    if (state == ProgressTrendState.BUILDING_TREND && coverage.isMature) {
        "Need comparable workouts"
    } else {
        state.label
    }

internal fun ProgressEvidenceSource.displayValueLabel(weightUnit: WeightUnit): String =
    when (val measure = measurement) {
        is ProgressSourceMeasurement.EstimatedStrength ->
            "${WeightKg(measure.estimatedOneRepMaxKg).format(weightUnit)} estimated 1RM • " +
                "from ${measure.sourceWeight.format(weightUnit, decimalPlaces = 2)} x ${measure.sourceReps}"
        is ProgressSourceMeasurement.CompletedWork ->
            "${measure.sourceWeight.format(weightUnit, decimalPlaces = 2)} x ${measure.sourceReps} • " +
                "${WeightKg(measure.loadRepTotalKg).format(weightUnit)} load x reps"
        ProgressSourceMeasurement.CompletedSession -> "Completed session"
    }

internal fun buildRecentPrRows(
    records: List<PersonalRecord>,
    workouts: List<CompletedWorkout>,
    weightUnit: WeightUnit
): List<ProgressPrRow> {
    val names = buildExerciseNameLookup(workouts)
    return records
        .sortedWith(
            compareByDescending<PersonalRecord> { it.achievedAt }
                .thenBy { it.exerciseCatalogId.value }
                .thenBy { it.recordKind.name }
        )
        .map { record ->
            record.toProgressPrRow(
                exerciseName = names[record.exerciseCatalogId] ?: fallbackExerciseName(record.exerciseCatalogId),
                weightUnit = weightUnit
            )
        }
}

internal fun buildExerciseGroups(
    records: List<PersonalRecord>,
    points: List<ProgressPoint>,
    workouts: List<CompletedWorkout>,
    weightUnit: WeightUnit,
    capabilityByExercise: Map<FoundationId, ProgressionSummary> = emptyMap()
): List<ProgressExerciseGroup> {
    val names = buildExerciseNameLookup(workouts)
    val sourceRecordByPoint = points.associate { point ->
        point.id to records.firstOrNull { record ->
            record.exerciseCatalogId == point.exerciseCatalogId &&
                record.sourceWorkoutId == point.sourceWorkoutId &&
                record.sourceSetId == point.sourceSetId &&
                record.metricCode == point.metricCode
        }?.id
    }
    val trendRowsByExercise = points
        .filter { it.isDisplayableTrendPoint() }
        .groupBy { it.exerciseCatalogId }
        .mapValues { (_, exercisePoints) ->
            exercisePoints
                .mapNotNull { point -> point.evidenceMetric()?.let { point to it } }
                .sortedWith(compareBy<Pair<ProgressPoint, ProgressEvidenceMetric>> { it.first.recordedAt }
                    .thenBy { it.second.wireCode.value }
                    .thenBy { it.first.id.value })
                .map { (point, metric) ->
                    point.toTrendRow(
                        evidenceMetric = metric,
                        exerciseName = names[point.exerciseCatalogId] ?: fallbackExerciseName(point.exerciseCatalogId),
                        weightUnit = weightUnit,
                        sourceRecordId = sourceRecordByPoint[point.id]
                    )
                }
        }
    val exerciseIds = (records.map { it.exerciseCatalogId } + points.map { it.exerciseCatalogId }).distinct()
    return exerciseIds
        .map { exerciseId ->
            val exerciseRecords = records.filter { it.exerciseCatalogId == exerciseId }
            val exerciseName = names[exerciseId] ?: fallbackExerciseName(exerciseId)
            val rows = exerciseRecords
                .sortedWith(compareBy<PersonalRecord> { it.evidenceMetric()?.sortOrder() ?: Int.MAX_VALUE }
                    .thenBy { it.reps ?: Int.MAX_VALUE })
                .map { it.toProgressPrRow(exerciseName, weightUnit) }
            val chart = buildProgressChartState(
                points = points,
                records = records,
                exerciseCatalogId = exerciseId,
                selectedMetric = null,
                weightUnit = weightUnit
            )
            val latestPointAt = chart.points.maxByOrNull { it.recordedAt }?.recordedAt
            ProgressExerciseGroup(
                exerciseCatalogId = exerciseId,
                exerciseName = exerciseName,
                records = rows,
                latestRecord = rows.maxByOrNull { it.achievedAt },
                trendRows = trendRowsByExercise[exerciseId].orEmpty(),
                chart = chart,
                latestAchievedAt = rows.maxByOrNull { it.achievedAt }?.achievedAt ?: latestPointAt,
                capability = capabilityByExercise[exerciseId]
            )
        }
        .sortedWith(
            compareByDescending<ProgressExerciseGroup> { it.latestAchievedAt?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                .thenBy { it.exerciseName }
        )
}

internal fun buildProgressEvidence(
    record: PersonalRecord,
    records: List<PersonalRecord>,
    workouts: List<CompletedWorkout>,
    weightUnit: WeightUnit
): ProgressEvidence {
    val row = record.toProgressPrRow(
        exerciseName = buildExerciseNameLookup(workouts)[record.exerciseCatalogId]
            ?: fallbackExerciseName(record.exerciseCatalogId),
        weightUnit = weightUnit
    )
    val workout = workouts.firstOrNull { it.id == record.sourceWorkoutId }
    val summary = workout?.toSummary(records)
    val sourceExercise = summary
        ?.exercises
        ?.firstOrNull { exercise -> exercise.setRows.any { it.setId == record.sourceSetId } }
    val sourceSet = sourceExercise?.setRows?.firstOrNull { it.setId == record.sourceSetId }
    val isAvailable = summary != null && sourceExercise != null && sourceSet != null
    return ProgressEvidence(
        recordId = record.id,
        title = row.exerciseName,
        recordKindLabel = row.kindLabel,
        recordValueLabel = row.valueLabel,
        sourceWorkoutId = record.sourceWorkoutId,
        sourceSetId = record.sourceSetId,
        workoutSummary = summary,
        sourceExerciseName = sourceExercise?.displayName,
        sourceSetLabel = sourceSet?.formatSetLabel(weightUnit),
        achievedDateLabel = row.achievedDateLabel,
        message = if (isAvailable) {
            "Recorded from completed workout."
        } else {
            "Source workout or set is no longer available locally."
        },
        isAvailable = isAvailable
    )
}

internal fun PersonalRecord.toProgressPrRow(
    exerciseName: String,
    weightUnit: WeightUnit
): ProgressPrRow =
    ProgressPrRow(
        recordId = id,
        exerciseCatalogId = exerciseCatalogId,
        exerciseName = exerciseName,
        kind = recordKind,
        metricCode = metricCode,
        kindLabel = evidenceMetric()?.recordLabel(reps) ?: "Record",
        valueLabel = valueLabel(weightUnit),
        detailLabel = detailLabel(weightUnit),
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        achievedAt = achievedAt,
        achievedDateLabel = achievedAt.shortDateLabel()
    )

private fun ProgressPoint.toTrendRow(
    evidenceMetric: ProgressEvidenceMetric,
    exerciseName: String,
    weightUnit: WeightUnit,
    sourceRecordId: FoundationId?
): ProgressTrendRow =
    ProgressTrendRow(
        pointId = id,
        exerciseCatalogId = exerciseCatalogId,
        metric = evidenceMetric,
        metricLabel = evidenceMetric.label(),
        valueLabel = valueLabel(evidenceMetric, weightUnit, exerciseName),
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        sourceRecordId = sourceRecordId,
        recordedAt = recordedAt
    )

private fun PersonalRecord.valueLabel(weightUnit: WeightUnit): String =
    when (evidenceMetric()) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> {
            val weightLabel = weight?.format(weightUnit) ?: WeightKg(value).format(weightUnit)
            val repsLabel = reps?.let { " x $it" }.orEmpty()
            "$weightLabel$repsLabel"
        }
        ProgressEvidenceMetric.REPS -> "${(reps ?: value.roundToInt()).coerceAtLeast(0)} reps"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> WeightKg(value).format(weightUnit)
        ProgressEvidenceMetric.VOLUME -> "${WeightKg(value).format(weightUnit)} volume"
        ProgressEvidenceMetric.LONGEST_DURATION -> value.roundToLong().formatDurationMs()
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "${value.formatCompact()} m"
        null -> value.formatCompact()
    }

private fun PersonalRecord.detailLabel(weightUnit: WeightUnit): String =
    when (evidenceMetric()) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> reps?.let { "$it reps" }.orEmpty()
        ProgressEvidenceMetric.REPS -> "Bodyweight"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> sourceSetDetail(weightUnit)
        ProgressEvidenceMetric.VOLUME -> sourceSetDetail(weightUnit)
        ProgressEvidenceMetric.LONGEST_DURATION -> "Time"
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "Distance"
        null -> "Record"
    }

private fun ProgressPoint.valueLabel(
    evidenceMetric: ProgressEvidenceMetric,
    weightUnit: WeightUnit,
    exerciseName: String
): String =
    when (evidenceMetric) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> {
            val weightLabel = weight?.format(weightUnit) ?: WeightKg(value).format(weightUnit)
            val repsLabel = reps?.let { " x $it" }.orEmpty()
            "$weightLabel$repsLabel"
        }
        ProgressEvidenceMetric.REPS -> "${(reps ?: value.roundToInt()).coerceAtLeast(0)} reps"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> "${WeightKg(value).format(weightUnit)} Estimated 1RM"
        ProgressEvidenceMetric.VOLUME -> "${WeightKg(value).format(weightUnit)} volume"
        ProgressEvidenceMetric.LONGEST_DURATION -> value.roundToLong().formatDurationMs()
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "${value.formatCompact()} m"
    } + " • $exerciseName"

private fun PersonalRecord.sourceSetDetail(weightUnit: WeightUnit): String {
    val weightLabel = weight?.format(weightUnit)
    val repsLabel = reps?.let { " x $it" }
    return listOfNotNull(weightLabel, repsLabel).joinToString("")
}

private fun CompletedSetSummary.formatSetLabel(weightUnit: WeightUnit): String {
    val values = listOfNotNull(
        reps?.let { "${it.coerceAtLeast(0)} reps" },
        weight?.format(weightUnit, decimalPlaces = 2),
        durationMs?.formatDurationMs(),
        distanceMeters?.let { "${it.formatCompact()} m" }
    ).joinToString(" • ").ifEmpty { "Performance unavailable" }
    return "Set ${position + 1}: $values"
}

internal fun WeightKg.format(unit: WeightUnit, decimalPlaces: Int = 1): String =
    "${displayValue(unit).formatCompact(decimalPlaces)} ${unit.abbreviation()}"

private fun WeightUnit.abbreviation(): String =
    when (this) {
        WeightUnit.KILOGRAMS -> "kg"
        WeightUnit.POUNDS -> "lb"
    }

private fun Double.formatCompact(decimalPlaces: Int = 1): String {
    val factor = 10.0.pow(decimalPlaces)
    val rounded = round(this * factor) / factor
    val whole = rounded.roundToInt()
    return if (abs(rounded - whole.toDouble()) < 0.0001) whole.toString() else rounded.toString()
}

private fun buildExerciseNameLookup(workouts: List<CompletedWorkout>): Map<FoundationId, String> {
    val names = linkedMapOf<FoundationId, String>()
    workouts.sortedBy { it.finishedAt }.forEach { workout ->
        workout.exercises.forEach { exercise ->
            names[exercise.exerciseCatalogId] = exercise.displayNameSnapshot
        }
    }
    return names
}

private fun fallbackExerciseName(exerciseId: FoundationId): String = "Exercise ${exerciseId.value}"

private fun PersonalRecord.evidenceMetric(): ProgressEvidenceMetric? =
    ProgressEvidenceMetric.fromWireCode(metricCode.value)

private fun ProgressEvidenceMetric.recordLabel(reps: Int?): String =
    when (this) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> "${reps ?: 0}-rep PR"
        ProgressEvidenceMetric.REPS -> "Bodyweight reps"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> "Estimated 1RM"
        ProgressEvidenceMetric.VOLUME -> "Best volume"
        ProgressEvidenceMetric.LONGEST_DURATION -> "Best time"
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "Longest distance"
    }

private fun ProgressPoint.evidenceMetric(): ProgressEvidenceMetric? =
    ProgressEvidenceMetric.fromWireCode(metricCode.value)
