package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class BackupSnapshotReader(
    private val workouts: WorkoutRepository,
    private val sessions: SessionRepository,
    private val activeUx: ActiveWorkoutUxRepository,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val preferences: PreferencesRepository,
    private val progress: ProgressRepository,
    private val deviceId: String = "local-device",
    private val schemaVersion: Int = WorkoutDatabase.Schema.version.toInt(),
    private val revisionCalculator: LocalRevisionCalculator = LocalRevisionCalculator()
) {
    suspend fun createPackage(now: Instant = Clock.System.now()): FoundationResult<BackupPackage> {
        val activeWorkout = workouts.currentActiveWorkout()
        val session = sessions.load()
        val uxSession: ActiveWorkoutUxSession? = activeWorkout?.id?.let { activeUx.loadUxSession(it) }
        val drafts: List<PersistedSetDraft> = activeWorkout?.id?.let { activeUx.loadSetDrafts(it) }.orEmpty()
        val completed = workouts.completedWorkouts()
        val routines = routines.routines()
        val exercises = exercises.all()
        val records = progress.personalRecords()
        val points = progress.progressPoints()
        val summary = summary(
            activeWorkout = activeWorkout?.toDto(),
            completed = completed.map { it.toDto() },
            routines = routines.map { it.toDto() },
            exercises = exercises.map { it.toDto() },
            records = records.map { it.toDto() },
            points = points.map { it.toDto() }
        )
        val revision = revision(summary)
        return foundationSuccess(
            BackupPackage(
                formatVersion = BACKUP_FORMAT_VERSION,
                createdAt = now.toBackupMillis(),
                deviceId = deviceId,
                lastLocalRevision = revision.value,
                appSchemaVersion = schemaVersion,
                summary = summary.toDto(),
                preferences = PreferencesSnapshotDto(
                    weightUnit = preferences.weightUnit().name,
                    weightStepPounds = preferences.weightStep(WeightUnit.POUNDS),
                    weightStepKilograms = preferences.weightStep(WeightUnit.KILOGRAMS),
                    defaultRestSeconds = preferences.defaultRestSeconds(),
                    restSoundEnabled = preferences.restSoundEnabled()
                ),
                exercises = exercises.map { it.toDto() },
                routines = routines.map { it.toDto() },
                activeWorkout = activeWorkout?.toDto(),
                activeSession = session?.toDto(),
                activeUxSession = uxSession?.toDto(),
                activeSetDrafts = drafts.map { it.toDto() },
                completedWorkouts = completed.map { it.toDto() },
                personalRecords = records.map { it.toDto() },
                progressPoints = points.map { it.toDto() },
                exportMetadata = emptyList()
            )
        )
    }

    suspend fun revision(): BackupRevision {
        val activeWorkout = workouts.currentActiveWorkout()?.toDto()
        val completed = workouts.completedWorkouts().map { it.toDto() }
        val routines = routines.routines().map { it.toDto() }
        val exercises = exercises.all().map { it.toDto() }
        val records = progress.personalRecords().map { it.toDto() }
        val points = progress.progressPoints().map { it.toDto() }
        return revision(summary(activeWorkout, completed, routines, exercises, records, points))
    }

    suspend fun currentSummary(): SnapshotSummary =
        revision().summary

    fun revision(pkg: BackupPackage): BackupRevision =
        BackupRevision(
            value = pkg.lastLocalRevision,
            timestamp = pkg.createdAt.toBackupInstant(),
            summary = pkg.summary.toDomain()
        )

    private fun revision(summary: SnapshotSummary): BackupRevision {
        val timestamps = mutableListOf<Instant?>(
            summary.latestUpdatedTimestamp,
            summary.latestWorkoutTimestamp
        )
        return revisionCalculator.revision(summary, timestamps)
    }

    private fun summary(
        activeWorkout: ActiveWorkoutDto?,
        completed: List<CompletedWorkoutDto>,
        routines: List<RoutineDto>,
        exercises: List<ExerciseCatalogItemDto>,
        records: List<PersonalRecordDto>,
        points: List<ProgressPointDto>
    ): SnapshotSummary {
        val activeSetCount = activeWorkout?.exercises.orEmpty().sumOf { it.sets.size }
        val completedSetCount = completed.sumOf { workout -> workout.exercises.sumOf { it.loggedSets.size } }
        val latestWorkoutTimestamp = completed.maxOfOrNull { it.finishedAt }?.toBackupInstant()
        val latestUpdated = buildList {
            activeWorkout?.updatedAt?.let { add(it) }
            addAll(completed.map { it.finishedAt })
            addAll(routines.map { it.updatedAt })
            addAll(exercises.map { it.updatedAt })
            addAll(records.map { it.createdAt })
            addAll(points.map { it.recordedAt })
        }.maxOrNull()?.toBackupInstant()
        return SnapshotSummary(
            workoutCount = completed.size,
            setCount = activeSetCount + completedSetCount,
            routineCount = routines.count { it.archivedAt == null },
            customExerciseCount = exercises.count { it.isUserCreated && it.archivedAt == null },
            progressRecordCount = records.size + points.size,
            hasActiveWorkout = activeWorkout != null,
            latestWorkoutTimestamp = latestWorkoutTimestamp,
            latestUpdatedTimestamp = latestUpdated
        )
    }
}
