package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.ExportSnapshot
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.WorkoutStatus
import kotlinx.datetime.Instant

internal fun Instant.toBackupMillis(): Long = toEpochMilliseconds()
internal fun Long.toBackupInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun SnapshotSummary.toDto(): SnapshotSummaryDto =
    SnapshotSummaryDto(
        workoutCount = workoutCount,
        setCount = setCount,
        routineCount = routineCount,
        customExerciseCount = customExerciseCount,
        progressRecordCount = progressRecordCount,
        hasActiveWorkout = hasActiveWorkout,
        latestWorkoutTimestamp = latestWorkoutTimestamp?.toBackupMillis(),
        latestUpdatedTimestamp = latestUpdatedTimestamp?.toBackupMillis()
    )

internal fun SnapshotSummaryDto.toDomain(): SnapshotSummary =
    SnapshotSummary(
        workoutCount = workoutCount,
        setCount = setCount,
        routineCount = routineCount,
        customExerciseCount = customExerciseCount,
        progressRecordCount = progressRecordCount,
        hasActiveWorkout = hasActiveWorkout,
        latestWorkoutTimestamp = latestWorkoutTimestamp?.toBackupInstant(),
        latestUpdatedTimestamp = latestUpdatedTimestamp?.toBackupInstant()
    )

internal fun RestConfiguration.toDto(): RestConfigurationDto =
    RestConfigurationDto(durationSeconds = durationSeconds, autoStart = autoStart)

internal fun RestConfigurationDto.toDomain(): RestConfiguration =
    RestConfiguration(durationSeconds = durationSeconds, autoStart = autoStart)

internal fun ExerciseCatalogItem.toDto(): ExerciseCatalogItemDto =
    ExerciseCatalogItemDto(
        id = id.value,
        canonicalName = canonicalName,
        displayName = displayName,
        muscleGroup = muscleGroup,
        equipment = equipment,
        movementPattern = movementPattern,
        exerciseType = exerciseType,
        experienceLevel = experienceLevel,
        bodyRegion = bodyRegion,
        isBodyweight = isBodyweight,
        loggingMode = loggingMode.name,
        isUserCreated = isUserCreated,
        createdAt = createdAt.toBackupMillis(),
        updatedAt = updatedAt.toBackupMillis(),
        archivedAt = archivedAt?.toBackupMillis(),
        sourceSeedVersion = sourceSeedVersion,
        userNotes = userNotes
    )

internal fun ExerciseCatalogItemDto.toDomain(): ExerciseCatalogItem =
    ExerciseCatalogItem(
        id = FoundationId(id),
        canonicalName = canonicalName,
        displayName = displayName,
        muscleGroup = muscleGroup,
        equipment = equipment,
        movementPattern = movementPattern,
        exerciseType = exerciseType,
        experienceLevel = experienceLevel,
        bodyRegion = bodyRegion,
        isBodyweight = isBodyweight,
        loggingMode = ExerciseLoggingMode.valueOf(loggingMode),
        isUserCreated = isUserCreated,
        createdAt = createdAt.toBackupInstant(),
        updatedAt = updatedAt.toBackupInstant(),
        archivedAt = archivedAt?.toBackupInstant(),
        sourceSeedVersion = sourceSeedVersion,
        userNotes = userNotes
    )

internal fun ExerciseSet.toDto(): ExerciseSetDto =
    ExerciseSetDto(
        id = id.value,
        exerciseInstanceId = exerciseInstanceId.value,
        position = position.value,
        setKind = setKind.name,
        weightKg = weight?.value,
        reps = reps,
        durationMs = durationMs,
        loggedAt = loggedAt?.toBackupMillis(),
        createdAt = createdAt.toBackupMillis(),
        updatedAt = updatedAt.toBackupMillis(),
        editedAt = editedAt?.toBackupMillis()
    )

internal fun ExerciseSetDto.toDomain(): ExerciseSet =
    ExerciseSet(
        id = FoundationId(id),
        exerciseInstanceId = FoundationId(exerciseInstanceId),
        position = OrderedPosition(position),
        setKind = SetKind.valueOf(setKind),
        weight = weightKg?.let(::WeightKg),
        reps = reps,
        loggedAt = loggedAt?.toBackupInstant(),
        createdAt = createdAt.toBackupInstant(),
        updatedAt = updatedAt.toBackupInstant(),
        editedAt = editedAt?.toBackupInstant(),
        durationMs = durationMs
    )

internal fun ActiveExercise.toDto(): ActiveExerciseDto =
    ActiveExerciseDto(
        id = id.value,
        activeWorkoutId = activeWorkoutId.value,
        exerciseCatalogId = reference.exerciseCatalogId.value,
        displayNameSnapshot = reference.displayNameSnapshot,
        equipmentSnapshot = reference.equipmentSnapshot,
        isBodyweight = reference.isBodyweight,
        loggingMode = reference.loggingMode.name,
        position = position.value,
        sets = sets.map { it.toDto() },
        rest = rest.toDto()
    )

internal fun ActiveExerciseDto.toDomain(): ActiveExercise =
    ActiveExercise(
        id = FoundationId(id),
        activeWorkoutId = FoundationId(activeWorkoutId),
        reference = ExerciseReference(
            exerciseCatalogId = FoundationId(exerciseCatalogId),
            displayNameSnapshot = displayNameSnapshot,
            isBodyweight = isBodyweight,
            loggingMode = ExerciseLoggingMode.valueOf(loggingMode),
            equipmentSnapshot = equipmentSnapshot
        ),
        position = OrderedPosition(position),
        sets = sets.map { it.toDomain() },
        rest = rest.toDomain()
    )

internal fun ActiveWorkout.toDto(): ActiveWorkoutDto =
    ActiveWorkoutDto(
        id = id.value,
        startedAt = startedAt.toBackupMillis(),
        routineId = routineId?.value,
        routineSnapshotName = routineSnapshotName,
        exercises = exercises.map { it.toDto() },
        createdAt = createdAt.toBackupMillis(),
        updatedAt = updatedAt.toBackupMillis(),
        status = status.name
    )

internal fun ActiveWorkoutDto.toDomain(): ActiveWorkout =
    ActiveWorkout(
        id = FoundationId(id),
        startedAt = startedAt.toBackupInstant(),
        routineId = routineId?.let(::FoundationId),
        routineSnapshotName = routineSnapshotName,
        exercises = exercises.map { it.toDomain() },
        createdAt = createdAt.toBackupInstant(),
        updatedAt = updatedAt.toBackupInstant(),
        status = WorkoutStatus.valueOf(status)
    )

internal fun ActiveSessionState.toDto(): ActiveSessionDto =
    ActiveSessionDto(
        activeWorkoutId = activeWorkoutId?.value,
        startedAt = startedAt?.toBackupMillis(),
        restEndsAt = restEndsAt?.toBackupMillis(),
        restStartedAt = restStartedAt?.toBackupMillis(),
        restOriginSetId = restOriginSetId?.value,
        lastOpenedRoute = lastOpenedRoute,
        updatedAt = updatedAt.toBackupMillis()
    )

internal fun ActiveSessionDto.toDomain(): ActiveSessionState =
    ActiveSessionState(
        activeWorkoutId = activeWorkoutId?.let(::FoundationId),
        startedAt = startedAt?.toBackupInstant(),
        restEndsAt = restEndsAt?.toBackupInstant(),
        restStartedAt = restStartedAt?.toBackupInstant(),
        restOriginSetId = restOriginSetId?.let(::FoundationId),
        lastOpenedRoute = lastOpenedRoute,
        updatedAt = updatedAt.toBackupInstant()
    )

internal fun ActiveWorkoutUxSession.toDto(): ActiveWorkoutUxSessionDto =
    ActiveWorkoutUxSessionDto(
        activeWorkoutId = activeWorkoutId.value,
        focusedExerciseInstanceId = focusedExerciseInstanceId?.value,
        focusedDraftId = focusedDraftId?.value,
        updatedAt = updatedAt.toBackupMillis()
    )

internal fun ActiveWorkoutUxSessionDto.toDomain(): ActiveWorkoutUxSession =
    ActiveWorkoutUxSession(
        activeWorkoutId = FoundationId(activeWorkoutId),
        focusedExerciseInstanceId = focusedExerciseInstanceId?.let(::FoundationId),
        focusedDraftId = focusedDraftId?.let(::FoundationId),
        updatedAt = updatedAt.toBackupInstant()
    )

internal fun PersistedSetDraft.toDto(): PersistedSetDraftDto =
    PersistedSetDraftDto(
        draftId = draftId.value,
        activeWorkoutId = activeWorkoutId.value,
        exerciseInstanceId = exerciseInstanceId.value,
        position = position.value,
        setKind = setKind.name,
        reps = reps,
        weightKg = weight?.value,
        durationMs = durationMs,
        timerStartedAt = timerStartedAt?.toBackupMillis(),
        updatedAt = updatedAt.toBackupMillis()
    )

internal fun PersistedSetDraftDto.toDomain(): PersistedSetDraft =
    PersistedSetDraft(
        draftId = FoundationId(draftId),
        activeWorkoutId = FoundationId(activeWorkoutId),
        exerciseInstanceId = FoundationId(exerciseInstanceId),
        position = OrderedPosition(position),
        setKind = SetKind.valueOf(setKind),
        reps = reps,
        weight = weightKg?.let(::WeightKg),
        durationMs = durationMs,
        timerStartedAt = timerStartedAt?.toBackupInstant(),
        updatedAt = updatedAt.toBackupInstant()
    )

internal fun RoutineSetTemplate.toDto(): RoutineSetTemplateDto =
    RoutineSetTemplateDto(
        id = id.value,
        routineExerciseId = routineExerciseId.value,
        position = position.value,
        targetWeightKg = targetWeight?.value,
        targetReps = targetReps,
        targetDurationMs = targetDurationMs,
        setKind = setKind.name
    )

internal fun RoutineSetTemplateDto.toDomain(): RoutineSetTemplate =
    RoutineSetTemplate(
        id = FoundationId(id),
        routineExerciseId = FoundationId(routineExerciseId),
        position = OrderedPosition(position),
        targetWeight = targetWeightKg?.let(::WeightKg),
        targetReps = targetReps,
        targetDurationMs = targetDurationMs,
        setKind = SetKind.valueOf(setKind)
    )

internal fun RoutineExercise.toDto(): RoutineExerciseDto =
    RoutineExerciseDto(
        id = id.value,
        routineId = routineId.value,
        exerciseCatalogId = exerciseCatalogId.value,
        displayNameSnapshot = displayNameSnapshot,
        position = position.value,
        plannedSets = plannedSets.map { it.toDto() },
        rest = rest.toDto()
    )

internal fun RoutineExerciseDto.toDomain(): RoutineExercise =
    RoutineExercise(
        id = FoundationId(id),
        routineId = FoundationId(routineId),
        exerciseCatalogId = FoundationId(exerciseCatalogId),
        displayNameSnapshot = displayNameSnapshot,
        position = OrderedPosition(position),
        plannedSets = plannedSets.map { it.toDomain() },
        rest = rest.toDomain()
    )

internal fun ReusableRoutine.toDto(): RoutineDto =
    RoutineDto(
        id = id.value,
        name = name,
        exercises = exercises.map { it.toDto() },
        createdAt = createdAt.toBackupMillis(),
        updatedAt = updatedAt.toBackupMillis(),
        sourceCompletedWorkoutId = sourceCompletedWorkoutId?.value,
        archivedAt = archivedAt?.toBackupMillis()
    )

internal fun RoutineDto.toDomain(): ReusableRoutine =
    ReusableRoutine(
        id = FoundationId(id),
        name = name,
        exercises = exercises.map { it.toDomain() },
        createdAt = createdAt.toBackupInstant(),
        updatedAt = updatedAt.toBackupInstant(),
        sourceCompletedWorkoutId = sourceCompletedWorkoutId?.let(::FoundationId),
        archivedAt = archivedAt?.toBackupInstant()
    )

internal fun CompletedExercise.toDto(): CompletedExerciseDto =
    CompletedExerciseDto(
        id = id.value,
        completedWorkoutId = completedWorkoutId.value,
        exerciseCatalogId = exerciseCatalogId.value,
        displayNameSnapshot = displayNameSnapshot,
        position = position.value,
        loggedSets = loggedSets.map { it.toDto() },
        rest = rest.toDto()
    )

internal fun CompletedExerciseDto.toDomain(): CompletedExercise =
    CompletedExercise(
        id = FoundationId(id),
        completedWorkoutId = FoundationId(completedWorkoutId),
        exerciseCatalogId = FoundationId(exerciseCatalogId),
        displayNameSnapshot = displayNameSnapshot,
        position = OrderedPosition(position),
        loggedSets = loggedSets.map { it.toDomain() },
        rest = rest.toDomain()
    )

internal fun CompletedWorkout.toDto(): CompletedWorkoutDto =
    CompletedWorkoutDto(
        id = id.value,
        sourceActiveWorkoutId = sourceActiveWorkoutId.value,
        startedAt = startedAt.toBackupMillis(),
        finishedAt = finishedAt.toBackupMillis(),
        durationMs = durationMs,
        routineId = routineId?.value,
        exercises = exercises.map { it.toDto() },
        createdAt = createdAt.toBackupMillis()
    )

internal fun CompletedWorkoutDto.toDomain(): CompletedWorkout =
    CompletedWorkout(
        id = FoundationId(id),
        sourceActiveWorkoutId = FoundationId(sourceActiveWorkoutId),
        startedAt = startedAt.toBackupInstant(),
        finishedAt = finishedAt.toBackupInstant(),
        durationMs = durationMs,
        routineId = routineId?.let(::FoundationId),
        exercises = exercises.map { it.toDomain() },
        createdAt = createdAt.toBackupInstant()
    )

internal fun PersonalRecord.toDto(): PersonalRecordDto =
    PersonalRecordDto(
        id = id.value,
        exerciseCatalogId = exerciseCatalogId.value,
        recordKind = recordKind.name,
        reps = reps,
        weightKg = weight?.value,
        value = value,
        sourceWorkoutId = sourceWorkoutId.value,
        sourceSetId = sourceSetId.value,
        achievedAt = achievedAt.toBackupMillis(),
        createdAt = createdAt.toBackupMillis()
    )

internal fun PersonalRecordDto.toDomain(): PersonalRecord =
    PersonalRecord(
        id = FoundationId(id),
        exerciseCatalogId = FoundationId(exerciseCatalogId),
        recordKind = PersonalRecordKind.valueOf(recordKind),
        reps = reps,
        weight = weightKg?.let(::WeightKg),
        value = value,
        sourceWorkoutId = FoundationId(sourceWorkoutId),
        sourceSetId = FoundationId(sourceSetId),
        achievedAt = achievedAt.toBackupInstant(),
        createdAt = createdAt.toBackupInstant()
    )

internal fun ProgressPoint.toDto(): ProgressPointDto =
    ProgressPointDto(
        id = id.value,
        exerciseCatalogId = exerciseCatalogId.value,
        sourceWorkoutId = sourceWorkoutId.value,
        sourceSetId = sourceSetId?.value,
        metric = metric.name,
        value = value,
        weightKg = weight?.value,
        reps = reps,
        recordedAt = recordedAt.toBackupMillis()
    )

internal fun ProgressPointDto.toDomain(): ProgressPoint =
    ProgressPoint(
        id = FoundationId(id),
        exerciseCatalogId = FoundationId(exerciseCatalogId),
        sourceWorkoutId = FoundationId(sourceWorkoutId),
        sourceSetId = sourceSetId?.let(::FoundationId),
        metric = ProgressMetric.valueOf(metric),
        value = value,
        weight = weightKg?.let(::WeightKg),
        reps = reps,
        recordedAt = recordedAt.toBackupInstant()
    )

internal fun ExportSnapshot.toDto(): ExportSnapshotDto =
    ExportSnapshotDto(
        id = id.value,
        exportType = exportType.name,
        createdAt = createdAt.toBackupMillis(),
        weightUnit = weightUnit.name,
        rowCount = rowCount,
        formatVersion = formatVersion
    )

internal fun ExportSnapshotDto.toDomain(): ExportSnapshot =
    ExportSnapshot(
        id = FoundationId(id),
        exportType = ExportType.valueOf(exportType),
        createdAt = createdAt.toBackupInstant(),
        weightUnit = WeightUnit.valueOf(weightUnit),
        rowCount = rowCount,
        formatVersion = formatVersion
    )
