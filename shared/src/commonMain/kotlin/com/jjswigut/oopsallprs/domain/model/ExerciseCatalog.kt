package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

enum class ExerciseLoggingMode {
    WEIGHTED,
    BODYWEIGHT,
    TIMED
}

data class ExerciseCatalogItem(
    val id: FoundationId,
    val canonicalName: String,
    val displayName: String,
    val muscleGroup: String,
    val equipment: String,
    val movementPattern: String,
    val exerciseType: String,
    val experienceLevel: String,
    val bodyRegion: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode = defaultExerciseLoggingMode(
        canonicalName = canonicalName,
        movementPattern = movementPattern,
        exerciseType = exerciseType,
        isBodyweight = isBodyweight
    ),
    val isUserCreated: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null,
    val sourceSeedVersion: String? = null,
    val userNotes: String? = null
)

data class ExerciseSeedImport(
    val id: FoundationId,
    val sourceName: String,
    val sourceHash: String,
    val importedAt: Instant,
    val rowCount: Int,
    val rejectedRowCount: Int,
    val warnings: List<String>
)

data class ExerciseSeedRow(
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val movementPattern: String,
    val exerciseType: String,
    val experienceLevel: String,
    val bodyRegion: String,
    val rowNumber: Int
) {
    val canonicalName: String = canonicalExerciseName(exerciseName)
    val isBodyweight: Boolean =
        equipment.equals("Bodyweight", ignoreCase = true) ||
            exerciseType.contains("Bodyweight", ignoreCase = true) ||
            exerciseName.contains("Push-Up", ignoreCase = true) ||
            exerciseName.contains("Pull-Up", ignoreCase = true) ||
            exerciseName.contains("Chin-Up", ignoreCase = true)
    val loggingMode: ExerciseLoggingMode = defaultExerciseLoggingMode(
        canonicalName = canonicalName,
        movementPattern = movementPattern,
        exerciseType = exerciseType,
        isBodyweight = isBodyweight
    )
}

data class SeedImportReport(
    val acceptedRows: List<ExerciseSeedRow>,
    val rejectedRows: List<RejectedSeedRow>,
    val duplicateCanonicalNames: Set<String>
)

data class RejectedSeedRow(
    val rowNumber: Int,
    val rawValue: String,
    val reason: String
)

fun canonicalExerciseName(name: String): String =
    name.trim().lowercase().replace(Regex("\\s+"), " ")

fun defaultExerciseLoggingMode(
    canonicalName: String,
    movementPattern: String,
    exerciseType: String,
    isBodyweight: Boolean
): ExerciseLoggingMode =
    when {
        isTimedExerciseName(canonicalName) ||
            movementPattern.equals("Static Hold", ignoreCase = true) ||
            exerciseType.equals("Static Hold", ignoreCase = true) -> ExerciseLoggingMode.TIMED
        isBodyweight -> ExerciseLoggingMode.BODYWEIGHT
        else -> ExerciseLoggingMode.WEIGHTED
    }

private fun isTimedExerciseName(canonicalName: String): Boolean {
    val name = canonicalName.lowercase()
    val explicitTimed = listOf(
        "plank",
        "side plank",
        "wall sit",
        "dead hang",
        "hollow hold",
        "l-sit",
        "l sit"
    )
    return explicitTimed.any { name == it || name.contains(it) }
}
