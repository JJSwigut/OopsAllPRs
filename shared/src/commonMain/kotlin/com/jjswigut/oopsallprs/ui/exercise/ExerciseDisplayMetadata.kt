package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode

internal fun exerciseContextSubtitle(
    muscleGroup: String,
    equipment: String,
    isBodyweight: Boolean
): String =
    listOf(muscleGroup, equipment)
        .map { it.trim() }
        .filter { value ->
            value.isNotBlank() &&
                !value.equals("Custom", ignoreCase = true) &&
                !(isBodyweight && value.equals("Bodyweight", ignoreCase = true))
        }
        .distinctBy { it.lowercase() }
        .joinToString(" • ")

internal fun exerciseKindLabel(isBodyweight: Boolean): String =
    if (isBodyweight) "Bodyweight" else "Weighted"

internal fun exerciseKindLabel(loggingMode: ExerciseLoggingMode): String =
    when (loggingMode) {
        ExerciseLoggingMode.WEIGHTED -> "Weighted"
        ExerciseLoggingMode.BODYWEIGHT -> "Bodyweight"
        ExerciseLoggingMode.TIMED -> "Timed"
    }

internal fun exerciseMetadataLine(
    subtitle: String,
    isBodyweight: Boolean,
    separator: String
): String =
    (subtitle.split(Regex("\\s+[•·]\\s+")) + exerciseKindLabel(isBodyweight))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .joinToString(separator)

internal fun exerciseMetadataLine(
    subtitle: String,
    loggingMode: ExerciseLoggingMode,
    separator: String
): String =
    (subtitle.split(Regex("\\s+[•·]\\s+")) + exerciseKindLabel(loggingMode))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .joinToString(separator)
