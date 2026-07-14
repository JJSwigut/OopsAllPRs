package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.history.formatDurationMs
import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class ProgressChartState(
    val selectedMetric: ProgressEvidenceMetric?,
    val availableMetrics: List<ProgressEvidenceMetric>,
    val points: List<ProgressChartPoint>,
    val latestValueLabel: String?,
    val emptyMessage: String
) {
    val hasPoints: Boolean = points.isNotEmpty()
}

data class ProgressChartPoint(
    val pointId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val metric: ProgressEvidenceMetric,
    val value: Double,
    val valueLabel: String,
    val dateLabel: String,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId?,
    val sourceRecordId: FoundationId?,
    val recordedAt: Instant
)

internal fun buildProgressChartState(
    points: List<ProgressPoint>,
    records: List<PersonalRecord>,
    exerciseCatalogId: FoundationId,
    selectedMetric: ProgressEvidenceMetric?,
    weightUnit: WeightUnit
): ProgressChartState {
    val exercisePoints = points
        .filter { it.exerciseCatalogId == exerciseCatalogId && it.value.isFinite() }
        .mapNotNull { point -> point.evidenceMetric()?.let { point to it } }
    val availableMetrics = exercisePoints
        .map { it.second }
        .distinct()
        .sortedBy { it.sortOrder() }
    val metric = selectedMetric
        ?.takeIf { it in availableMetrics }
        ?: availableMetrics.defaultMetric()
    val chartPoints = metric
        ?.let { selected ->
            exercisePoints
                .filter { it.second == selected }
                .map { it.first }
                .sortedWith(compareBy<ProgressPoint> { it.recordedAt }.thenBy { it.id.value })
                .map { point -> point.toChartPoint(selected, records, weightUnit) }
        }
        .orEmpty()
    return ProgressChartState(
        selectedMetric = metric,
        availableMetrics = availableMetrics,
        points = chartPoints,
        latestValueLabel = chartPoints.lastOrNull()?.valueLabel,
        emptyMessage = if (availableMetrics.isEmpty()) {
            "Chart points will appear after completed workouts."
        } else {
            "No ${metric?.label().orEmpty()} points yet."
        }
    )
}

internal fun ProgressEvidenceMetric.label(): String =
    when (this) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> "Best set"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> "Estimated 1RM"
        ProgressEvidenceMetric.VOLUME -> "Volume"
        ProgressEvidenceMetric.REPS -> "Bodyweight reps"
        ProgressEvidenceMetric.LONGEST_DURATION -> "Time"
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "Longest distance"
    }

internal fun ProgressEvidenceMetric.shortLabel(): String =
    when (this) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> "Best"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> "e1RM"
        ProgressEvidenceMetric.VOLUME -> "Volume"
        ProgressEvidenceMetric.REPS -> "Reps"
        ProgressEvidenceMetric.LONGEST_DURATION -> "Time"
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "Distance"
    }

internal fun ProgressEvidenceMetric.sortOrder(): Int =
    when (this) {
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> 0
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> 1
        ProgressEvidenceMetric.REPS -> 2
        ProgressEvidenceMetric.LONGEST_DURATION -> 3
        ProgressEvidenceMetric.LONGEST_DISTANCE -> 4
        ProgressEvidenceMetric.VOLUME -> 5
    }

private fun ProgressPoint.toChartPoint(
    evidenceMetric: ProgressEvidenceMetric,
    records: List<PersonalRecord>,
    weightUnit: WeightUnit
): ProgressChartPoint =
    ProgressChartPoint(
        pointId = id,
        exerciseCatalogId = exerciseCatalogId,
        metric = evidenceMetric,
        value = value,
        valueLabel = valueLabel(evidenceMetric, weightUnit),
        dateLabel = recordedAt.shortDateLabel(),
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        sourceRecordId = sourceRecord(records),
        recordedAt = recordedAt
    )

private fun ProgressPoint.valueLabel(evidenceMetric: ProgressEvidenceMetric, weightUnit: WeightUnit): String =
    when (evidenceMetric) {
        ProgressEvidenceMetric.WEIGHT_FOR_REPS -> {
            val weightLabel = (weight ?: WeightKg(value)).format(weightUnit)
            val repsLabel = reps?.let { " x $it" }.orEmpty()
            "$weightLabel$repsLabel"
        }
        ProgressEvidenceMetric.REPS -> "${(reps ?: value.roundToInt()).coerceAtLeast(0)} reps"
        ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX -> "${WeightKg(value).format(weightUnit)} e1RM"
        ProgressEvidenceMetric.VOLUME -> "${WeightKg(value).format(weightUnit)} volume"
        ProgressEvidenceMetric.LONGEST_DURATION -> value.roundToLong().formatDurationMs()
        ProgressEvidenceMetric.LONGEST_DISTANCE -> "${value.formatCompact()} m"
    }

private fun ProgressPoint.sourceRecord(records: List<PersonalRecord>): FoundationId? {
    return records.firstOrNull { record ->
        record.exerciseCatalogId == exerciseCatalogId &&
            record.sourceWorkoutId == sourceWorkoutId &&
            record.sourceSetId == sourceSetId &&
            record.metricCode == metricCode
    }?.id
}

private fun List<ProgressEvidenceMetric>.defaultMetric(): ProgressEvidenceMetric? =
    firstOrNull { it == ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX }
        ?: firstOrNull { it == ProgressEvidenceMetric.WEIGHT_FOR_REPS }
        ?: firstOrNull { it == ProgressEvidenceMetric.REPS }
        ?: firstOrNull { it == ProgressEvidenceMetric.LONGEST_DURATION }
        ?: firstOrNull { it == ProgressEvidenceMetric.LONGEST_DISTANCE }
        ?: firstOrNull()

private fun ProgressPoint.evidenceMetric(): ProgressEvidenceMetric? =
    ProgressEvidenceMetric.fromWireCode(metricCode.value)

private fun Double.formatCompact(): String {
    val oneDecimal = round(this * 10.0) / 10.0
    val whole = oneDecimal.roundToInt()
    return if (abs(oneDecimal - whole.toDouble()) < 0.0001) whole.toString() else oneDecimal.toString()
}
