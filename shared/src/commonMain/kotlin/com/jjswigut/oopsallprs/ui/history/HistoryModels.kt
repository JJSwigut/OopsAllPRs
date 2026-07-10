package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.common.shortDateTimeLabel
import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class CompletedWorkoutSummary(
    val workoutId: FoundationId,
    val startedAt: Instant,
    val finishedAt: Instant,
    val durationMs: Long,
    val durationLabel: String,
    val exerciseCount: Int,
    val setCount: Int,
    val prCount: Int,
    val exercises: List<CompletedExerciseSummary>
)

data class CompletedExerciseSummary(
    val completedExerciseId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val displayName: String,
    val isBodyweight: Boolean,
    val position: Int,
    val setRows: List<CompletedSetSummary>
)

data class CompletedSetSummary(
    val setId: FoundationId,
    val position: Int,
    val setKind: SetKind,
    val reps: Int?,
    val weight: WeightKg?,
    val durationMs: Long?,
    val loggedAt: Instant,
    val prMarkers: List<CompletedPrMarker> = emptyList()
)

data class CompletedPrMarker(
    val kind: PersonalRecordKind,
    val label: String,
    val value: Double,
    val weight: WeightKg? = null,
    val reps: Int? = null,
    val achievedAt: Instant
)

data class HistoryListItem(
    val workoutId: FoundationId,
    val title: String,
    val finishedAt: Instant,
    val durationLabel: String,
    val exerciseCount: Int,
    val setCount: Int
)

data class TemplateListItem(
    val templateId: FoundationId,
    val name: String,
    val exerciseCount: Int,
    val setTargetCount: Int,
    val sourceCompletedWorkoutId: FoundationId?
)

data class TemplateSaveDraft(
    val completedWorkoutId: FoundationId,
    val name: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

fun CompletedWorkout.toSummary(
    personalRecords: List<PersonalRecord> = emptyList()
): CompletedWorkoutSummary {
    val recordsBySetId = personalRecords.groupBy { it.sourceSetId }
    val exerciseSummaries = exercises
        .sortedBy { it.position.value }
        .map { it.toSummary(recordsBySetId) }
    val setCount = exerciseSummaries.sumOf { it.setRows.size }
    val prCount = exerciseSummaries.sumOf { exercise ->
        exercise.setRows.sumOf { it.prMarkers.size }
    }
    return CompletedWorkoutSummary(
        workoutId = id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        durationMs = durationMs,
        durationLabel = formatDuration(durationMs),
        exerciseCount = exerciseSummaries.size,
        setCount = setCount,
        prCount = prCount,
        exercises = exerciseSummaries
    )
}

fun List<CompletedWorkout>.toHistoryRows(
    personalRecords: List<PersonalRecord> = emptyList()
): List<HistoryListItem> =
    sortedByDescending { it.finishedAt }.map { workout ->
        val summary = workout.toSummary(personalRecords)
        HistoryListItem(
            workoutId = workout.id,
            title = workout.finishedAt.shortDateTimeLabel(),
            finishedAt = workout.finishedAt,
            durationLabel = summary.durationLabel,
            exerciseCount = summary.exerciseCount,
            setCount = summary.setCount
        )
    }

fun ReusableRoutine.toTemplateListItem(): TemplateListItem =
    TemplateListItem(
        templateId = id,
        name = name,
        exerciseCount = exercises.size,
        setTargetCount = exercises.sumOf { it.plannedSets.size },
        sourceCompletedWorkoutId = sourceCompletedWorkoutId
    )

private fun CompletedExercise.toSummary(
    recordsBySetId: Map<FoundationId, List<PersonalRecord>>
): CompletedExerciseSummary {
    val rows = loggedSets
        .filter { it.loggedAt != null }
        .sortedBy { it.position.value }
        .map { it.toSummary(recordsBySetId[it.id].orEmpty()) }
    return CompletedExerciseSummary(
        completedExerciseId = id,
        exerciseCatalogId = exerciseCatalogId,
        displayName = displayNameSnapshot,
        isBodyweight = rows.any { it.setKind == SetKind.BODYWEIGHT || it.setKind == SetKind.TIMED },
        position = position.value,
        setRows = rows
    )
}

private fun ExerciseSet.toSummary(records: List<PersonalRecord>): CompletedSetSummary =
    CompletedSetSummary(
        setId = id,
        position = position.value,
        setKind = setKind,
        reps = reps,
        weight = weight,
        durationMs = durationMs,
        loggedAt = requireNotNull(loggedAt),
        prMarkers = records.map { it.toMarker() }
    )

private fun PersonalRecord.toMarker(): CompletedPrMarker =
    CompletedPrMarker(
        kind = recordKind,
        label = when (recordKind) {
            PersonalRecordKind.WEIGHT_FOR_REPS -> {
                val repsLabel = reps?.let { " x $it" }.orEmpty()
                "PR ${weight?.value?.formatCompact().orEmpty()}kg$repsLabel"
            }
            PersonalRecordKind.BODYWEIGHT_REPS -> "PR ${reps ?: value.roundToInt()} reps"
            PersonalRecordKind.ESTIMATED_ONE_REP_MAX -> "PR e1RM ${value.formatCompact()}kg"
            PersonalRecordKind.VOLUME -> "PR volume ${value.formatCompact()}"
            PersonalRecordKind.TIME -> "PR ${value.roundToLong().formatDurationMs()}"
        },
        value = value,
        weight = weight,
        reps = reps,
        achievedAt = achievedAt
    )

internal fun CompletedSetSummary.historyDisplayLabel(weightUnit: WeightUnit): String {
    val weightLabel = weight?.let { " • ${it.historyWeightLabel(weightUnit)}" }.orEmpty()
    val prs = if (prMarkers.isEmpty()) {
        ""
    } else {
        " • ${prMarkers.joinToString { it.historyDetailLabel(weightUnit) }}"
    }
    val valueLabel = if (setKind == SetKind.TIMED) {
        durationMs.formatDurationMs()
    } else {
        "${(reps ?: 0).coerceAtLeast(0)} reps$weightLabel"
    }
    return "Set ${position + 1}: $valueLabel$prs"
}

internal fun CompletedPrMarker.historyLabel(weightUnit: WeightUnit): String =
    when (kind) {
        PersonalRecordKind.WEIGHT_FOR_REPS -> {
            val weightLabel = (weight ?: WeightKg(value)).historyWeightLabel(weightUnit)
            val repsLabel = reps?.let { " x $it" }.orEmpty()
            "PR $weightLabel$repsLabel"
        }
        PersonalRecordKind.BODYWEIGHT_REPS -> "PR ${(reps ?: value.roundToInt()).coerceAtLeast(0)} reps"
        PersonalRecordKind.ESTIMATED_ONE_REP_MAX -> "PR e1RM ${WeightKg(value).historyWeightLabel(weightUnit)}"
        PersonalRecordKind.VOLUME -> "PR volume ${WeightKg(value).historyWeightLabel(weightUnit)}"
        PersonalRecordKind.TIME -> "PR ${value.roundToLong().formatDurationMs()}"
    }

internal fun CompletedPrMarker.historyDetailLabel(weightUnit: WeightUnit): String =
    "${historyLabel(weightUnit)} • ${achievedAt.shortDateLabel()}"

private fun WeightKg.historyWeightLabel(unit: WeightUnit): String =
    "${displayValue(unit).formatCompact()} ${unit.abbreviation()}"

private fun WeightUnit.abbreviation(): String =
    when (this) {
        WeightUnit.KILOGRAMS -> "kg"
        WeightUnit.POUNDS -> "lb"
    }

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = (durationMs.coerceAtLeast(0L) / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun Long?.formatDurationMs(): String {
    val totalSeconds = (((this ?: 0L).coerceAtLeast(0L)) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private fun Double.formatCompact(): String {
    val oneDecimal = round(this * 10.0) / 10.0
    val whole = oneDecimal.roundToInt()
    return if (abs(oneDecimal - whole.toDouble()) < 0.0001) whole.toString() else oneDecimal.toString()
}
