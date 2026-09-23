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

/**
 * Stable semantic identifiers for derived record and progress evidence.
 *
 * The legacy enum projections remain on the evidence models for source and storage compatibility.
 * New derivation logic must use this wire code as the authoritative metric identity.
 */
enum class ProgressEvidenceMetric(
    code: String,
    val legacyRecordKind: PersonalRecordKind,
    val legacyProgressMetric: ProgressMetric
) : WireCoded {
    WEIGHT_FOR_REPS("weight_for_reps", PersonalRecordKind.WEIGHT_FOR_REPS, ProgressMetric.BEST_SET),
    REPS("reps", PersonalRecordKind.BODYWEIGHT_REPS, ProgressMetric.BODYWEIGHT_REPS),
    ESTIMATED_ONE_REP_MAX(
        "estimated_one_rep_max",
        PersonalRecordKind.ESTIMATED_ONE_REP_MAX,
        ProgressMetric.ESTIMATED_ONE_REP_MAX
    ),
    VOLUME("volume", PersonalRecordKind.VOLUME, ProgressMetric.VOLUME),
    LONGEST_DURATION("longest_duration", PersonalRecordKind.TIME, ProgressMetric.TIME),

    // TIME is the closest scalar legacy projection; metricCode remains authoritative.
    LONGEST_DISTANCE("longest_distance", PersonalRecordKind.TIME, ProgressMetric.TIME);

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "progress evidence metric")

        fun fromWireCode(value: String): ProgressEvidenceMetric? = byWireCode[value]
    }
}

object ProgressDerivationVersions {
    const val LEGACY_SET_KIND: Int = 1
    const val CONFIGURATION_CAPTURE: Int = 2
    const val EVIDENCE_LADDER: Int = 3
    const val CURRENT: Int = EVIDENCE_LADDER
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
    val createdAt: Instant,
    val metricCode: WireCode = recordKind.defaultEvidenceMetric().wireCode,
    val derivationVersion: Int = ProgressDerivationVersions.LEGACY_SET_KIND
) {
    init {
        require(derivationVersion > 0) { "Progress derivation version must be greater than zero" }
    }
}

data class ProgressPoint(
    val id: FoundationId,
    val exerciseCatalogId: FoundationId,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId?,
    val metric: ProgressMetric,
    val value: Double,
    val weight: WeightKg?,
    val reps: Int?,
    val recordedAt: Instant,
    val metricCode: WireCode = metric.defaultEvidenceMetric().wireCode,
    val derivationVersion: Int = ProgressDerivationVersions.LEGACY_SET_KIND
) {
    init {
        require(derivationVersion > 0) { "Progress derivation version must be greater than zero" }
    }
}

private fun PersonalRecordKind.defaultEvidenceMetric(): ProgressEvidenceMetric =
    when (this) {
        PersonalRecordKind.WEIGHT_FOR_REPS -> ProgressEvidenceMetric.WEIGHT_FOR_REPS
        PersonalRecordKind.BODYWEIGHT_REPS -> ProgressEvidenceMetric.REPS
        PersonalRecordKind.ESTIMATED_ONE_REP_MAX -> ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX
        PersonalRecordKind.VOLUME -> ProgressEvidenceMetric.VOLUME
        PersonalRecordKind.TIME -> ProgressEvidenceMetric.LONGEST_DURATION
    }

private fun ProgressMetric.defaultEvidenceMetric(): ProgressEvidenceMetric =
    when (this) {
        ProgressMetric.BEST_SET -> ProgressEvidenceMetric.WEIGHT_FOR_REPS
        ProgressMetric.BODYWEIGHT_REPS -> ProgressEvidenceMetric.REPS
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX
        ProgressMetric.VOLUME -> ProgressEvidenceMetric.VOLUME
        ProgressMetric.TIME -> ProgressEvidenceMetric.LONGEST_DURATION
    }

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
