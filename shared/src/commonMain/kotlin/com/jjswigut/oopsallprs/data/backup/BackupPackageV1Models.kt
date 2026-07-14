package com.jjswigut.oopsallprs.data.backup

import kotlinx.serialization.Serializable

internal const val BACKUP_FORMAT_VERSION_V1: Int = 1

/** The released V1 JSON contract. Do not add fields or change enum spellings in these DTOs. */
@Serializable
internal data class BackupPackageV1Dto(
    val formatVersion: Int,
    val createdAt: Long,
    val deviceId: String,
    val lastLocalRevision: String,
    val appSchemaVersion: Int,
    val summary: SnapshotSummaryV1Dto,
    val preferences: PreferencesSnapshotV1Dto,
    val exercises: List<ExerciseCatalogItemV1Dto>,
    val routines: List<RoutineV1Dto>,
    val activeWorkout: ActiveWorkoutV1Dto?,
    val activeSession: ActiveSessionV1Dto?,
    val activeUxSession: ActiveWorkoutUxSessionV1Dto?,
    val activeSetDrafts: List<PersistedSetDraftV1Dto>,
    val completedWorkouts: List<CompletedWorkoutV1Dto>,
    val personalRecords: List<PersonalRecordV1Dto>,
    val progressPoints: List<ProgressPointV1Dto>,
    val exportMetadata: List<ExportSnapshotV1Dto>
)

@Serializable internal data class SnapshotSummaryV1Dto(val workoutCount: Int, val setCount: Int, val routineCount: Int, val customExerciseCount: Int, val progressRecordCount: Int, val hasActiveWorkout: Boolean, val latestWorkoutTimestamp: Long?, val latestUpdatedTimestamp: Long?)
@Serializable internal data class PreferencesSnapshotV1Dto(val weightUnit: String, val weightStepPounds: Double, val weightStepKilograms: Double, val defaultRestSeconds: Int, val restSoundEnabled: Boolean)
@Serializable internal data class ExerciseCatalogItemV1Dto(val id: String, val canonicalName: String, val displayName: String, val muscleGroup: String, val equipment: String, val movementPattern: String, val exerciseType: String, val experienceLevel: String, val bodyRegion: String, val isBodyweight: Boolean, val loggingMode: String, val isUserCreated: Boolean, val createdAt: Long, val updatedAt: Long, val archivedAt: Long?, val sourceSeedVersion: String?, val userNotes: String?)
@Serializable internal data class RestConfigurationV1Dto(val durationSeconds: Int, val autoStart: Boolean)
@Serializable internal data class ActiveWorkoutV1Dto(val id: String, val startedAt: Long, val routineId: String?, val routineSnapshotName: String?, val exercises: List<ActiveExerciseV1Dto>, val createdAt: Long, val updatedAt: Long, val status: String)
@Serializable internal data class ActiveExerciseV1Dto(val id: String, val activeWorkoutId: String, val exerciseCatalogId: String, val displayNameSnapshot: String, val equipmentSnapshot: String?, val isBodyweight: Boolean, val loggingMode: String, val position: Int, val groupId: String? = null, val groupPosition: Int? = null, val groupLabel: String? = null, val groupRounds: Int? = null, val sets: List<ExerciseSetV1Dto>, val rest: RestConfigurationV1Dto)
@Serializable internal data class ExerciseSetV1Dto(val id: String, val exerciseInstanceId: String, val position: Int, val setKind: String, val weightKg: Double?, val reps: Int?, val durationMs: Long?, val loggedAt: Long?, val createdAt: Long, val updatedAt: Long, val editedAt: Long?)
@Serializable internal data class ActiveSessionV1Dto(val activeWorkoutId: String?, val startedAt: Long?, val restEndsAt: Long?, val restStartedAt: Long?, val restOriginSetId: String?, val lastOpenedRoute: String?, val updatedAt: Long)
@Serializable internal data class ActiveWorkoutUxSessionV1Dto(val activeWorkoutId: String, val focusedExerciseInstanceId: String?, val focusedDraftId: String?, val updatedAt: Long)
@Serializable internal data class PersistedSetDraftV1Dto(val draftId: String, val activeWorkoutId: String, val exerciseInstanceId: String, val position: Int, val setKind: String, val reps: Int?, val weightKg: Double?, val durationMs: Long?, val timerStartedAt: Long?, val updatedAt: Long)
@Serializable internal data class RoutineV1Dto(val id: String, val name: String, val exercises: List<RoutineExerciseV1Dto>, val createdAt: Long, val updatedAt: Long, val sourceCompletedWorkoutId: String?, val archivedAt: Long?)
@Serializable internal data class RoutineExerciseV1Dto(val id: String, val routineId: String, val exerciseCatalogId: String, val displayNameSnapshot: String, val position: Int, val groupId: String? = null, val groupPosition: Int? = null, val groupRounds: Int? = null, val plannedSets: List<RoutineSetTemplateV1Dto>, val rest: RestConfigurationV1Dto)
@Serializable internal data class RoutineSetTemplateV1Dto(val id: String, val routineExerciseId: String, val position: Int, val targetWeightKg: Double?, val targetReps: Int?, val targetDurationMs: Long?, val setKind: String)
@Serializable internal data class CompletedWorkoutV1Dto(val id: String, val sourceActiveWorkoutId: String, val startedAt: Long, val finishedAt: Long, val durationMs: Long, val routineId: String?, val exercises: List<CompletedExerciseV1Dto>, val createdAt: Long)
@Serializable internal data class CompletedExerciseV1Dto(val id: String, val completedWorkoutId: String, val exerciseCatalogId: String, val displayNameSnapshot: String, val position: Int, val loggedSets: List<ExerciseSetV1Dto>, val rest: RestConfigurationV1Dto)
@Serializable internal data class PersonalRecordV1Dto(val id: String, val exerciseCatalogId: String, val recordKind: String, val reps: Int?, val weightKg: Double?, val value: Double, val sourceWorkoutId: String, val sourceSetId: String, val achievedAt: Long, val createdAt: Long)
@Serializable internal data class ProgressPointV1Dto(val id: String, val exerciseCatalogId: String, val sourceWorkoutId: String, val sourceSetId: String?, val metric: String, val value: Double, val weightKg: Double?, val reps: Int?, val recordedAt: Long)
@Serializable internal data class ExportSnapshotV1Dto(val id: String, val exportType: String, val createdAt: Long, val weightUnit: String, val rowCount: Int, val formatVersion: Int)
