package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

data class AchievementSeries(
    val metric: ProgressEvidenceMetric,
    val reps: Int? = null,
    val captureConfigurationId: LoggingConfigurationId? = null
) {
    init {
        require(metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS || reps == null) {
            "Only load-for-reps achievements have an exact repetition qualifier"
        }
        require(reps == null || reps > 0) { "Achievement repetitions must be positive" }
    }
}

data class PersonalRecordAchievement(
    val id: FoundationId,
    val exerciseCatalogId: FoundationId,
    val series: AchievementSeries,
    val value: Double,
    val weight: WeightKg?,
    val reps: Int?,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId,
    val achievedAt: Instant,
    val derivationVersion: Int
)

data class CurrentBenchmark(
    val series: AchievementSeries,
    val achievement: PersonalRecordAchievement
)

enum class EvidenceReading {
    CAPABILITY,
    WORK_CAPACITY,
    CONSISTENCY
}

enum class ProgressTrendState(val label: String) {
    BUILDING_TREND("Building a trend"),
    RECENT_RANGE_HIGHER("Recent range is higher"),
    HOLDING_STEADY("Holding steady"),
    REBUILDING("Rebuilding"),
    STEADY("Steady")
}

/** Coverage categories, not statistical confidence or physiological certainty. */
enum class EvidenceConfidence {
    LOW,
    MEDIUM,
    HIGH
}

data class EvidenceCoverage(
    val qualifyingSessions: Int,
    val spanDays: Int,
    val contributingExercises: Int,
    val minimumSessions: Int = 6,
    val minimumSpanDays: Int = 28
) {
    val isMature: Boolean = qualifyingSessions >= minimumSessions && spanDays >= minimumSpanDays
}

data class ProgressComparisonWindow(
    val earlierStart: Instant,
    val earlierEnd: Instant,
    val recentStart: Instant,
    val recentEnd: Instant
)

data class ProgressEvidenceSource(
    val exerciseCatalogId: FoundationId,
    val exerciseName: String,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId?,
    val recordedAt: Instant,
    val measurement: ProgressSourceMeasurement
)

sealed interface ProgressSourceMeasurement {
    data class EstimatedStrength(
        val estimatedOneRepMaxKg: Double,
        val sourceWeight: WeightKg,
        val sourceReps: Int
    ) : ProgressSourceMeasurement

    data class CompletedWork(
        val loadRepTotalKg: Double,
        val sourceWeight: WeightKg,
        val sourceReps: Int
    ) : ProgressSourceMeasurement

    data object CompletedSession : ProgressSourceMeasurement
}

data class CapabilityPoint(
    val exerciseCatalogId: FoundationId,
    val exerciseName: String,
    val sourceWorkoutId: FoundationId,
    val sourceSetId: FoundationId,
    val recordedAt: Instant,
    val estimatedOneRepMaxKg: Double,
    val sourceWeight: WeightKg,
    val sourceReps: Int,
    val captureConfigurationId: LoggingConfigurationId? = null
)

data class ProgressionSummary(
    val reading: EvidenceReading,
    val state: ProgressTrendState,
    val changePercent: Double?,
    val confidence: EvidenceConfidence,
    val coverage: EvidenceCoverage,
    // For aggregates this is an envelope; exerciseComparisons holds the actual partitions.
    val comparisonWindow: ProgressComparisonWindow?,
    val evidence: List<ProgressEvidenceSource>,
    val explanation: String,
    val exerciseComparisons: List<ExerciseEvidenceComparison> = emptyList()
)

data class ExerciseEvidenceComparison(
    val exerciseCatalogId: FoundationId,
    val exerciseName: String,
    val changePercent: Double,
    val comparisonWindow: ProgressComparisonWindow
)

data class ExerciseProgression(
    val exerciseCatalogId: FoundationId,
    val exerciseName: String,
    val achievements: List<PersonalRecordAchievement>,
    val currentBenchmarks: List<CurrentBenchmark>,
    val capabilityPoints: List<CapabilityPoint>,
    val capability: ProgressionSummary
)

data class EvidenceLadderSnapshot(
    val achievements: List<PersonalRecordAchievement>,
    val currentBenchmarks: List<CurrentBenchmark>,
    val capabilityPoints: List<CapabilityPoint> = emptyList(),
    val exerciseProgressions: List<ExerciseProgression> = emptyList(),
    val overallReadings: List<ProgressionSummary> = emptyList()
)
