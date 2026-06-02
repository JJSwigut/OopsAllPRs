package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

enum class PersonalRecordKind {
    WEIGHT_FOR_REPS,
    BODYWEIGHT_REPS,
    ESTIMATED_ONE_REP_MAX,
    VOLUME,
    TIME
}

enum class ProgressMetric {
    BEST_SET,
    ESTIMATED_ONE_REP_MAX,
    VOLUME,
    BODYWEIGHT_REPS,
    TIME
}

enum class ExportType {
    WORKOUTS,
    PERSONAL_RECORDS,
    EXERCISES,
    ROUTINES
}

data class PersonalRecord(
    val id: FoundationId,
    val exerciseCatalogId: FoundationId,
    val recordKind: PersonalRecordKind,
    val reps: Int?,
    val weight: WeightKg?,
    val value: Double,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId,
    val achievedAt: Instant,
    val createdAt: Instant
)

data class ProgressPoint(
    val id: FoundationId,
    val exerciseCatalogId: FoundationId,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId?,
    val metric: ProgressMetric,
    val value: Double,
    val weight: WeightKg?,
    val reps: Int?,
    val recordedAt: Instant
)

data class ExportSnapshot(
    val id: FoundationId,
    val exportType: ExportType,
    val createdAt: Instant,
    val weightUnit: WeightUnit,
    val rowCount: Int,
    val formatVersion: Int = 1
)

data class ExportFile(
    val snapshot: ExportSnapshot,
    val fileName: String,
    val content: String
)
