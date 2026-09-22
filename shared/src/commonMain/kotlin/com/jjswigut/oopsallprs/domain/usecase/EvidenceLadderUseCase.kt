package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.AchievementSeries
import com.jjswigut.oopsallprs.domain.model.CapabilityPoint
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.CurrentBenchmark
import com.jjswigut.oopsallprs.domain.model.EvidenceConfidence
import com.jjswigut.oopsallprs.domain.model.EvidenceCoverage
import com.jjswigut.oopsallprs.domain.model.EvidenceLadderSnapshot
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ExerciseEvidenceComparison
import com.jjswigut.oopsallprs.domain.model.ExerciseProgression
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordAchievement
import com.jjswigut.oopsallprs.domain.model.ProgressComparisonWindow
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceSource
import com.jjswigut.oopsallprs.domain.model.ProgressSourceMeasurement
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.ProgressionSummary
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class EvidenceLadderUseCase {
    fun project(
        workouts: List<CompletedWorkout>,
        records: List<PersonalRecord>,
        points: List<ProgressPoint>,
        loggingConfigurations: List<LoggingConfiguration> = LegacyLoggingConfigurations.all,
        asOf: Instant? = workouts.maxOfOrNull { it.finishedAt }
    ): EvidenceLadderSnapshot {
        val sourceSets = workouts.flatMap { workout ->
            workout.exercises.flatMap { exercise ->
                exercise.loggedSets.filter { it.isLogged }.map { set ->
                    Triple(workout.id, exercise.exerciseCatalogId, set.id) to set
                }
            }
        }.toMap()
        val configurations = (LegacyLoggingConfigurations.all + loggingConfigurations).associateBy { it.id }
        fun sourceSet(point: ProgressPoint): ExerciseSet? {
            val setId = point.sourceSetId ?: return null
            return sourceSets[Triple(point.sourceWorkoutId, point.exerciseCatalogId, setId)]
        }
        val windowDescription = if (asOf == null) " No completed-workout comparison window is available."
            else " Uses a $TREND_WINDOW_DAYS-day window ending on ${asOf.toLocalDateTime(TimeZone.UTC).date}."
        val recentWorkouts = workouts.filter { it.finishedAt.inRecentWindow(asOf) }.distinctBy { it.id }
        val recentWorkoutIds = recentWorkouts.map { it.id }.toSet()
        val recentConfigurations = recentWorkouts.flatMap { it.exercises }
            .groupBy { it.exerciseCatalogId }
            .mapValues { (_, exercises) ->
                exercises.flatMap { it.loggedSets }.filter { it.isLogged }
                    .map { it.captureConfigurationId }.distinct()
            }
        val achievements = records.mapNotNull { record ->
            record.toAchievement(
                sourceSets[Triple(record.sourceWorkoutId, record.exerciseCatalogId, record.sourceSetId)]
                    ?.captureConfigurationId
            )
        }
        val benchmarks = achievements
            .filter { it.series.captureConfigurationId != null }
            .groupBy { it.exerciseCatalogId to it.series }
            .values
            .mapNotNull { series -> series.maxWithOrNull(compareBy<PersonalRecordAchievement> { it.value }.thenBy { it.achievedAt }) }
            .map { CurrentBenchmark(it.series, it) }
        val names = workouts.exerciseNames()
        val comparablePoints = points.filter { point ->
            val set = sourceSet(point)
            set != null && configurations[set.captureConfigurationId].supportsExternalLoadEvidence() &&
                point.value.isFinite() && point.value > 0.0 &&
                point.weight == set.weight && point.reps == set.reps &&
                point.recordedAt == set.loggedAt
        }.distinctBy { Triple(it.sourceWorkoutId, it.sourceSetId, it.metricCode) }
        val capabilityPoints = comparablePoints.asSequence()
            .filter { it.metricCode == ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX.wireCode }
            .filter { it.reps in 1..12 && it.weight != null && it.sourceSetId != null }
            .groupBy { Triple(it.exerciseCatalogId, it.sourceWorkoutId, sourceSet(it)?.captureConfigurationId) }
            .values
            .mapNotNull { session -> session.maxByOrNull { it.value } }
            .map { point ->
                CapabilityPoint(
                    exerciseCatalogId = point.exerciseCatalogId,
                    exerciseName = names[point.exerciseCatalogId] ?: "Exercise ${point.exerciseCatalogId.value}",
                    sourceWorkoutId = point.sourceWorkoutId,
                    sourceSetId = requireNotNull(point.sourceSetId),
                    recordedAt = point.recordedAt,
                    estimatedOneRepMaxKg = point.value,
                    sourceWeight = requireNotNull(point.weight),
                    sourceReps = requireNotNull(point.reps),
                    captureConfigurationId = sourceSet(point)?.captureConfigurationId
                )
            }
            .sortedBy { it.recordedAt }
        val exerciseIds = (achievements.map { it.exerciseCatalogId } + capabilityPoints.map { it.exerciseCatalogId } +
            recentConfigurations.keys).distinct()
        val exerciseProgressions = exerciseIds.map { exerciseId ->
            val exerciseAchievements = achievements.filter { it.exerciseCatalogId == exerciseId }
            val exerciseBenchmarks = benchmarks.filter { it.achievement.exerciseCatalogId == exerciseId }
            val exercisePoints = capabilityPoints.filter { it.exerciseCatalogId == exerciseId }
            val recentPoints = exercisePoints.filter {
                it.sourceWorkoutId in recentWorkoutIds && it.recordedAt.inRecentWindow(asOf)
            }
            val baseCapability = summarizeCapability(
                recentPoints,
                comparable = recentConfigurations[exerciseId]?.singleOrNull()?.let {
                    configurations[it].supportsExternalLoadEvidence()
                } == true
            )
            val latestRepAchievement = exerciseAchievements
                .filter {
                    it.series.metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS &&
                        it.sourceWorkoutId in recentWorkoutIds &&
                        it.series.captureConfigurationId == recentConfigurations[exerciseId]?.singleOrNull()
                }
                .maxByOrNull { it.achievedAt }
            val capability = if (
                baseCapability.state == ProgressTrendState.HOLDING_STEADY && latestRepAchievement?.reps != null
            ) {
                baseCapability.copy(
                    explanation = baseCapability.explanation +
                        " The ${latestRepAchievement.reps}-rep benchmark expanded without overriding the overall reading."
                )
            } else {
                baseCapability
            }
            ExerciseProgression(
                exerciseCatalogId = exerciseId,
                exerciseName = names[exerciseId] ?: "Exercise ${exerciseId.value}",
                achievements = exerciseAchievements,
                currentBenchmarks = exerciseBenchmarks,
                capabilityPoints = exercisePoints,
                capability = capability.copy(explanation = capability.explanation + windowDescription)
            )
        }
        val overallReadings = listOf(
            summarizeOverallCapability(exerciseProgressions, capabilityPoints.filter {
                it.sourceWorkoutId in recentWorkoutIds && it.recordedAt.inRecentWindow(asOf)
            }),
            summarizeWorkCapacity(
                comparablePoints.filter { it.sourceWorkoutId in recentWorkoutIds && it.recordedAt.inRecentWindow(asOf) },
                names,
                recentConfigurations,
                configurations
            ),
            summarizeConsistency(recentWorkouts)
        ).map { summary ->
            summary.copy(explanation = summary.explanation + windowDescription)
        }
        return EvidenceLadderSnapshot(
            achievements = achievements.sortedByDescending { it.achievedAt },
            currentBenchmarks = benchmarks,
            capabilityPoints = capabilityPoints,
            exerciseProgressions = exerciseProgressions,
            overallReadings = overallReadings
        )
    }

    private fun summarizeCapability(points: List<CapabilityPoint>, comparable: Boolean = true): ProgressionSummary {
        val ordered = points.sortedBy { it.recordedAt }
        val spanDays = ordered.capabilitySpanDays()
        val coverage = EvidenceCoverage(
            qualifyingSessions = ordered.map { it.sourceWorkoutId }.distinct().size,
            spanDays = spanDays,
            contributingExercises = ordered.map { it.exerciseCatalogId }.distinct().size
        )
        val evidence = ordered.map { point ->
            ProgressEvidenceSource(
                exerciseCatalogId = point.exerciseCatalogId,
                exerciseName = point.exerciseName,
                sourceWorkoutId = point.sourceWorkoutId,
                sourceSetId = point.sourceSetId,
                recordedAt = point.recordedAt,
                measurement = ProgressSourceMeasurement.EstimatedStrength(
                    point.estimatedOneRepMaxKg, point.sourceWeight, point.sourceReps
                )
            )
        }
        if (!coverage.isMature || !comparable) {
            return ProgressionSummary(
                reading = EvidenceReading.CAPABILITY,
                state = ProgressTrendState.BUILDING_TREND,
                changePercent = null,
                confidence = EvidenceConfidence.LOW,
                coverage = coverage,
                comparisonWindow = null,
                evidence = evidence,
                explanation = if (!comparable) {
                    "Not enough comparable evidence: different or unresolved logging configurations are not combined into a strength trend."
                } else {
                    "Building a trend from qualified Estimated 1RM sets of 1-12 reps in the last $TREND_WINDOW_DAYS days. Six sessions across 28 days are required."
                }
            )
        }
        val split = ordered.size / 2
        val earlier = ordered.take(split)
        val recent = ordered.drop(split)
        val earlierMedian = earlier.map { it.estimatedOneRepMaxKg }.median()
        val recentMedian = recent.map { it.estimatedOneRepMaxKg }.median()
        val change = if (earlierMedian == 0.0) 0.0 else ((recentMedian - earlierMedian) / earlierMedian) * 100.0
        val state = when {
            change >= 3.0 -> ProgressTrendState.RECENT_RANGE_HIGHER
            change <= -5.0 -> ProgressTrendState.REBUILDING
            else -> ProgressTrendState.HOLDING_STEADY
        }
        return ProgressionSummary(
            reading = EvidenceReading.CAPABILITY,
            state = state,
            changePercent = change,
            confidence = EvidenceConfidence.MEDIUM,
            coverage = coverage,
            comparisonWindow = ProgressComparisonWindow(
                earlierStart = earlier.first().recordedAt,
                earlierEnd = earlier.last().recordedAt,
                recentStart = recent.first().recordedAt,
                recentEnd = recent.last().recordedAt
            ),
            evidence = evidence,
            explanation = when (state) {
                ProgressTrendState.RECENT_RANGE_HIGHER -> "Recent qualified Estimated 1RM range is higher than the earlier comparison range."
                ProgressTrendState.HOLDING_STEADY -> "Qualified Estimated 1RM evidence is holding steady across the comparison window."
                ProgressTrendState.REBUILDING -> "Recent qualified Estimated 1RM evidence is below the earlier range; rebuilding is a normal training state."
                ProgressTrendState.BUILDING_TREND,
                ProgressTrendState.STEADY -> error("Unexpected mature capability state")
            }
        )
    }

    private fun summarizeOverallCapability(
        exercises: List<ExerciseProgression>,
        points: List<CapabilityPoint>
    ): ProgressionSummary {
        val matureExercises = exercises.filter { it.capability.coverage.isMature && it.capability.changePercent != null }
        val mature = matureExercises.map { it.capability }
        if (mature.isEmpty()) {
            val base = summarizeCapability(points, comparable = false)
            return base.copy(
                state = ProgressTrendState.BUILDING_TREND,
                changePercent = null,
                confidence = EvidenceConfidence.LOW,
                comparisonWindow = null,
                explanation = "Building a trend until at least one exercise has six qualified Estimated 1RM sessions across 28 days."
            )
        }
        val changes = mature.mapNotNull { it.changePercent }
        val change = changes.median()
        val state = trendState(change, higherThreshold = 3.0, rebuildingThreshold = -5.0)
        val evidence = mature.flatMap { it.evidence }.sortedBy { it.recordedAt }
        val sessions = evidence.map { it.sourceWorkoutId }.distinct().size
        val spanDays = evidence.map { it.recordedAt }.spanDays()
        val windows = mature.mapNotNull { it.comparisonWindow }
        return ProgressionSummary(
            reading = EvidenceReading.CAPABILITY,
            state = state,
            changePercent = change,
            confidence = EvidenceConfidence.MEDIUM,
            coverage = EvidenceCoverage(sessions, spanDays, mature.size),
            comparisonWindow = ProgressComparisonWindow(
                earlierStart = windows.minOf { it.earlierStart },
                earlierEnd = windows.maxOf { it.earlierEnd },
                recentStart = windows.minOf { it.recentStart },
                recentEnd = windows.maxOf { it.recentEnd }
            ),
            evidence = evidence,
            explanation = "Each exercise is compared with its own earlier estimated 1RM. " +
                "The overall change is the middle (median) percentage change across ${mature.size} " +
                "${if (mature.size == 1) "exercise" else "exercises"}.",
            exerciseComparisons = matureExercises.map { exercise ->
                ExerciseEvidenceComparison(
                    exerciseCatalogId = exercise.exerciseCatalogId,
                    exerciseName = exercise.exerciseName,
                    changePercent = requireNotNull(exercise.capability.changePercent),
                    comparisonWindow = requireNotNull(exercise.capability.comparisonWindow)
                )
            }
        )
    }

    private fun summarizeWorkCapacity(
        points: List<ProgressPoint>,
        names: Map<FoundationId, String>,
        recentConfigurations: Map<FoundationId, List<LoggingConfigurationId>>,
        configurations: Map<LoggingConfigurationId, LoggingConfiguration>
    ): ProgressionSummary {
        val volumePoints = points
            .filter { it.metricCode == ProgressEvidenceMetric.VOLUME.wireCode && it.sourceSetId != null }
            .filter { it.weight != null && it.reps != null && it.reps > 0 }
            .sortedBy { it.recordedAt }
        val pointsByExercise = volumePoints.groupBy { it.exerciseCatalogId }
        var changedConfigurations = 0
        var unsupportedConfigurations = 0
        val comparableExercises = recentConfigurations.mapNotNull { (exerciseId, ids) ->
            if (ids.size > 1) {
                changedConfigurations += 1
                return@mapNotNull null
            }
            if (ids.singleOrNull()?.let { configurations[it].supportsExternalLoadEvidence() } != true) {
                unsupportedConfigurations += 1
                return@mapNotNull null
            }
            val exercisePoints = pointsByExercise[exerciseId].orEmpty()
            val sessions = exercisePoints.groupBy { it.sourceWorkoutId }
                .map { (workoutId, sessionPoints) ->
                    WorkCapacitySession(
                        workoutId = workoutId,
                        recordedAt = sessionPoints.maxOf { it.recordedAt },
                        loadRepTotalKg = sessionPoints.sumOf { it.value }
                    )
                }
                .sortedBy { it.recordedAt }
            WorkCapacityExercise(
                exerciseId = exerciseId,
                points = exercisePoints,
                sessions = sessions,
                coverage = EvidenceCoverage(
                    qualifyingSessions = sessions.size,
                    spanDays = sessions.map { it.recordedAt }.spanDays(),
                    contributingExercises = if (sessions.isEmpty()) 0 else 1
                )
            )
        }
        val mature = comparableExercises.filter { it.coverage.isMature }
        val sparseCount = comparableExercises.size - mature.size
        val excludedCount = sparseCount + changedConfigurations + unsupportedConfigurations
        val exclusionReasons = listOfNotNull(
            "$sparseCount awaiting six qualifying sessions across at least 28 days per exercise".takeIf { sparseCount > 0 },
            "$changedConfigurations with changed logging configurations".takeIf { changedConfigurations > 0 },
            "$unsupportedConfigurations unsupported by this external-weight reading".takeIf { unsupportedConfigurations > 0 }
        )
        val inclusionExplanation = "Included ${mature.size} ${if (mature.size == 1) "exercise" else "exercises"}; " +
            "excluded $excludedCount ${if (excludedCount == 1) "exercise" else "exercises"}" +
            if (exclusionReasons.isEmpty()) "." else ": ${exclusionReasons.joinToString("; ")}."

        // Insufficient coverage describes one actual exercise, never pooled maturity across sparse exercises.
        val buildingCandidate = comparableExercises.maxWithOrNull(
            compareBy<WorkCapacityExercise> { it.coverage.qualifyingSessions }.thenBy { it.coverage.spanDays }
        )
        val contributingPoints = if (mature.isEmpty()) buildingCandidate?.points.orEmpty()
            else mature.flatMap { it.points }.sortedBy { it.recordedAt }
        val evidence = contributingPoints.map { point ->
            ProgressEvidenceSource(
                exerciseCatalogId = point.exerciseCatalogId,
                exerciseName = names[point.exerciseCatalogId] ?: "Exercise ${point.exerciseCatalogId.value}",
                sourceWorkoutId = point.sourceWorkoutId,
                sourceSetId = point.sourceSetId,
                recordedAt = point.recordedAt,
                measurement = ProgressSourceMeasurement.CompletedWork(
                    point.value, requireNotNull(point.weight), requireNotNull(point.reps)
                )
            )
        }
        if (mature.isEmpty()) {
            val candidateExplanation = buildingCandidate?.takeIf { it.points.isNotEmpty() }?.let {
                " The closest comparable history is ${names[it.exerciseId] ?: "Exercise ${it.exerciseId.value}"}."
            }.orEmpty()
            return ProgressionSummary(
                reading = EvidenceReading.WORK_CAPACITY,
                state = ProgressTrendState.BUILDING_TREND,
                changePercent = null,
                confidence = EvidenceConfidence.LOW,
                coverage = buildingCandidate?.coverage ?: EvidenceCoverage(0, 0, 0),
                comparisonWindow = null,
                evidence = evidence,
                explanation = "Building a trend from completed load × reps per exercise per session. " +
                    "$inclusionExplanation$candidateExplanation " +
                    "Six sessions across at least 28 days per exercise are required. " +
                    "This describes work performed, not strength or physiological change."
            )
        }
        val comparisons = mature.map { exercise ->
            val split = exercise.sessions.size / 2
            val earlier = exercise.sessions.take(split)
            val recent = exercise.sessions.drop(split)
            ExerciseEvidenceComparison(
                exerciseCatalogId = exercise.exerciseId,
                exerciseName = names[exercise.exerciseId] ?: "Exercise ${exercise.exerciseId.value}",
                changePercent = percentChange(
                    earlier.map { it.loadRepTotalKg }.median(),
                    recent.map { it.loadRepTotalKg }.median()
                ),
                comparisonWindow = ProgressComparisonWindow(
                    earlierStart = earlier.first().recordedAt,
                    earlierEnd = earlier.last().recordedAt,
                    recentStart = recent.first().recordedAt,
                    recentEnd = recent.last().recordedAt
                )
            )
        }
        val change = comparisons.map { it.changePercent }.median()
        return ProgressionSummary(
            reading = EvidenceReading.WORK_CAPACITY,
            state = trendState(change, higherThreshold = 5.0, rebuildingThreshold = -10.0),
            changePercent = change,
            confidence = EvidenceConfidence.MEDIUM,
            coverage = EvidenceCoverage(
                qualifyingSessions = contributingPoints.map { it.sourceWorkoutId }.distinct().size,
                spanDays = contributingPoints.map { it.recordedAt }.spanDays(),
                contributingExercises = mature.size
            ),
            comparisonWindow = ProgressComparisonWindow(
                earlierStart = comparisons.minOf { it.comparisonWindow.earlierStart },
                earlierEnd = comparisons.maxOf { it.comparisonWindow.earlierEnd },
                recentStart = comparisons.minOf { it.comparisonWindow.recentStart },
                recentEnd = comparisons.maxOf { it.comparisonWindow.recentEnd }
            ),
            evidence = evidence,
            explanation = "Compares typical work per session (load × reps) within each exercise. " +
                "The overall change is the middle (median) of the exercise percentage changes. " +
                "$inclusionExplanation More work does not necessarily mean greater strength.",
            exerciseComparisons = comparisons
        )
    }

    private fun summarizeConsistency(workouts: List<CompletedWorkout>): ProgressionSummary {
        val ordered = workouts.sortedBy { it.finishedAt }
        val coverage = EvidenceCoverage(
            qualifyingSessions = ordered.size,
            spanDays = ordered.map { it.finishedAt }.spanDays(),
            contributingExercises = ordered.flatMap { it.exercises }.map { it.exerciseCatalogId }.distinct().size
        )
        val evidence = ordered.map { workout ->
            ProgressEvidenceSource(
                exerciseCatalogId = workout.exercises.firstOrNull()?.exerciseCatalogId
                    ?: com.jjswigut.oopsallprs.domain.model.FoundationId("workout-evidence"),
                exerciseName = "Completed workout",
                sourceWorkoutId = workout.id,
                sourceSetId = null,
                recordedAt = workout.finishedAt,
                measurement = ProgressSourceMeasurement.CompletedSession
            )
        }
        if (!coverage.isMature) {
            return ProgressionSummary(
                reading = EvidenceReading.CONSISTENCY,
                state = ProgressTrendState.BUILDING_TREND,
                changePercent = null,
                confidence = EvidenceConfidence.LOW,
                coverage = coverage,
                comparisonWindow = null,
                evidence = evidence,
                explanation = "Building a trend from completed-session spacing; six sessions across 28 days are required."
            )
        }
        val split = ordered.size / 2
        val earlierGaps = ordered.take(split).sessionGapsInDays()
        val recentGaps = ordered.drop(split - 1).sessionGapsInDays()
        // Whole-day spacing and the earlier range tolerate alternating schedules and time-of-day noise.
        // A wider gap is observable; a return/deload or physiological state is not.
        val state = if (recentGaps.median() <= earlierGaps.max()) ProgressTrendState.STEADY
            else ProgressTrendState.HOLDING_STEADY
        return ProgressionSummary(
            reading = EvidenceReading.CONSISTENCY,
            state = state,
            changePercent = null,
            confidence = EvidenceConfidence.MEDIUM,
            coverage = coverage,
            comparisonWindow = ProgressComparisonWindow(
                ordered.first().finishedAt,
                ordered[split - 1].finishedAt,
                ordered[split - 1].finishedAt,
                ordered.last().finishedAt
            ),
            evidence = evidence,
            explanation = if (state == ProgressTrendState.STEADY) {
                "Median spacing (typical days between sessions) is within or below your earlier range. Measured in whole days."
            } else {
                "Typical spacing between sessions is wider than your earlier range. Measured in whole days. " +
                    "This is a neutral observation; it does not identify a return or deload."
            }
        )
    }
}

private fun List<CapabilityPoint>.capabilitySpanDays(): Int =
    if (size < 2) 0 else ((last().recordedAt.toEpochMilliseconds() - first().recordedAt.toEpochMilliseconds()) / DAY_MS).toInt()

private fun List<CompletedWorkout>.sessionGapsInDays(): List<Double> = zipWithNext { first, second ->
    kotlin.math.round((second.finishedAt.toEpochMilliseconds() - first.finishedAt.toEpochMilliseconds()).toDouble() / DAY_MS)
}

private fun List<Double>.median(): Double {
    val ordered = sorted()
    val middle = ordered.size / 2
    return if (ordered.size % 2 == 0) (ordered[middle - 1] + ordered[middle]) / 2.0 else ordered[middle]
}

private fun List<kotlinx.datetime.Instant>.spanDays(): Int =
    if (size < 2) 0 else ((max().toEpochMilliseconds() - min().toEpochMilliseconds()) / DAY_MS).toInt()

private fun percentChange(earlier: Double, recent: Double): Double =
    if (earlier == 0.0) 0.0 else ((recent - earlier) / earlier) * 100.0

private fun trendState(change: Double, higherThreshold: Double, rebuildingThreshold: Double): ProgressTrendState =
    when {
        change >= higherThreshold -> ProgressTrendState.RECENT_RANGE_HIGHER
        change <= rebuildingThreshold -> ProgressTrendState.REBUILDING
        else -> ProgressTrendState.HOLDING_STEADY
    }

private data class WorkCapacitySession(
    val workoutId: com.jjswigut.oopsallprs.domain.model.FoundationId,
    val recordedAt: kotlinx.datetime.Instant,
    val loadRepTotalKg: Double
)

private data class WorkCapacityExercise(
    val exerciseId: FoundationId,
    val points: List<ProgressPoint>,
    val sessions: List<WorkCapacitySession>,
    val coverage: EvidenceCoverage
)

private fun List<CompletedWorkout>.exerciseNames(): Map<com.jjswigut.oopsallprs.domain.model.FoundationId, String> {
    val names = linkedMapOf<com.jjswigut.oopsallprs.domain.model.FoundationId, String>()
    sortedBy { it.finishedAt }.forEach { workout ->
        workout.exercises.forEach { names[it.exerciseCatalogId] = it.displayNameSnapshot }
    }
    return names
}

private const val DAY_MS: Long = 86_400_000L
private const val TREND_WINDOW_DAYS: Int = 56

private fun Instant.inRecentWindow(asOf: Instant?): Boolean = asOf != null &&
    toEpochMilliseconds() <= asOf.toEpochMilliseconds() &&
    toEpochMilliseconds() >= asOf.toEpochMilliseconds() - TREND_WINDOW_DAYS * DAY_MS

private fun LoggingConfiguration?.supportsExternalLoadEvidence(): Boolean = this != null &&
    measures.any { it.kind == MeasureKind.REPETITIONS } &&
    measures.any { it.kind == MeasureKind.LOAD && it.loadRole == LoadRole.EXTERNAL_RESISTANCE }

private fun PersonalRecord.toAchievement(configurationId: LoggingConfigurationId?): PersonalRecordAchievement? {
    val metric = ProgressEvidenceMetric.fromWireCode(metricCode.value) ?: return null
    return PersonalRecordAchievement(
        id = id,
        exerciseCatalogId = exerciseCatalogId,
        series = AchievementSeries(
            metric = metric,
            reps = reps.takeIf { metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS },
            captureConfigurationId = configurationId
        ),
        value = value,
        weight = weight,
        reps = reps,
        sourceWorkoutId = sourceWorkoutId,
        sourceSetId = sourceSetId,
        achievedAt = achievedAt,
        derivationVersion = derivationVersion
    )
}
