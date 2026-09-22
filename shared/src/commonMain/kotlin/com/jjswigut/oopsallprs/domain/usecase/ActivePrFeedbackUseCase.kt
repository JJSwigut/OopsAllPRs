package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedbackKind
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository

class ActivePrFeedbackUseCase(
    private val progressRepository: ProgressRepository,
    private val loggingConfigurationRepository: LoggingConfigurationRepository? =
        progressRepository as? LoggingConfigurationRepository
) {
    suspend fun feedbackFor(
        workout: ActiveWorkout,
        exercise: ActiveExercise,
        set: ExerciseSet
    ): ActivePrFeedback? {
        val configurationCache = mutableMapOf<LoggingConfigurationId, LoggingConfiguration?>()
        val evidence = derive(set, configurationCache).feedbackEvidence() ?: return null
        val repetitions = evidence.sourceSet.reps
        val previous = when (evidence.metric) {
            ProgressEvidenceMetric.WEIGHT_FOR_REPS -> {
                val candidate = WeightedSetPerformance(
                    weightKg = evidence.value,
                    reps = repetitions ?: return null
                )
                val previousSets = previousWeightedSets(workout, exercise, set, configurationCache)
                previousSets.filter { it.reps == candidate.reps }.maxOfOrNull { it.weightKg }
            }
            ProgressEvidenceMetric.REPS,
            ProgressEvidenceMetric.LONGEST_DURATION -> previousBest(
                workout = workout,
                exercise = exercise,
                set = set,
                metric = evidence.metric,
                configurationCache = configurationCache
            )
            ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX,
            ProgressEvidenceMetric.VOLUME,
            ProgressEvidenceMetric.LONGEST_DISTANCE -> return null
        }
        if (previous != null && evidence.value <= previous) return null

        val kind = when (evidence.metric) {
            ProgressEvidenceMetric.WEIGHT_FOR_REPS -> ActivePrFeedbackKind.WEIGHT_FOR_REPS
            ProgressEvidenceMetric.REPS -> ActivePrFeedbackKind.BODYWEIGHT_REPS
            ProgressEvidenceMetric.LONGEST_DURATION -> ActivePrFeedbackKind.TIME
            ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX,
            ProgressEvidenceMetric.VOLUME,
            ProgressEvidenceMetric.LONGEST_DISTANCE -> return null
        }
        return ActivePrFeedback(
            setId = set.id,
            exerciseCatalogId = exercise.reference.exerciseCatalogId,
            kind = kind,
            label = labelFor(kind, evidence.value, repetitions, previous),
            previousValue = previous,
            newValue = evidence.value
        )
    }

    private suspend fun previousWeightedSets(
        workout: ActiveWorkout,
        exercise: ActiveExercise,
        set: ExerciseSet,
        configurationCache: MutableMap<LoggingConfigurationId, LoggingConfiguration?>
    ): List<WeightedSetPerformance> {
        val recordSets = progressRepository.personalRecords()
            .asSequence()
            .filter { record ->
                record.exerciseCatalogId == exercise.reference.exerciseCatalogId &&
                    record.sourceSetId != set.id &&
                    record.metricCode == ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode
            }
            .mapNotNull { record ->
                val reps = record.reps ?: return@mapNotNull null
                WeightedSetPerformance(record.weight?.value ?: record.value, reps)
            }

        val activeSets = mutableListOf<WeightedSetPerformance>()
        workout.exercises
            .filter { it.reference.exerciseCatalogId == exercise.reference.exerciseCatalogId }
            .flatMap { it.sets }
            .filter { candidate -> candidate.isLogged && candidate.id != set.id && candidate.loggedAt != null }
            .forEach { candidate ->
                val candidateEvidence = derive(candidate, configurationCache)
                    .firstOrNull { it.metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS }
                    ?: return@forEach
                val reps = candidate.reps ?: return@forEach
                activeSets += WeightedSetPerformance(candidateEvidence.value, reps)
            }

        return recordSets.toList() + activeSets
    }

    private suspend fun previousBest(
        workout: ActiveWorkout,
        exercise: ActiveExercise,
        set: ExerciseSet,
        metric: ProgressEvidenceMetric,
        configurationCache: MutableMap<LoggingConfigurationId, LoggingConfiguration?>
    ): Double? {
        val recordBest = progressRepository.personalRecords()
            .filter { record ->
                record.exerciseCatalogId == exercise.reference.exerciseCatalogId &&
                    record.sourceSetId != set.id &&
                    record.metricCode == metric.wireCode
            }
            .maxOfOrNull { it.value }

        val activeValues = mutableListOf<Double>()
        workout.exercises
            .filter { it.reference.exerciseCatalogId == exercise.reference.exerciseCatalogId }
            .flatMap { it.sets }
            .filter { candidate -> candidate.isLogged && candidate.id != set.id && candidate.loggedAt != null }
            .forEach { candidate ->
                derive(candidate, configurationCache)
                    .firstOrNull { it.metric == metric }
                    ?.let { activeValues += it.value }
            }

        return listOfNotNull(recordBest, activeValues.maxOrNull()).maxOrNull()
    }

    private suspend fun derive(
        set: ExerciseSet,
        configurationCache: MutableMap<LoggingConfigurationId, LoggingConfiguration?>
    ): List<DerivedSetEvidence> {
        val configuration = configurationCache.getOrPutSuspending(set.captureConfigurationId) {
            resolveConfiguration(set.captureConfigurationId)
        } ?: return emptyList()
        return SetProgressEvidenceDerivation.derive(set, configuration)
    }

    private suspend fun resolveConfiguration(id: LoggingConfigurationId): LoggingConfiguration? =
        loggingConfigurationRepository?.loggingConfiguration(id)
            ?: LegacyLoggingConfigurations.all.firstOrNull { it.id == id }

    private fun List<DerivedSetEvidence>.feedbackEvidence(): DerivedSetEvidence? =
        firstOrNull { it.metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS }
            ?: firstOrNull { it.metric == ProgressEvidenceMetric.REPS }
            ?: firstOrNull { it.metric == ProgressEvidenceMetric.LONGEST_DURATION }

    private fun labelFor(
        kind: ActivePrFeedbackKind,
        newValue: Double,
        reps: Int?,
        previous: Double?
    ): String {
        val value = when (kind) {
            ActivePrFeedbackKind.BODYWEIGHT_REPS -> "${newValue.toInt()} reps"
            ActivePrFeedbackKind.WEIGHT_FOR_REPS -> "${newValue.trimmed()} kg x ${reps ?: 0}"
            ActivePrFeedbackKind.TIME -> formatDuration(newValue.toLong())
        }
        if (kind == ActivePrFeedbackKind.WEIGHT_FOR_REPS) {
            return "New ${reps ?: 0}-rep PR: $value"
        }
        return if (previous == null) {
            "New PR: $value"
        } else {
            "PR: $value"
        }
    }

    private fun Double.trimmed(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }
}
