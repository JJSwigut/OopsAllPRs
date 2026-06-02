package com.jjswigut.oopsallprs.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import kotlinx.datetime.Instant

internal class SqlFoundationStoreTestHarness {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val database: WorkoutDatabase

    init {
        WorkoutDatabase.Schema.create(driver)
        database = WorkoutDatabase(driver)
    }

    fun repositories(): SqlRepositoryBundle {
        val store = SqlFoundationStore(database)
        val workouts = SqlWorkoutRepository(store)
        val sets = SqlSetLedgerRepository(store)
        val routines = SqlRoutineRepository(store)
        val exercises = SqlExerciseRepository(store)
        val progress = SqlProgressRepository(store)
        val derivation = PersonalRecordDerivationUseCase(progress)
        return SqlRepositoryBundle(
            store = store,
            workouts = workouts,
            sets = sets,
            routines = routines,
            exercises = exercises,
            progress = progress,
            lifecycle = WorkoutLifecycleUseCases(workouts, workouts, routines, workouts, preferences = store),
            setLogging = SetLoggingUseCases(workouts, sets, store),
            routineUseCases = RoutineUseCases(workouts, routines, workouts, derivation, preferences = store),
            exerciseCatalog = ExerciseCatalogUseCases(exercises, workouts),
            personalRecords = derivation
        )
    }
}

internal data class SqlRepositoryBundle(
    val store: SqlFoundationStore,
    val workouts: SqlWorkoutRepository,
    val sets: SqlSetLedgerRepository,
    val routines: SqlRoutineRepository,
    val exercises: SqlExerciseRepository,
    val progress: SqlProgressRepository,
    val lifecycle: WorkoutLifecycleUseCases,
    val setLogging: SetLoggingUseCases,
    val routineUseCases: RoutineUseCases,
    val exerciseCatalog: ExerciseCatalogUseCases,
    val personalRecords: PersonalRecordDerivationUseCase
)

internal fun instant(ms: Long): Instant = Instant.fromEpochMilliseconds(ms)

internal fun <T> FoundationResult<T>.successValue(): T =
    when (this) {
        is FoundationResult.Failure -> error(error.message)
        is FoundationResult.Success -> value
    }
