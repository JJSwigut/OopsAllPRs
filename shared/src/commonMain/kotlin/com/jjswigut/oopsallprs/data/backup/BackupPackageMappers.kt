package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.data.LoggingConfigurationIdentity
import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveExerciseGroupContext
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.EffortTarget
import com.jjswigut.oopsallprs.domain.model.EffortTargetKind
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionOrigin
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedKey
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.ExportSnapshot
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.WireCode
import com.jjswigut.oopsallprs.domain.model.WorkoutStatus
import kotlinx.datetime.Instant

internal fun Instant.toBackupMillis(): Long = toEpochMilliseconds()
internal fun Long.toBackupInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun SnapshotSummary.toDto() = SnapshotSummaryDto(
    workoutCount, setCount, routineCount, customExerciseCount, progressRecordCount,
    hasActiveWorkout, latestWorkoutTimestamp?.toBackupMillis(), latestUpdatedTimestamp?.toBackupMillis()
)

internal fun SnapshotSummaryDto.toDomain() = SnapshotSummary(
    workoutCount, setCount, routineCount, customExerciseCount, progressRecordCount,
    hasActiveWorkout, latestWorkoutTimestamp?.toBackupInstant(), latestUpdatedTimestamp?.toBackupInstant()
)

internal fun RestConfiguration.toDto() = RestConfigurationDto(durationSeconds, autoStart)
internal fun RestConfigurationDto.toDomain() = RestConfiguration(durationSeconds, autoStart)

internal fun LoggingConfiguration.toDto(): LoggingConfigurationDto = LoggingConfigurationDto(
    id = id.value,
    schemaVersion = schemaVersion.value,
    contentHash = LoggingConfigurationIdentity.contentHash(this),
    measures = measures.map { measure ->
        LoggingMeasureDto(
            kind = measure.kind.wireCode.value,
            requirement = measure.requirement.wireCode.value,
            canonicalUnit = measure.kind.canonicalUnit.wireCode.value,
            loadRole = measure.loadRole?.wireCode?.value
        )
    },
    observedEffortKinds = observedEffort?.kinds.orEmpty().map { it.wireCode.value }
)

internal fun LoggingConfigurationDto.toDomain(): LoggingConfiguration {
    val configuration = LoggingConfiguration(
        id = LoggingConfigurationId(id),
        schemaVersion = LoggingSchemaVersion(schemaVersion),
        measures = measures.map { measure ->
            val kind = requireNotNull(MeasureKind.fromWireCode(measure.kind)) {
                "Unknown measure code: ${measure.kind}"
            }
            require(kind.canonicalUnit.wireCode.value == measure.canonicalUnit) {
                "Measure ${measure.kind} has an incompatible canonical unit"
            }
            MeasureSpec(
                kind = kind,
                requirement = requireNotNull(MeasureRequirement.fromWireCode(measure.requirement)) {
                    "Unknown measure requirement: ${measure.requirement}"
                },
                loadRole = measure.loadRole?.let {
                    requireNotNull(LoadRole.fromWireCode(it)) { "Unknown load role: $it" }
                }
            )
        },
        observedEffort = observedEffortKinds.takeIf { it.isNotEmpty() }?.map { code ->
            requireNotNull(EffortKind.fromWireCode(code)) { "Unknown effort kind: $code" }
        }?.let(::ObservedEffortSpec)
    )
    require(contentHash == LoggingConfigurationIdentity.contentHash(configuration)) {
        "Logging configuration $id content hash does not match its semantic content"
    }
    return configuration
}

internal fun UserExerciseConfiguration.toDto() = UserExerciseConfigurationDto(
    exerciseDefinitionId.value, configuration.id.value, basedOnDefinitionRevision.value, configuredAt.toBackupMillis()
)

internal fun UserExerciseConfigurationDto.toDomain(configurations: Map<String, LoggingConfiguration>) =
    UserExerciseConfiguration(
        FoundationId(exerciseDefinitionId),
        requireNotNull(configurations[loggingConfigurationId]) { "Unknown logging configuration: $loggingConfigurationId" },
        ExerciseDefinitionRevision(basedOnDefinitionRevision),
        configuredAt.toBackupInstant()
    )

internal fun ExerciseCatalogItem.toDto() = ExerciseCatalogItemDto(
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
    userNotes = userNotes,
    origin = origin.wireCode.value,
    definitionRevision = definitionRevision.value,
    seedKey = seedKey?.value,
    seedManifestRevision = sourceSeedVersion,
    defaultLoggingConfigurationId = defaultLoggingConfiguration.id.value
)

internal fun ExerciseCatalogItemDto.toDomain(configurations: Map<String, LoggingConfiguration>) = ExerciseCatalogItem(
    id = FoundationId(id), canonicalName = canonicalName, displayName = displayName, muscleGroup = muscleGroup,
    equipment = equipment, movementPattern = movementPattern, exerciseType = exerciseType,
    experienceLevel = experienceLevel, bodyRegion = bodyRegion, isBodyweight = isBodyweight,
    loggingMode = ExerciseLoggingMode.valueOf(loggingMode), isUserCreated = isUserCreated,
    createdAt = createdAt.toBackupInstant(), updatedAt = updatedAt.toBackupInstant(),
    archivedAt = archivedAt?.toBackupInstant(), sourceSeedVersion = sourceSeedVersion, userNotes = userNotes,
    origin = requireNotNull(ExerciseDefinitionOrigin.fromWireCode(origin)) { "Unknown exercise origin: $origin" },
    definitionRevision = ExerciseDefinitionRevision(definitionRevision),
    seedKey = seedKey?.let(::ExerciseSeedKey),
    defaultLoggingConfiguration = requireNotNull(configurations[defaultLoggingConfigurationId]) {
        "Unknown logging configuration: $defaultLoggingConfigurationId"
    }
)

private fun Effort?.toFields(): Triple<Int?, Int?, String?> =
    Triple(this?.rpeTenths, this?.rir, this?.failureOutcome?.wireCode?.value)

private fun effort(rpeTenths: Int?, rir: Int?, failureOutcome: String?): Effort? {
    if (rpeTenths == null && rir == null && failureOutcome == null) return null
    return Effort(
        rpeTenths,
        rir,
        failureOutcome?.let { requireNotNull(FailureOutcome.fromWireCode(it)) { "Unknown failure outcome: $it" } }
    )
}

internal fun ExerciseSet.toDto(): ExerciseSetDto {
    val effort = observedEffort.toFields()
    return ExerciseSetDto(
        id.value, exerciseInstanceId.value, position.value, setKind.name, weight?.value, reps, durationMs,
        loggedAt?.toBackupMillis(), createdAt.toBackupMillis(), updatedAt.toBackupMillis(), editedAt?.toBackupMillis(),
        captureConfigurationId.value, distanceMeters, effort.first, effort.second, effort.third
    )
}

internal fun ExerciseSetDto.toDomain() = ExerciseSet(
    FoundationId(id), FoundationId(exerciseInstanceId), OrderedPosition(position), SetKind.valueOf(setKind),
    weightKg?.let(::WeightKg), reps, loggedAt?.toBackupInstant(), createdAt.toBackupInstant(),
    updatedAt.toBackupInstant(), editedAt?.toBackupInstant(), durationMs,
    LoggingConfigurationId(captureConfigurationId), distanceMeters, effort(rpeTenths, rir, failureOutcome)
)

internal fun ActiveExercise.toDto() = ActiveExerciseDto(
    id = id.value, activeWorkoutId = activeWorkoutId.value,
    exerciseCatalogId = reference.exerciseCatalogId.value,
    displayNameSnapshot = reference.displayNameSnapshot, equipmentSnapshot = reference.equipmentSnapshot,
    isBodyweight = reference.isBodyweight, loggingMode = reference.loggingMode.name, position = position.value,
    groupId = groupContext?.groupId?.value, groupPosition = groupContext?.groupPosition?.value,
    groupLabel = groupContext?.label, groupRounds = groupContext?.rounds, sets = sets.map { it.toDto() },
    rest = rest.toDto(), definitionOriginSnapshot = reference.definitionOriginSnapshot?.wireCode?.value,
    definitionRevisionSnapshot = reference.definitionRevisionSnapshot.value,
    seedKeySnapshot = reference.seedKeySnapshot?.value,
    loggingConfigurationId = resolvedLoggingConfiguration.configuration.id.value,
    loggingConfigurationSource = resolvedLoggingConfiguration.source.wireCode.value
)

internal fun ActiveExerciseDto.toDomain(configurations: Map<String, LoggingConfiguration>) = ActiveExercise(
    id = FoundationId(id), activeWorkoutId = FoundationId(activeWorkoutId),
    reference = ExerciseReference(
        exerciseCatalogId = FoundationId(exerciseCatalogId), displayNameSnapshot = displayNameSnapshot,
        isBodyweight = isBodyweight, loggingMode = ExerciseLoggingMode.valueOf(loggingMode),
        equipmentSnapshot = equipmentSnapshot,
        definitionOriginSnapshot = definitionOriginSnapshot?.let {
            requireNotNull(ExerciseDefinitionOrigin.fromWireCode(it)) { "Unknown exercise origin: $it" }
        },
        definitionRevisionSnapshot = ExerciseDefinitionRevision(definitionRevisionSnapshot),
        seedKeySnapshot = seedKeySnapshot?.let(::ExerciseSeedKey),
        resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
            requireNotNull(configurations[loggingConfigurationId]) { "Unknown logging configuration: $loggingConfigurationId" },
            requireNotNull(LoggingConfigurationSource.fromWireCode(loggingConfigurationSource)) {
                "Unknown logging configuration source: $loggingConfigurationSource"
            }
        )
    ),
    position = OrderedPosition(position),
    groupContext = if (groupId != null && groupPosition != null && groupLabel != null && groupRounds != null) {
        ActiveExerciseGroupContext(FoundationId(groupId), OrderedPosition(groupPosition), groupLabel, groupRounds)
    } else null,
    sets = sets.map { it.toDomain() }, rest = rest.toDomain()
)

internal fun ActiveWorkout.toDto() = ActiveWorkoutDto(
    id.value, startedAt.toBackupMillis(), routineId?.value, routineSnapshotName, exercises.map { it.toDto() },
    createdAt.toBackupMillis(), updatedAt.toBackupMillis(), status.name
)

internal fun ActiveWorkoutDto.toDomain(configurations: Map<String, LoggingConfiguration>) = ActiveWorkout(
    FoundationId(id), startedAt.toBackupInstant(), routineId?.let(::FoundationId), routineSnapshotName,
    exercises.map { it.toDomain(configurations) }, createdAt.toBackupInstant(), updatedAt.toBackupInstant(),
    WorkoutStatus.valueOf(status)
)

internal fun ActiveSessionState.toDto() = ActiveSessionDto(
    activeWorkoutId?.value, startedAt?.toBackupMillis(), restEndsAt?.toBackupMillis(), restStartedAt?.toBackupMillis(),
    restOriginSetId?.value, lastOpenedRoute, updatedAt.toBackupMillis()
)
internal fun ActiveSessionDto.toDomain() = ActiveSessionState(
    activeWorkoutId?.let(::FoundationId), startedAt?.toBackupInstant(), restEndsAt?.toBackupInstant(),
    restStartedAt?.toBackupInstant(), restOriginSetId?.let(::FoundationId), lastOpenedRoute, updatedAt.toBackupInstant()
)
internal fun ActiveWorkoutUxSession.toDto() = ActiveWorkoutUxSessionDto(
    activeWorkoutId.value, focusedExerciseInstanceId?.value, focusedDraftId?.value, updatedAt.toBackupMillis()
)
internal fun ActiveWorkoutUxSessionDto.toDomain() = ActiveWorkoutUxSession(
    FoundationId(activeWorkoutId), focusedExerciseInstanceId?.let(::FoundationId), focusedDraftId?.let(::FoundationId),
    updatedAt.toBackupInstant()
)

internal fun PersistedSetDraft.toDto(): PersistedSetDraftDto {
    val effort = observedEffort.toFields()
    return PersistedSetDraftDto(
        draftId.value, activeWorkoutId.value, exerciseInstanceId.value, position.value, setKind.name, reps,
        weight?.value, durationMs, timerStartedAt?.toBackupMillis(), updatedAt.toBackupMillis(),
        captureConfigurationId.value, distanceMeters, effort.first, effort.second, effort.third
    )
}
internal fun PersistedSetDraftDto.toDomain() = PersistedSetDraft(
    FoundationId(draftId), FoundationId(activeWorkoutId), FoundationId(exerciseInstanceId), OrderedPosition(position),
    SetKind.valueOf(setKind), reps, weightKg?.let(::WeightKg), durationMs, timerStartedAt?.toBackupInstant(),
    updatedAt.toBackupInstant(), LoggingConfigurationId(captureConfigurationId), distanceMeters,
    effort(rpeTenths, rir, failureOutcome)
)

private fun EffortTarget?.toFields(): Triple<String?, Int?, Int?> = when (this) {
    null -> Triple(null, null, null)
    is EffortTarget.Rpe -> Triple(kind.wireCode.value, rpeTenths, null)
    is EffortTarget.Rir -> Triple(kind.wireCode.value, null, rir)
    EffortTarget.ToFailure -> Triple(kind.wireCode.value, null, null)
}

private fun effortTarget(kind: String?, rpeTenths: Int?, rir: Int?): EffortTarget? {
    if (kind == null) {
        require(rpeTenths == null && rir == null) { "Effort target values require a target kind" }
        return null
    }
    return when (requireNotNull(EffortTargetKind.fromWireCode(kind)) { "Unknown effort target: $kind" }) {
        EffortTargetKind.RPE -> EffortTarget.Rpe(requireNotNull(rpeTenths))
        EffortTargetKind.RIR -> EffortTarget.Rir(requireNotNull(rir))
        EffortTargetKind.TO_FAILURE -> EffortTarget.ToFailure.also {
            require(rpeTenths == null && rir == null) { "To-failure targets cannot carry RPE or RIR" }
        }
    }
}

internal fun RoutineSetTemplate.toDto(configurationId: LoggingConfigurationId): RoutineSetTemplateDto {
    val effort = effortTarget.toFields()
    return RoutineSetTemplateDto(
        id.value, routineExerciseId.value, position.value, targetWeight?.value, targetReps, targetDurationMs,
        setKind.name, configurationId.value, targetDistanceMeters, effort.first, effort.second, effort.third
    )
}
internal fun RoutineSetTemplateDto.toDomain() = RoutineSetTemplate(
    FoundationId(id), FoundationId(routineExerciseId), OrderedPosition(position), targetWeightKg?.let(::WeightKg),
    targetReps, targetDurationMs, SetKind.valueOf(setKind), targetDistanceMeters,
    effortTarget(effortTargetKind, targetRpeTenths, targetRir)
)

internal fun RoutineExercise.toDto() = RoutineExerciseDto(
    id.value, routineId.value, exerciseCatalogId.value, displayNameSnapshot, position.value,
    groupId?.value, groupPosition?.value, groupRounds,
    plannedSets.map { it.toDto(resolvedLoggingConfiguration.configuration.id) }, rest.toDto(),
    definitionRevisionSnapshot.value, seedKeySnapshot?.value,
    resolvedLoggingConfiguration.configuration.id.value, resolvedLoggingConfiguration.source.wireCode.value
)
internal fun RoutineExerciseDto.toDomain(configurations: Map<String, LoggingConfiguration>) = RoutineExercise(
    FoundationId(id), FoundationId(routineId), FoundationId(exerciseCatalogId), displayNameSnapshot,
    OrderedPosition(position), groupId?.let(::FoundationId), groupPosition?.let(::OrderedPosition), groupRounds,
    plannedSets.map { it.toDomain() }, rest.toDomain(), ExerciseDefinitionRevision(definitionRevisionSnapshot),
    seedKeySnapshot?.let(::ExerciseSeedKey), ResolvedLoggingConfiguration(
        requireNotNull(configurations[loggingConfigurationId]) { "Unknown logging configuration: $loggingConfigurationId" },
        requireNotNull(LoggingConfigurationSource.fromWireCode(loggingConfigurationSource)) {
            "Unknown logging configuration source: $loggingConfigurationSource"
        }
    )
)
internal fun ReusableRoutine.toDto() = RoutineDto(
    id.value, name, exercises.map { it.toDto() }, createdAt.toBackupMillis(), updatedAt.toBackupMillis(),
    sourceCompletedWorkoutId?.value, archivedAt?.toBackupMillis()
)
internal fun RoutineDto.toDomain(configurations: Map<String, LoggingConfiguration>) = ReusableRoutine(
    FoundationId(id), name, exercises.map { it.toDomain(configurations) }, createdAt.toBackupInstant(),
    updatedAt.toBackupInstant(), sourceCompletedWorkoutId?.let(::FoundationId), archivedAt?.toBackupInstant()
)

internal fun CompletedExercise.toDto(): CompletedExerciseDto {
    val ledgerExerciseId = loggedSets.firstOrNull()?.exerciseInstanceId?.value ?: id.value
    require(loggedSets.all { it.exerciseInstanceId.value == ledgerExerciseId }) {
        "Completed exercise contains sets from multiple exercise instances"
    }
    return CompletedExerciseDto(
        ledgerExerciseId, completedWorkoutId.value, exerciseCatalogId.value, displayNameSnapshot, position.value,
        loggedSets.map { it.toDto() }, rest.toDto()
    )
}
internal fun CompletedExerciseDto.toDomain() = CompletedExercise(
    FoundationId(id), FoundationId(completedWorkoutId), FoundationId(exerciseCatalogId), displayNameSnapshot,
    OrderedPosition(position), loggedSets.map { it.toDomain() }, rest.toDomain()
)
internal fun CompletedWorkout.toDto() = CompletedWorkoutDto(
    id.value, sourceActiveWorkoutId.value, startedAt.toBackupMillis(), finishedAt.toBackupMillis(), durationMs,
    routineId?.value, exercises.map { it.toDto() }, createdAt.toBackupMillis()
)
internal fun CompletedWorkoutDto.toDomain() = CompletedWorkout(
    FoundationId(id), FoundationId(sourceActiveWorkoutId), startedAt.toBackupInstant(), finishedAt.toBackupInstant(),
    durationMs, routineId?.let(::FoundationId), exercises.map { it.toDomain() }, createdAt.toBackupInstant()
)

internal fun PersonalRecord.toDto() = PersonalRecordDto(
    id.value, exerciseCatalogId.value, recordKind.name, reps, weight?.value, value, sourceWorkoutId.value,
    sourceSetId.value, achievedAt.toBackupMillis(), createdAt.toBackupMillis(), metricCode.value, derivationVersion
)
internal fun PersonalRecordDto.toDomain() = PersonalRecord(
    FoundationId(id), FoundationId(exerciseCatalogId), PersonalRecordKind.valueOf(recordKind), reps,
    weightKg?.let(::WeightKg), value, FoundationId(sourceWorkoutId), FoundationId(sourceSetId),
    achievedAt.toBackupInstant(), createdAt.toBackupInstant(), validatedMetricCode(metricCode, recordKind),
    derivationVersion
)

internal fun ProgressPoint.toDto() = ProgressPointDto(
    id.value, exerciseCatalogId.value, sourceWorkoutId.value, sourceSetId?.value, metric.name, value,
    weight?.value, reps, recordedAt.toBackupMillis(), metricCode.value, derivationVersion
)
internal fun ProgressPointDto.toDomain() = ProgressPoint(
    FoundationId(id), FoundationId(exerciseCatalogId), FoundationId(sourceWorkoutId), sourceSetId?.let(::FoundationId),
    ProgressMetric.valueOf(metric), value, weightKg?.let(::WeightKg), reps, recordedAt.toBackupInstant(),
    validatedMetricCode(metricCode, metric), derivationVersion
)

private fun validatedMetricCode(code: String, legacyProjection: String): WireCode {
    val metric = requireNotNull(ProgressEvidenceMetric.fromWireCode(code)) { "Unknown progress metric code: $code" }
    require(
        metric.legacyRecordKind.name == legacyProjection || metric.legacyProgressMetric.name == legacyProjection
    ) { "Progress metric code $code is incompatible with legacy projection $legacyProjection" }
    return metric.wireCode
}

internal fun ExportSnapshot.toDto() = ExportSnapshotDto(
    id.value, exportType.name, createdAt.toBackupMillis(), weightUnit.name, rowCount, formatVersion
)
internal fun ExportSnapshotDto.toDomain() = ExportSnapshot(
    FoundationId(id), ExportType.valueOf(exportType), createdAt.toBackupInstant(), WeightUnit.valueOf(weightUnit),
    rowCount, formatVersion
)

internal fun BackupPackageV1Dto.normalizeToV2(): BackupPackage {
    require(formatVersion == BACKUP_FORMAT_VERSION_V1) { "V1 backup has an invalid format version" }
    val configurations = LegacyLoggingConfigurations.all.associateBy { it.id.value }
    return BackupPackage(
        formatVersion = BACKUP_FORMAT_VERSION_V1,
        createdAt = createdAt,
        deviceId = deviceId,
        lastLocalRevision = lastLocalRevision,
        appSchemaVersion = appSchemaVersion,
        summary = SnapshotSummaryDto(summary.workoutCount, summary.setCount, summary.routineCount, summary.customExerciseCount, summary.progressRecordCount, summary.hasActiveWorkout, summary.latestWorkoutTimestamp, summary.latestUpdatedTimestamp),
        preferences = PreferencesSnapshotDto(
            preferences.weightUnit,
            preferences.weightStepPounds,
            preferences.weightStepKilograms,
            preferences.defaultRestSeconds,
            preferences.restSoundEnabled,
            startWorkoutTimerWithFirstSet = true,
            restTimerSurfaceEnabled = true
        ),
        loggingConfigurations = configurations.values.map { it.toDto() },
        userExerciseConfigurations = emptyList(),
        exercises = exercises.map { item ->
            val configuration = legacyConfigurationForMode(item.loggingMode)
            ExerciseCatalogItemDto(
                item.id, item.canonicalName, item.displayName, item.muscleGroup, item.equipment, item.movementPattern,
                item.exerciseType, item.experienceLevel, item.bodyRegion, item.isBodyweight, item.loggingMode,
                item.isUserCreated, item.createdAt, item.updatedAt, item.archivedAt, item.sourceSeedVersion, item.userNotes,
                if (item.isUserCreated) "user" else "seed", 1,
                if (item.isUserCreated) null else "oopsallprs_baseline:${item.canonicalName}", item.sourceSeedVersion,
                configuration.id.value
            )
        },
        routines = routines.map { it.normalize(configurations) },
        activeWorkout = activeWorkout?.normalize(configurations),
        activeSession = activeSession?.let { ActiveSessionDto(it.activeWorkoutId, it.startedAt, it.restEndsAt, it.restStartedAt, it.restOriginSetId, it.lastOpenedRoute, it.updatedAt) },
        activeUxSession = activeUxSession?.let { ActiveWorkoutUxSessionDto(it.activeWorkoutId, it.focusedExerciseInstanceId, it.focusedDraftId, it.updatedAt) },
        activeSetDrafts = activeSetDrafts.map { draft ->
            val configuration = legacyConfigurationForSet(draft.setKind, draft.weightKg)
            PersistedSetDraftDto(draft.draftId, draft.activeWorkoutId, draft.exerciseInstanceId, draft.position, draft.setKind, draft.reps, draft.weightKg, draft.durationMs, draft.timerStartedAt, draft.updatedAt, configuration.id.value, null, null, null, null)
        },
        completedWorkouts = completedWorkouts.map { it.normalize(configurations) },
        personalRecords = personalRecords.map { record ->
            val domain = PersonalRecord(
                FoundationId(record.id), FoundationId(record.exerciseCatalogId), PersonalRecordKind.valueOf(record.recordKind),
                record.reps, record.weightKg?.let(::WeightKg), record.value, FoundationId(record.sourceWorkoutId),
                FoundationId(record.sourceSetId), record.achievedAt.toBackupInstant(), record.createdAt.toBackupInstant()
            )
            domain.toDto()
        },
        progressPoints = progressPoints.map { point ->
            val domain = ProgressPoint(
                FoundationId(point.id), FoundationId(point.exerciseCatalogId), FoundationId(point.sourceWorkoutId),
                point.sourceSetId?.let(::FoundationId), ProgressMetric.valueOf(point.metric), point.value,
                point.weightKg?.let(::WeightKg), point.reps, point.recordedAt.toBackupInstant()
            )
            domain.toDto()
        },
        exportMetadata = exportMetadata.map { ExportSnapshotDto(it.id, it.exportType, it.createdAt, it.weightUnit, it.rowCount, it.formatVersion) }
    )
}

private fun legacyConfigurationForMode(mode: String): LoggingConfiguration =
    LegacyLoggingConfigurations.from(ExerciseLoggingMode.valueOf(mode))

private fun legacyConfigurationForSet(kind: String, load: Double? = null): LoggingConfiguration =
    LegacyLoggingConfigurations.from(SetKind.valueOf(kind), kind == "BODYWEIGHT" && load != null)

private fun ExerciseSetV1Dto.normalize(): ExerciseSetDto {
    val configuration = legacyConfigurationForSet(setKind, weightKg)
    return ExerciseSetDto(id, exerciseInstanceId, position, setKind, weightKg, reps, durationMs, loggedAt, createdAt, updatedAt, editedAt, configuration.id.value, null, null, null, null)
}

private fun ActiveWorkoutV1Dto.normalize(configurations: Map<String, LoggingConfiguration>) = ActiveWorkoutDto(
    id, startedAt, routineId, routineSnapshotName,
    exercises.map { exercise ->
        val configuration = legacyConfigurationForMode(exercise.loggingMode)
        ActiveExerciseDto(
            exercise.id, exercise.activeWorkoutId, exercise.exerciseCatalogId, exercise.displayNameSnapshot,
            exercise.equipmentSnapshot, exercise.isBodyweight, exercise.loggingMode, exercise.position, exercise.groupId,
            exercise.groupPosition, exercise.groupLabel, exercise.groupRounds, exercise.sets.map { it.normalize() },
            RestConfigurationDto(exercise.rest.durationSeconds, exercise.rest.autoStart), null, 1, null,
            configurations.getValue(configuration.id.value).id.value, "definition_default"
        )
    }, createdAt, updatedAt, status
)

private fun RoutineV1Dto.normalize(configurations: Map<String, LoggingConfiguration>) = RoutineDto(
    id, name, exercises.map { exercise ->
        val first = exercise.plannedSets.firstOrNull()
        val configuration = first?.let { legacyConfigurationForSet(it.setKind, it.targetWeightKg) }
            ?: LegacyLoggingConfigurations.weighted
        RoutineExerciseDto(
            exercise.id, exercise.routineId, exercise.exerciseCatalogId, exercise.displayNameSnapshot,
            exercise.position, exercise.groupId, exercise.groupPosition, exercise.groupRounds,
            exercise.plannedSets.map { set ->
                val setConfiguration = legacyConfigurationForSet(set.setKind, set.targetWeightKg)
                RoutineSetTemplateDto(set.id, set.routineExerciseId, set.position, set.targetWeightKg, set.targetReps, set.targetDurationMs, set.setKind, setConfiguration.id.value, null, null, null, null)
            }, RestConfigurationDto(exercise.rest.durationSeconds, exercise.rest.autoStart), 1, null,
            configurations.getValue(configuration.id.value).id.value, "definition_default"
        )
    }, createdAt, updatedAt, sourceCompletedWorkoutId, archivedAt
)

private fun CompletedWorkoutV1Dto.normalize(configurations: Map<String, LoggingConfiguration>) = CompletedWorkoutDto(
    id, sourceActiveWorkoutId, startedAt, finishedAt, durationMs, routineId,
    exercises.map { exercise ->
        val ledgerExerciseId = exercise.loggedSets.firstOrNull()?.exerciseInstanceId ?: exercise.id
        CompletedExerciseDto(
            ledgerExerciseId, exercise.completedWorkoutId, exercise.exerciseCatalogId, exercise.displayNameSnapshot,
            exercise.position, exercise.loggedSets.map { it.normalize() },
            RestConfigurationDto(exercise.rest.durationSeconds, exercise.rest.autoStart)
        )
    }, createdAt
)
