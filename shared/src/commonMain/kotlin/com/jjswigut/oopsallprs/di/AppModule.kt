package com.jjswigut.oopsallprs.di

import com.jjswigut.oopsallprs.data.exercise.ExerciseCatalogInitializer
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.repository.SqlBackupRepository
import com.jjswigut.oopsallprs.data.repository.SqlBackupSyncRepository
import com.jjswigut.oopsallprs.data.repository.SqlExerciseRepository
import com.jjswigut.oopsallprs.data.repository.SqlFoundationStore
import com.jjswigut.oopsallprs.data.repository.SqlProgressRepository
import com.jjswigut.oopsallprs.data.repository.SqlRoutineRepository
import com.jjswigut.oopsallprs.data.repository.SqlSetLedgerRepository
import com.jjswigut.oopsallprs.data.repository.SqlWorkoutRepository
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.PreviousWorkoutDefaultsUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WeightInputUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.ui.progress.ProgressStateHolder
import org.koin.dsl.module

val foundationModule = module {
    single { WorkoutDatabase(get<PlatformDatabaseDriverFactory>().createDriver()) }
    single { SqlFoundationStore(get<WorkoutDatabase>()) }
    single { SqlBackupRepository(get<WorkoutDatabase>(), get<SqlFoundationStore>()) }
    single { SqlBackupSyncRepository(get<WorkoutDatabase>()) }
    single { BackupSyncCoordinator(get<SqlBackupRepository>(), get<SqlBackupSyncRepository>(), documents = null) }
    single { SqlWorkoutRepository(get<SqlFoundationStore>()) }
    single { SqlSetLedgerRepository(get<SqlFoundationStore>()) }
    single { SqlRoutineRepository(get<SqlFoundationStore>()) }
    single { SqlExerciseRepository(get<SqlFoundationStore>()) }
    single { SqlProgressRepository(get<SqlFoundationStore>()) }
    single { PreviousWorkoutDefaultsUseCase(get<SqlWorkoutRepository>()) }
    single {
        WorkoutLifecycleUseCases(
            workouts = get<SqlWorkoutRepository>(),
            sessions = get<SqlWorkoutRepository>(),
            routines = get<SqlRoutineRepository>(),
            activeUx = get<SqlWorkoutRepository>(),
            preferences = get<SqlFoundationStore>(),
            previousDefaults = get<PreviousWorkoutDefaultsUseCase>()
        )
    }
    single { SetLoggingUseCases(get<SqlWorkoutRepository>(), get<SqlSetLedgerRepository>(), get<SqlFoundationStore>()) }
    single { PersonalRecordDerivationUseCase(get<SqlProgressRepository>()) }
    single { RoutineUseCases(get<SqlWorkoutRepository>(), get<SqlRoutineRepository>(), get<SqlWorkoutRepository>(), get<PersonalRecordDerivationUseCase>(), get<SqlFoundationStore>()) }
    single { ExerciseCatalogUseCases(get<SqlExerciseRepository>(), get<SqlWorkoutRepository>()) }
    single { ExerciseCatalogInitializer(get<SqlExerciseRepository>()) }
    single { ProgressStateHolder(get<SqlProgressRepository>(), get<SqlWorkoutRepository>(), get<SqlFoundationStore>()) }
    single { WeightInputUseCases() }
}
