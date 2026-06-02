package com.jjswigut.oopsallprs.domain.repository

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlinx.datetime.Instant

interface WorkoutRepository {
    suspend fun createActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout>
    suspend fun activeWorkout(id: FoundationId): ActiveWorkout?
    suspend fun currentActiveWorkout(): ActiveWorkout?
    suspend fun saveActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout>
    suspend fun discardActiveWorkout(id: FoundationId, now: Instant): FoundationResult<Unit>
    suspend fun finishWorkout(workout: CompletedWorkout): FoundationResult<CompletedWorkout>
    suspend fun deleteCompletedWorkout(id: FoundationId, now: Instant): FoundationResult<Unit>
    suspend fun completedWorkout(id: FoundationId): CompletedWorkout?
    suspend fun completedWorkouts(): List<CompletedWorkout>
}

interface SessionRepository {
    suspend fun load(): ActiveSessionState?
    suspend fun save(state: ActiveSessionState): FoundationResult<ActiveSessionState>
    suspend fun clear(now: Instant): FoundationResult<Unit>
}

interface ActiveWorkoutUxRepository {
    suspend fun loadUxSession(activeWorkoutId: FoundationId): ActiveWorkoutUxSession?
    suspend fun saveUxSession(session: ActiveWorkoutUxSession): FoundationResult<ActiveWorkoutUxSession>
    suspend fun loadSetDrafts(activeWorkoutId: FoundationId): List<PersistedSetDraft>
    suspend fun saveSetDraft(draft: PersistedSetDraft): FoundationResult<PersistedSetDraft>
    suspend fun clearExerciseDrafts(activeWorkoutId: FoundationId, exerciseInstanceId: FoundationId): FoundationResult<Unit>
    suspend fun clearWorkoutUx(activeWorkoutId: FoundationId, now: Instant): FoundationResult<Unit>
}

interface SetLedgerRepository {
    suspend fun confirmSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet>
    suspend fun editLoggedSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet>
    suspend fun deleteLoggedSet(workoutId: FoundationId, setId: FoundationId, now: Instant): FoundationResult<ExerciseSet>
}

interface RoutineRepository {
    suspend fun routine(id: FoundationId): ReusableRoutine?
    suspend fun routines(): List<ReusableRoutine>
    suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine>
    suspend fun deleteRoutine(id: FoundationId, now: Instant): FoundationResult<Unit>
}

interface ExerciseRepository {
    suspend fun search(query: String): List<ExerciseCatalogItem>
    suspend fun all(): List<ExerciseCatalogItem>
    suspend fun exercise(id: FoundationId): ExerciseCatalogItem?
    suspend fun userCreatedExercises(): List<ExerciseCatalogItem>
    suspend fun saveSeedItems(items: List<ExerciseCatalogItem>, import: ExerciseSeedImport): FoundationResult<ExerciseSeedImport>
    suspend fun saveUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem>
    suspend fun updateUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem>
    suspend fun archiveUserExercise(id: FoundationId, now: Instant): FoundationResult<Unit>
}

interface PreferencesRepository {
    suspend fun weightUnit(): WeightUnit
    suspend fun setWeightUnit(unit: WeightUnit): FoundationResult<WeightUnit>
    suspend fun weightStep(unit: WeightUnit): Double
    suspend fun setWeightStep(unit: WeightUnit, step: Double): FoundationResult<Double>
    suspend fun defaultRestSeconds(): Int
    suspend fun setDefaultRestSeconds(seconds: Int): FoundationResult<Int>
    suspend fun restSoundEnabled(): Boolean
    suspend fun setRestSoundEnabled(enabled: Boolean): FoundationResult<Boolean>
}

interface ProgressRepository {
    suspend fun replaceRecords(records: List<PersonalRecord>, points: List<ProgressPoint>): FoundationResult<Unit>
    suspend fun personalRecords(): List<PersonalRecord>
    suspend fun progressPoints(): List<ProgressPoint>
}

interface ExportRepository {
    suspend fun export(type: ExportType, unit: WeightUnit): FoundationResult<ExportFile>
}
