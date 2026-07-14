package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline

enum class ExerciseLoggingMode {
    WEIGHTED,
    BODYWEIGHT,
    TIMED
}

enum class ExerciseDefinitionOrigin(code: String) : WireCoded {
    SEED("seed"),
    USER("user");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "exercise definition origin")

        fun fromWireCode(value: String): ExerciseDefinitionOrigin? = byWireCode[value]
    }
}

@JvmInline
value class ExerciseDefinitionRevision(val value: Long) {
    init {
        require(value > 0L) { "Exercise definition revision must be greater than zero" }
    }
}

@JvmInline
value class ExerciseSeedKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Exercise seed key cannot be blank" }
    }

    override fun toString(): String = value
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
    val userNotes: String? = null,
    val origin: ExerciseDefinitionOrigin = if (isUserCreated) ExerciseDefinitionOrigin.USER else ExerciseDefinitionOrigin.SEED,
    val definitionRevision: ExerciseDefinitionRevision = ExerciseDefinitionRevision(1),
    val seedKey: ExerciseSeedKey? = if (origin == ExerciseDefinitionOrigin.SEED) {
        canonicalExerciseSeedKey(canonicalName)
    } else {
        null
    },
    val defaultLoggingConfiguration: LoggingConfiguration = loggingMode.toLegacyLoggingConfiguration()
) {
    init {
        require(isUserCreated == (origin == ExerciseDefinitionOrigin.USER)) {
            "Exercise origin must agree with isUserCreated"
        }
        require((origin == ExerciseDefinitionOrigin.SEED) == (seedKey != null)) {
            "Seed exercises require a seed key and user exercises cannot carry one"
        }
    }
}

data class UserExerciseConfiguration(
    val exerciseDefinitionId: FoundationId,
    val configuration: LoggingConfiguration,
    val basedOnDefinitionRevision: ExerciseDefinitionRevision,
    val configuredAt: Instant
)

fun ExerciseCatalogItem.resolveLoggingConfiguration(
    userDefault: UserExerciseConfiguration? = null,
    workoutOverride: LoggingConfiguration? = null
): ResolvedLoggingConfiguration =
    LoggingConfigurationResolver.resolve(this, userDefault, workoutOverride)

fun ExerciseCatalogItem.snapshotReference(
    userDefault: UserExerciseConfiguration? = null,
    workoutOverride: LoggingConfiguration? = null
): ExerciseReference =
    ExerciseReference(
        exerciseCatalogId = id,
        displayNameSnapshot = displayName,
        isBodyweight = isBodyweight,
        loggingMode = loggingMode,
        equipmentSnapshot = equipment,
        definitionOriginSnapshot = origin,
        definitionRevisionSnapshot = definitionRevision,
        seedKeySnapshot = seedKey,
        resolvedLoggingConfiguration = resolveLoggingConfiguration(userDefault, workoutOverride)
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

fun canonicalExerciseSeedKey(canonicalName: String): ExerciseSeedKey =
    ExerciseSeedKey("oopsallprs_baseline:${canonicalExerciseName(canonicalName)}")

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
