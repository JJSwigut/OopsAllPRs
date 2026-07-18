package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressDerivationVersions
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WireCode
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class PersonalRecordDerivationUseCase(
    private val progressRepository: ProgressRepository,
    private val loggingConfigurationRepository: LoggingConfigurationRepository? =
        progressRepository as? LoggingConfigurationRepository
) {
    suspend fun rebuildFrom(workouts: List<CompletedWorkout>) {
        val snapshot = snapshotFrom(workouts)
        progressRepository.replaceRecords(snapshot.records, snapshot.points)
    }

    suspend fun snapshotFrom(workouts: List<CompletedWorkout>): DerivedProgressSnapshot {
        val configurationCache = mutableMapOf<LoggingConfigurationId, LoggingConfiguration?>()
        val points = mutableListOf<ProgressPoint>()
        for (workout in workouts) {
            for (exercise in workout.exercises) {
                for (set in exercise.loggedSets) {
                    val configuration = configurationCache.getOrPutSuspending(set.captureConfigurationId) {
                        resolveConfiguration(set.captureConfigurationId)
                    } ?: continue
                    points += SetProgressEvidenceDerivation.derive(set, configuration).map { evidence ->
                        evidence.toProgressPoint(
                            exerciseCatalogId = exercise.exerciseCatalogId,
                            sourceWorkoutId = workout.id,
                            fallbackRecordedAt = workout.finishedAt
                        )
                    }
                }
            }
        }

        val weightedRecords = points
            .filter { it.metricCode == ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode }
            .groupBy { it.exerciseCatalogId }
            .values
            .flatMap { exercisePoints ->
                exercisePoints.nondominatedWeightedSets { point ->
                    WeightedSetPerformance(
                        weightKg = point.weight?.value ?: point.value,
                        reps = point.reps ?: 0
                    )
                }
            }
            .mapNotNull { it.toPersonalRecord() }

        val scalarRecords = points
            .filter { it.metricCode != ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode }
            .groupBy { it.recordBucket() }
            .mapNotNull { (_, values) -> values.maxByOrNull(ProgressPoint::value)?.toPersonalRecord() }

        return DerivedProgressSnapshot(weightedRecords + scalarRecords, points)
    }

    private suspend fun resolveConfiguration(id: LoggingConfigurationId): LoggingConfiguration? =
        loggingConfigurationRepository?.loggingConfiguration(id)
            ?: LegacyLoggingConfigurations.all.firstOrNull { it.id == id }

    private fun ProgressPoint.toPersonalRecord(): PersonalRecord? {
        val recordSourceSetId = sourceSetId ?: return null
        val evidenceMetric = ProgressEvidenceMetric.fromWireCode(metricCode.value) ?: return null
        return PersonalRecord(
            id = newFoundationId("pr"),
            exerciseCatalogId = exerciseCatalogId,
            recordKind = evidenceMetric.legacyRecordKind,
            reps = reps,
            weight = weight,
            value = value,
            sourceWorkoutId = sourceWorkoutId,
            sourceSetId = recordSourceSetId,
            achievedAt = recordedAt,
            createdAt = Clock.System.now(),
            metricCode = metricCode,
            derivationVersion = derivationVersion
        )
    }

    private fun ProgressPoint.recordBucket(): RecordBucket =
        RecordBucket(exerciseId = exerciseCatalogId, metricCode = metricCode)

    private data class RecordBucket(
        val exerciseId: FoundationId,
        val metricCode: WireCode
    )
}

data class DerivedProgressSnapshot(
    val records: List<PersonalRecord>,
    val points: List<ProgressPoint>
)

internal data class DerivedSetEvidence(
    val metric: ProgressEvidenceMetric,
    val value: Double,
    val sourceSet: ExerciseSet
) {
    fun toProgressPoint(
        exerciseCatalogId: FoundationId,
        sourceWorkoutId: FoundationId,
        fallbackRecordedAt: Instant
    ): ProgressPoint =
        ProgressPoint(
            id = newFoundationId("progress"),
            exerciseCatalogId = exerciseCatalogId,
            sourceWorkoutId = sourceWorkoutId,
            sourceSetId = sourceSet.id,
            metric = metric.legacyProgressMetric,
            value = value,
            weight = sourceSet.weight,
            reps = sourceSet.reps,
            recordedAt = sourceSet.loggedAt ?: fallbackRecordedAt,
            metricCode = metric.wireCode,
            derivationVersion = ProgressDerivationVersions.CURRENT
        )
}

internal object SetProgressEvidenceDerivation {
    fun derive(set: ExerciseSet, configuration: LoggingConfiguration): List<DerivedSetEvidence> {
        require(set.captureConfigurationId == configuration.id) {
            "Set capture configuration does not match derivation configuration"
        }

        val evidence = mutableListOf<DerivedSetEvidence>()
        val repetitionsEnabled = configuration.measures.any { it.kind == MeasureKind.REPETITIONS }
        val repetitions = set.reps?.takeIf { repetitionsEnabled && it > 0 }
        val loadSpec = configuration.measures.firstOrNull { it.kind == MeasureKind.LOAD }
        val load = set.weight
        val derivesLoadedPerformance = repetitions != null && load != null && when (loadSpec?.loadRole) {
            LoadRole.EXTERNAL_RESISTANCE -> true
            LoadRole.ADDED_TO_BODYWEIGHT -> load.value > 0.0
            LoadRole.ASSISTANCE,
            LoadRole.LEGACY_UNSPECIFIED,
            null -> false
        }

        if (derivesLoadedPerformance) {
            val repCount = requireNotNull(repetitions)
            val loadValue = requireNotNull(load).value
            evidence += DerivedSetEvidence(ProgressEvidenceMetric.WEIGHT_FOR_REPS, loadValue, set)
            evidence += DerivedSetEvidence(
                ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX,
                loadValue * (1.0 + repCount.toDouble() / 30.0),
                set
            )
            evidence += DerivedSetEvidence(
                ProgressEvidenceMetric.VOLUME,
                loadValue * repCount.toDouble(),
                set
            )
        } else if (repetitions != null) {
            evidence += DerivedSetEvidence(ProgressEvidenceMetric.REPS, repetitions.toDouble(), set)
        }

        if (configuration.measures.any { it.kind == MeasureKind.DURATION }) {
            set.durationMs?.takeIf { it > 0L }?.let { duration ->
                evidence += DerivedSetEvidence(
                    ProgressEvidenceMetric.LONGEST_DURATION,
                    duration.toDouble(),
                    set
                )
            }
        }

        if (configuration.measures.any { it.kind == MeasureKind.DISTANCE }) {
            set.distanceMeters?.takeIf { it.isFinite() && it > 0.0 }?.let { distance ->
                evidence += DerivedSetEvidence(ProgressEvidenceMetric.LONGEST_DISTANCE, distance, set)
            }
        }

        return evidence
    }
}

internal suspend fun <K, V> MutableMap<K, V>.getOrPutSuspending(
    key: K,
    defaultValue: suspend () -> V
): V {
    if (containsKey(key)) return getValue(key)
    return defaultValue().also { put(key, it) }
}
