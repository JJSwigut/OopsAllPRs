package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.history.formatDurationMs
import kotlinx.datetime.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class ProgressChartState(
    val selectedMetric: ProgressMetric?,
    val availableMetrics: List<ProgressMetric>,
    val points: List<ProgressChartPoint>,
    val latestValueLabel: String?,
    val emptyMessage: String
) {
    val hasPoints: Boolean = points.isNotEmpty()
}

data class ProgressChartPoint(
    val pointId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val metric: ProgressMetric,
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
    selectedMetric: ProgressMetric?,
    weightUnit: WeightUnit
): ProgressChartState {
    val exercisePoints = points
        .filter { it.exerciseCatalogId == exerciseCatalogId && it.value.isFinite() }
    val availableMetrics = exercisePoints
        .map { it.metric }
        .distinct()
        .sortedBy { it.sortOrder() }
    val metric = selectedMetric
        ?.takeIf { it in availableMetrics }
        ?: availableMetrics.defaultMetric()
    val chartPoints = metric
        ?.let { selected ->
            exercisePoints
                .filter { it.metric == selected }
                .sortedWith(compareBy<ProgressPoint> { it.recordedAt }.thenBy { it.id.value })
                .map { point -> point.toChartPoint(records, weightUnit) }
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

internal fun ProgressMetric.label(): String =
    when (this) {
        ProgressMetric.BEST_SET -> "Best set"
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> "Estimated 1RM"
        ProgressMetric.VOLUME -> "Volume"
        ProgressMetric.BODYWEIGHT_REPS -> "Bodyweight reps"
        ProgressMetric.TIME -> "Time"
    }

internal fun ProgressMetric.shortLabel(): String =
    when (this) {
        ProgressMetric.BEST_SET -> "Best"
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> "e1RM"
        ProgressMetric.VOLUME -> "Volume"
        ProgressMetric.BODYWEIGHT_REPS -> "Reps"
        ProgressMetric.TIME -> "Time"
    }

internal fun ProgressMetric.sortOrder(): Int =
    when (this) {
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> 0
        ProgressMetric.BEST_SET -> 1
        ProgressMetric.BODYWEIGHT_REPS -> 2
        ProgressMetric.TIME -> 3
        ProgressMetric.VOLUME -> 4
    }

private fun ProgressPoint.toChartPoint(
    records: List<PersonalRecord>,
    weightUnit: WeightUnit
): ProgressChartPoint =
    ProgressChartPoint(
        pointId = id,
        exerciseCatalogId = exerciseCatalogId,
        metric = metric,
        value = value,
        valueLabel = valueLabel(weightUnit),
        dateLabel = recordedAt.shortDateLabel(),
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        sourceRecordId = sourceRecord(records),
        recordedAt = recordedAt
    )

private fun ProgressPoint.valueLabel(weightUnit: WeightUnit): String =
    when (metric) {
        ProgressMetric.BEST_SET -> {
            val weightLabel = (weight ?: WeightKg(value)).format(weightUnit)
            val repsLabel = reps?.let { " x $it" }.orEmpty()
            "$weightLabel$repsLabel"
        }
        ProgressMetric.BODYWEIGHT_REPS -> "${(reps ?: value.roundToInt()).coerceAtLeast(0)} reps"
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> "${WeightKg(value).format(weightUnit)} e1RM"
        ProgressMetric.VOLUME -> "${WeightKg(value).format(weightUnit)} volume"
        ProgressMetric.TIME -> value.roundToLong().formatDurationMs()
    }

private fun ProgressPoint.sourceRecord(records: List<PersonalRecord>): FoundationId? {
    val expectedKind = metric.toRecordKind()
    return records.firstOrNull { record ->
        record.exerciseCatalogId == exerciseCatalogId &&
            record.sourceWorkoutId == sourceWorkoutId &&
            record.sourceSetId == sourceSetId &&
            record.recordKind == expectedKind
    }?.id
}

private fun ProgressMetric.toRecordKind(): PersonalRecordKind =
    when (this) {
        ProgressMetric.BEST_SET -> PersonalRecordKind.WEIGHT_FOR_REPS
        ProgressMetric.BODYWEIGHT_REPS -> PersonalRecordKind.BODYWEIGHT_REPS
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> PersonalRecordKind.ESTIMATED_ONE_REP_MAX
        ProgressMetric.VOLUME -> PersonalRecordKind.VOLUME
        ProgressMetric.TIME -> PersonalRecordKind.TIME
    }

private fun List<ProgressMetric>.defaultMetric(): ProgressMetric? =
    firstOrNull { it == ProgressMetric.ESTIMATED_ONE_REP_MAX }
        ?: firstOrNull { it == ProgressMetric.BEST_SET }
        ?: firstOrNull { it == ProgressMetric.BODYWEIGHT_REPS }
        ?: firstOrNull { it == ProgressMetric.TIME }
        ?: firstOrNull()
