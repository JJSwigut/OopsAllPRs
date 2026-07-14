package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WireCode
import com.jjswigut.oopsallprs.testing.instant

internal fun progressRecord(
    id: FoundationId = FoundationId("pr-weight"),
    exerciseCatalogId: FoundationId = FoundationId("exercise-bench"),
    recordKind: PersonalRecordKind = PersonalRecordKind.WEIGHT_FOR_REPS,
    reps: Int? = 5,
    weight: WeightKg? = WeightKg(100.0),
    value: Double = 100.0,
    sourceWorkoutId: FoundationId = FoundationId("completed-mixed"),
    sourceSetId: FoundationId = FoundationId("set-weighted"),
    achievedAtMs: Long = 10_000
): PersonalRecord =
    PersonalRecord(
        id = id,
        exerciseCatalogId = exerciseCatalogId,
        recordKind = recordKind,
        reps = reps,
        weight = weight,
        value = value,
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        achievedAt = instant(achievedAtMs),
        createdAt = instant(achievedAtMs)
    )

internal fun progressPoint(
    id: FoundationId,
    exerciseCatalogId: FoundationId = FoundationId("exercise-bench"),
    metric: ProgressMetric = ProgressMetric.BEST_SET,
    value: Double = 100.0,
    weight: WeightKg? = WeightKg(value),
    reps: Int? = 5,
    sourceWorkoutId: FoundationId = FoundationId("completed-mixed"),
    sourceSetId: FoundationId? = FoundationId("set-weighted"),
    recordedAtMs: Long = 10_000,
    metricCode: WireCode? = null
): ProgressPoint =
    ProgressPoint(
        id = id,
        exerciseCatalogId = exerciseCatalogId,
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        metric = metric,
        value = value,
        weight = weight,
        reps = reps,
        recordedAt = instant(recordedAtMs),
        metricCode = metricCode ?: when (metric) {
            ProgressMetric.BEST_SET -> ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode
            ProgressMetric.ESTIMATED_ONE_REP_MAX -> ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX.wireCode
            ProgressMetric.VOLUME -> ProgressEvidenceMetric.VOLUME.wireCode
            ProgressMetric.BODYWEIGHT_REPS -> ProgressEvidenceMetric.REPS.wireCode
            ProgressMetric.TIME -> ProgressEvidenceMetric.LONGEST_DURATION.wireCode
        }
    )
