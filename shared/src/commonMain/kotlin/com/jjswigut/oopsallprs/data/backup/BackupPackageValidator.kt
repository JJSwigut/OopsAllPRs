package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.WeightUnit

internal object BackupPackageValidator {
    fun validate(pkg: BackupPackage) {
        require(pkg.formatVersion in BACKUP_FORMAT_VERSION_V1..BACKUP_FORMAT_VERSION) {
            "Backup format version is invalid"
        }
        require(pkg.deviceId.isNotBlank()) { "Backup is missing device metadata" }
        require(pkg.lastLocalRevision.isNotBlank()) { "Backup is missing revision metadata" }
        WeightUnit.valueOf(pkg.preferences.weightUnit)
        require(pkg.preferences.weightStepPounds > 0.0 && pkg.preferences.weightStepPounds.isFinite())
        require(pkg.preferences.weightStepKilograms > 0.0 && pkg.preferences.weightStepKilograms.isFinite())
        require(pkg.preferences.defaultRestSeconds > 0)

        requireUnique(pkg.loggingConfigurations.map { it.id }, "logging configuration IDs")
        requireUnique(pkg.loggingConfigurations.map { it.contentHash }, "logging configuration hashes")
        val configurations = pkg.loggingConfigurations.associate { dto -> dto.id to dto.toDomain() }

        requireUnique(pkg.exercises.map { it.id }, "exercise IDs")
        requireUnique(pkg.exercises.map { it.canonicalName }, "exercise canonical names")
        requireUnique(pkg.exercises.mapNotNull { it.seedKey }, "exercise seed keys")
        val exercises = pkg.exercises.associateBy { it.id }
        pkg.exercises.forEach { exercise ->
            exercise.toDomain(configurations)
            require(exercise.seedManifestRevision == exercise.sourceSeedVersion) {
                "Exercise ${exercise.id} seed revisions disagree"
            }
        }

        requireUnique(pkg.userExerciseConfigurations.map { it.exerciseDefinitionId }, "user exercise configurations")
        pkg.userExerciseConfigurations.forEach { userConfiguration ->
            userConfiguration.toDomain(configurations)
            val exercise = requireNotNull(exercises[userConfiguration.exerciseDefinitionId]) {
                "User configuration references an unknown exercise: ${userConfiguration.exerciseDefinitionId}"
            }
            require(userConfiguration.basedOnDefinitionRevision == exercise.definitionRevision) {
                "User configuration revision does not match exercise ${exercise.id}"
            }
        }

        requireUnique(pkg.routines.map { it.id }, "routine IDs")
        val routineExerciseIds = mutableListOf<String>()
        val routineSetIds = mutableListOf<String>()
        pkg.routines.forEach { routine ->
            routine.toDomain(configurations)
            routine.exercises.forEach { exercise ->
                routineExerciseIds += exercise.id
                require(exercise.routineId == routine.id) { "Routine exercise ${exercise.id} has the wrong parent" }
                require(exercise.exerciseCatalogId in exercises) {
                    "Routine exercise ${exercise.id} references an unknown exercise"
                }
                validateResolvedConfiguration(
                    exercise.loggingConfigurationId,
                    exercise.loggingConfigurationSource,
                    configurations
                )
                exercise.plannedSets.forEach { set ->
                    routineSetIds += set.id
                    require(set.routineExerciseId == exercise.id) { "Routine set ${set.id} has the wrong parent" }
                    require(set.loggingConfigurationId == exercise.loggingConfigurationId) {
                        "Routine set ${set.id} configuration differs from its exercise snapshot"
                    }
                    require(set.loggingConfigurationId in configurations) {
                        "Routine set ${set.id} references an unknown logging configuration"
                    }
                    set.toDomain()
                }
            }
        }
        requireUnique(routineExerciseIds, "routine exercise IDs")
        requireUnique(routineSetIds, "routine set IDs")

        val allSetIds = mutableListOf<String>()
        val completedWorkoutIds = mutableSetOf<String>()
        val completedSourceWorkoutIds = mutableSetOf<String>()
        val completedSetIds = mutableSetOf<String>()
        pkg.completedWorkouts.forEach { workout ->
            require(completedWorkoutIds.add(workout.id)) { "Duplicate completed workout ID: ${workout.id}" }
            require(completedSourceWorkoutIds.add(workout.sourceActiveWorkoutId)) {
                "Duplicate completed source workout ID: ${workout.sourceActiveWorkoutId}"
            }
            workout.toDomain()
            workout.exercises.forEach { exercise ->
                require(exercise.loggedSets.isNotEmpty()) {
                    "Completed exercise ${exercise.id} has no logged sets"
                }
                require(exercise.completedWorkoutId == workout.id) {
                    "Completed exercise ${exercise.id} has the wrong parent"
                }
                require(exercise.exerciseCatalogId in exercises) {
                    "Completed exercise ${exercise.id} references an unknown exercise"
                }
                exercise.loggedSets.forEach { set ->
                    require(set.exerciseInstanceId == exercise.id) { "Completed set ${set.id} has the wrong exercise" }
                    validateSet(set, configurations, requireLogged = true)
                    allSetIds += set.id
                    completedSetIds += set.id
                }
            }
        }

        val active = pkg.activeWorkout
        val activeExerciseIds = mutableSetOf<String>()
        val activeSetIds = mutableSetOf<String>()
        if (active != null) {
            require(active.id !in completedSourceWorkoutIds) { "Active workout ID collides with completed history" }
            active.toDomain(configurations)
            active.exercises.forEach { exercise ->
                require(activeExerciseIds.add(exercise.id)) { "Duplicate active exercise ID: ${exercise.id}" }
                require(exercise.activeWorkoutId == active.id) { "Active exercise ${exercise.id} has the wrong parent" }
                require(exercise.exerciseCatalogId in exercises) {
                    "Active exercise ${exercise.id} references an unknown exercise"
                }
                validateResolvedConfiguration(
                    exercise.loggingConfigurationId,
                    exercise.loggingConfigurationSource,
                    configurations
                )
                exercise.sets.forEach { set ->
                    require(set.exerciseInstanceId == exercise.id) { "Active set ${set.id} has the wrong exercise" }
                    validateSet(set, configurations, requireLogged = false)
                    allSetIds += set.id
                    activeSetIds += set.id
                }
            }
        }
        requireUnique(allSetIds, "set IDs")

        pkg.activeSession?.let { session ->
            session.toDomain()
            require(session.activeWorkoutId == null || session.activeWorkoutId == active?.id) {
                "Active session references an unknown workout"
            }
            require(session.restOriginSetId == null || session.restOriginSetId in activeSetIds) {
                "Active session references an unknown rest-origin set"
            }
        }
        pkg.activeUxSession?.let { ux ->
            ux.toDomain()
            require(ux.activeWorkoutId == active?.id) { "Active UX state references an unknown workout" }
            require(ux.focusedExerciseInstanceId == null || ux.focusedExerciseInstanceId in activeExerciseIds) {
                "Active UX state references an unknown exercise"
            }
        }
        requireUnique(pkg.activeSetDrafts.map { it.draftId }, "draft IDs")
        pkg.activeSetDrafts.forEach { draft ->
            draft.toDomain()
            require(draft.activeWorkoutId == active?.id) { "Draft ${draft.draftId} references an unknown workout" }
            require(draft.exerciseInstanceId in activeExerciseIds) { "Draft ${draft.draftId} references an unknown exercise" }
            require(draft.captureConfigurationId in configurations) {
                "Draft ${draft.draftId} references an unknown logging configuration"
            }
        }
        pkg.activeUxSession?.focusedDraftId?.let { focusedDraftId ->
            require(pkg.activeSetDrafts.any { it.draftId == focusedDraftId }) {
                "Active UX state references an unknown draft"
            }
        }

        requireUnique(pkg.personalRecords.map { it.id }, "personal record IDs")
        pkg.personalRecords.forEach { record ->
            record.toDomain()
            require(record.exerciseCatalogId in exercises) { "Personal record ${record.id} references an unknown exercise" }
            require(record.sourceWorkoutId in completedWorkoutIds) { "Personal record ${record.id} references an unknown workout" }
            require(record.sourceSetId in completedSetIds) { "Personal record ${record.id} references an unknown set" }
        }
        requireUnique(pkg.progressPoints.map { it.id }, "progress point IDs")
        pkg.progressPoints.forEach { point ->
            point.toDomain()
            require(point.exerciseCatalogId in exercises) { "Progress point ${point.id} references an unknown exercise" }
            require(point.sourceWorkoutId in completedWorkoutIds) { "Progress point ${point.id} references an unknown workout" }
            require(point.sourceSetId == null || point.sourceSetId in completedSetIds) {
                "Progress point ${point.id} references an unknown set"
            }
        }
        pkg.exportMetadata.forEach { it.toDomain() }

        val actualSetCount = allSetIds.size
        require(pkg.summary.workoutCount == pkg.completedWorkouts.size) { "Backup summary workout count is invalid" }
        require(pkg.summary.setCount == actualSetCount) { "Backup summary set count is invalid" }
        require(pkg.summary.routineCount == pkg.routines.count { it.archivedAt == null }) {
            "Backup summary routine count is invalid"
        }
        require(pkg.summary.customExerciseCount == pkg.exercises.count { it.isUserCreated && it.archivedAt == null }) {
            "Backup summary custom exercise count is invalid"
        }
        require(pkg.summary.progressRecordCount == pkg.personalRecords.size + pkg.progressPoints.size) {
            "Backup summary progress count is invalid"
        }
        require(pkg.summary.hasActiveWorkout == (active != null)) { "Backup summary active-workout flag is invalid" }
    }

    private fun validateSet(
        set: ExerciseSetDto,
        configurations: Map<String, LoggingConfiguration>,
        requireLogged: Boolean
    ) {
        val domain = set.toDomain()
        val configuration = requireNotNull(configurations[set.captureConfigurationId]) {
            "Set ${set.id} references an unknown logging configuration"
        }
        if (requireLogged) require(set.loggedAt != null) { "Completed set ${set.id} is not logged" }
        if (set.loggedAt != null) {
            domain.validateForLogging(configuration)?.let { error -> throw IllegalArgumentException(error.message) }
        }
    }

    private fun validateResolvedConfiguration(
        configurationId: String,
        sourceCode: String,
        configurations: Map<String, LoggingConfiguration>
    ) {
        requireNotNull(LoggingConfigurationSource.fromWireCode(sourceCode)) {
            "Unknown logging configuration source: $sourceCode"
        }
        require(configurationId in configurations) {
            "Snapshot references an unknown logging configuration: $configurationId"
        }
    }

    private fun requireUnique(values: Collection<String>, label: String) {
        require(values.size == values.toSet().size) { "Backup contains duplicate $label" }
    }
}
