package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.exercise.ExerciseCsvParser
import com.jjswigut.oopsallprs.data.exercise.ExerciseSeedIngestion
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.canonicalExerciseName
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.model.toLegacyLoggingConfiguration
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ExerciseCatalogUseCases(
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository? = null,
    private val loggingConfigurations: LoggingConfigurationRepository? = null
) {
    suspend fun defaultResults(limit: Int = DEFAULT_LIMIT): List<ExerciseCatalogItem> =
        exercises.all().take(limit)

    suspend fun recentResults(limit: Int = RECENT_LIMIT): List<ExerciseCatalogItem> {
        val catalogById = exercises.all().associateBy { it.id }
        return workouts?.completedWorkouts().orEmpty()
            .sortedByDescending { it.finishedAt }
            .flatMap { workout -> workout.exercises.sortedBy { it.position.value } }
            .mapNotNull { completed -> catalogById[completed.exerciseCatalogId] }
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun search(query: String, limit: Int = DEFAULT_LIMIT): List<ExerciseCatalogItem> {
        val trimmed = query.trim()
        val results = if (trimmed.isEmpty()) {
            exercises.all()
        } else {
            exercises.search(trimmed)
        }
        return results.take(limit)
    }

    suspend fun exercise(id: com.jjswigut.oopsallprs.domain.model.FoundationId): ExerciseCatalogItem? =
        exercises.exercise(id)

    suspend fun userCreatedExercises(): List<ExerciseCatalogItem> =
        exercises.userCreatedExercises()

    suspend fun createCustomExercise(
        name: String,
        isBodyweight: Boolean,
        now: Instant = Clock.System.now(),
        loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
        defaultLoggingConfiguration: LoggingConfiguration? = null
    ): FoundationResult<ExerciseCatalogItem> {
        val displayName = name.trim()
        if (displayName.isEmpty()) {
            return foundationFailure(FoundationError.Validation("Exercise name cannot be blank"))
        }
        val requestedConfiguration = defaultLoggingConfiguration ?: loggingMode.toLegacyLoggingConfiguration()
        val canonicalConfiguration = when (
            val saved = loggingConfigurations?.saveLoggingConfiguration(requestedConfiguration)
                ?: foundationSuccess(requestedConfiguration)
        ) {
            is FoundationResult.Failure -> return saved
            is FoundationResult.Success -> saved.value
        }
        val item = ExerciseCatalogItem(
            id = newFoundationId("exercise"),
            canonicalName = canonicalExerciseName(displayName),
            displayName = displayName,
            muscleGroup = "Custom",
            equipment = if (isBodyweight || loggingMode == ExerciseLoggingMode.TIMED) "Bodyweight" else "Custom",
            movementPattern = "Custom",
            exerciseType = when (loggingMode) {
                ExerciseLoggingMode.TIMED -> "Static Hold"
                ExerciseLoggingMode.BODYWEIGHT -> "Bodyweight"
                ExerciseLoggingMode.WEIGHTED -> "Strength"
            },
            experienceLevel = "Custom",
            bodyRegion = "Custom",
            isBodyweight = isBodyweight || loggingMode == ExerciseLoggingMode.TIMED,
            loggingMode = loggingMode,
            defaultLoggingConfiguration = canonicalConfiguration,
            isUserCreated = true,
            createdAt = now,
            updatedAt = now
        )
        return exercises.saveUserExercise(item)
    }

    suspend fun updateCustomExercise(
        id: com.jjswigut.oopsallprs.domain.model.FoundationId,
        name: String,
        isBodyweight: Boolean,
        now: Instant = Clock.System.now(),
        loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
        defaultLoggingConfiguration: LoggingConfiguration? = null
    ): FoundationResult<ExerciseCatalogItem> {
        val existing = exercises.exercise(id)
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $id"))
        if (!existing.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Seeded exercises cannot be edited"))
        }
        val displayName = name.trim()
        if (displayName.isEmpty()) {
            return foundationFailure(FoundationError.Validation("Exercise name cannot be blank"))
        }
        val requestedConfiguration = defaultLoggingConfiguration
            ?: existing.defaultLoggingConfiguration.takeIf { loggingMode == existing.loggingMode }
            ?: loggingMode.toLegacyLoggingConfiguration()
        val canonicalConfiguration = when (
            val saved = loggingConfigurations?.saveLoggingConfiguration(requestedConfiguration)
                ?: foundationSuccess(requestedConfiguration)
        ) {
            is FoundationResult.Failure -> return saved
            is FoundationResult.Success -> saved.value
        }
        val updated = existing.copy(
            canonicalName = canonicalExerciseName(displayName),
            displayName = displayName,
            equipment = if (isBodyweight || loggingMode == ExerciseLoggingMode.TIMED) "Bodyweight" else "Custom",
            exerciseType = when (loggingMode) {
                ExerciseLoggingMode.TIMED -> "Static Hold"
                ExerciseLoggingMode.BODYWEIGHT -> "Bodyweight"
                ExerciseLoggingMode.WEIGHTED -> "Strength"
            },
            isBodyweight = isBodyweight || loggingMode == ExerciseLoggingMode.TIMED,
            loggingMode = loggingMode,
            defaultLoggingConfiguration = canonicalConfiguration,
            updatedAt = now,
            archivedAt = null,
            sourceSeedVersion = null
        )
        return exercises.updateUserExercise(updated)
    }

    suspend fun archiveCustomExercise(
        id: com.jjswigut.oopsallprs.domain.model.FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<Unit> =
        exercises.archiveUserExercise(id, now)

    suspend fun ensureSeeded(csv: String): FoundationResult<ExerciseSeedImport?> {
        val existingCanonicalNames = exercises.all().map { it.canonicalName }.toSet()
        val seedRows = ExerciseCsvParser.parse(csv).acceptedRows
        val hasMissingSeedRows = seedRows.any { it.canonicalName !in existingCanonicalNames }
        if (existingCanonicalNames.isNotEmpty() && !hasMissingSeedRows) {
            return foundationSuccess(null)
        }
        return when (val result = ExerciseSeedIngestion(exercises).ingest(csv)) {
            is FoundationResult.Failure -> result
            is FoundationResult.Success -> foundationSuccess(result.value)
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 1_000
        const val RECENT_LIMIT = 8
    }
}
