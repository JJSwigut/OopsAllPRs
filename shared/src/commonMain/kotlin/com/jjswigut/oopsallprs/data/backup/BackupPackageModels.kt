package com.jjswigut.oopsallprs.data.backup

import kotlinx.serialization.Serializable

const val BACKUP_FORMAT_VERSION: Int = 1
const val BACKUP_FILE_EXTENSION: String = "json"

@Serializable
data class BackupPackageDto(
    val formatVersion: Int,
    val createdAt: Long,
    val deviceId: String,
    val lastLocalRevision: String,
    val appSchemaVersion: Int,
    val summary: SnapshotSummaryDto,
    val preferences: PreferencesSnapshotDto,
    val exercises: List<ExerciseCatalogItemDto>,
    val routines: List<RoutineDto>,
    val activeWorkout: ActiveWorkoutDto?,
    val activeSession: ActiveSessionDto?,
    val activeUxSession: ActiveWorkoutUxSessionDto?,
    val activeSetDrafts: List<PersistedSetDraftDto>,
    val completedWorkouts: List<CompletedWorkoutDto>,
    val personalRecords: List<PersonalRecordDto>,
    val progressPoints: List<ProgressPointDto>,
    val exportMetadata: List<ExportSnapshotDto>
)

@Serializable
data class SnapshotSummaryDto(
    val workoutCount: Int,
    val setCount: Int,
    val routineCount: Int,
    val customExerciseCount: Int,
    val progressRecordCount: Int,
    val hasActiveWorkout: Boolean,
    val latestWorkoutTimestamp: Long?,
    val latestUpdatedTimestamp: Long?
)

@Serializable
data class PreferencesSnapshotDto(
    val weightUnit: String,
    val weightStepPounds: Double,
    val weightStepKilograms: Double,
    val defaultRestSeconds: Int,
    val restSoundEnabled: Boolean
)

@Serializable
data class ExerciseCatalogItemDto(
    val id: String,
    val canonicalName: String,
    val displayName: String,
    val muscleGroup: String,
    val equipment: String,
    val movementPattern: String,
    val exerciseType: String,
    val experienceLevel: String,
    val bodyRegion: String,
    val isBodyweight: Boolean,
    val loggingMode: String,
    val isUserCreated: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    val sourceSeedVersion: String?,
    val userNotes: String?
)

@Serializable
data class RestConfigurationDto(
    val durationSeconds: Int,
    val autoStart: Boolean
)

@Serializable
data class ActiveWorkoutDto(
    val id: String,
    val startedAt: Long,
    val routineId: String?,
    val routineSnapshotName: String?,
    val exercises: List<ActiveExerciseDto>,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String
)

@Serializable
data class ActiveExerciseDto(
    val id: String,
    val activeWorkoutId: String,
    val exerciseCatalogId: String,
    val displayNameSnapshot: String,
    val equipmentSnapshot: String?,
    val isBodyweight: Boolean,
    val loggingMode: String,
    val position: Int,
    val sets: List<ExerciseSetDto>,
    val rest: RestConfigurationDto
)

@Serializable
data class ExerciseSetDto(
    val id: String,
    val exerciseInstanceId: String,
    val position: Int,
    val setKind: String,
    val weightKg: Double?,
    val reps: Int?,
    val durationMs: Long?,
    val loggedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val editedAt: Long?
)

@Serializable
data class ActiveSessionDto(
    val activeWorkoutId: String?,
    val startedAt: Long?,
    val restEndsAt: Long?,
    val restStartedAt: Long?,
    val restOriginSetId: String?,
    val lastOpenedRoute: String?,
    val updatedAt: Long
)

@Serializable
data class ActiveWorkoutUxSessionDto(
    val activeWorkoutId: String,
    val focusedExerciseInstanceId: String?,
    val focusedDraftId: String?,
    val updatedAt: Long
)

@Serializable
data class PersistedSetDraftDto(
    val draftId: String,
    val activeWorkoutId: String,
    val exerciseInstanceId: String,
    val position: Int,
    val setKind: String,
    val reps: Int?,
    val weightKg: Double?,
    val durationMs: Long?,
    val timerStartedAt: Long?,
    val updatedAt: Long
)

@Serializable
data class RoutineDto(
    val id: String,
    val name: String,
    val exercises: List<RoutineExerciseDto>,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceCompletedWorkoutId: String?,
    val archivedAt: Long?
)

@Serializable
data class RoutineExerciseDto(
    val id: String,
    val routineId: String,
    val exerciseCatalogId: String,
    val displayNameSnapshot: String,
    val position: Int,
    val plannedSets: List<RoutineSetTemplateDto>,
    val rest: RestConfigurationDto
)

@Serializable
data class RoutineSetTemplateDto(
    val id: String,
    val routineExerciseId: String,
    val position: Int,
    val targetWeightKg: Double?,
    val targetReps: Int?,
    val targetDurationMs: Long?,
    val setKind: String
)

@Serializable
data class CompletedWorkoutDto(
    val id: String,
    val sourceActiveWorkoutId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMs: Long,
    val routineId: String?,
    val exercises: List<CompletedExerciseDto>,
    val createdAt: Long
)

@Serializable
data class CompletedExerciseDto(
    val id: String,
    val completedWorkoutId: String,
    val exerciseCatalogId: String,
    val displayNameSnapshot: String,
    val position: Int,
    val loggedSets: List<ExerciseSetDto>,
    val rest: RestConfigurationDto
)

@Serializable
data class PersonalRecordDto(
    val id: String,
    val exerciseCatalogId: String,
    val recordKind: String,
    val reps: Int?,
    val weightKg: Double?,
    val value: Double,
    val sourceWorkoutId: String,
    val sourceSetId: String,
    val achievedAt: Long,
    val createdAt: Long
)

@Serializable
data class ProgressPointDto(
    val id: String,
    val exerciseCatalogId: String,
    val sourceWorkoutId: String,
    val sourceSetId: String?,
    val metric: String,
    val value: Double,
    val weightKg: Double?,
    val reps: Int?,
    val recordedAt: Long
)

@Serializable
data class ExportSnapshotDto(
    val id: String,
    val exportType: String,
    val createdAt: Long,
    val weightUnit: String,
    val rowCount: Int,
    val formatVersion: Int
)

typealias BackupPackage = BackupPackageDto
