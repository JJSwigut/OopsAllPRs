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
import com.jjswigut.oopsallprs.domain.usecase.CompletedWorkoutCorrectionUseCase
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
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
    single { FullAccessUseCases(get<SqlFoundationStore>()) }
    single {
        PreviousWorkoutDefaultsUseCase(
            workouts = get<SqlWorkoutRepository>(),
            configurations = get<SqlFoundationStore>()
        )
    }
    single {
        createExerciseLoggingComposition(
            repositoryCapabilities = get<SqlFoundationStore>(),
            exercises = get<SqlExerciseRepository>(),
            workouts = get<SqlWorkoutRepository>(),
            activeUx = get<SqlWorkoutRepository>()
        )
    }
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
    single {
        SetLoggingUseCases(
            workouts = get<SqlWorkoutRepository>(),
            setLedger = get<SqlSetLedgerRepository>(),
            preferences = get<SqlFoundationStore>(),
            configurationManagement = get<ExerciseLoggingComposition>().management,
            activeUx = get<SqlWorkoutRepository>()
        )
    }
    single {
        PersonalRecordDerivationUseCase(
            progressRepository = get<SqlProgressRepository>(),
            loggingConfigurationRepository = get<SqlFoundationStore>()
        )
    }
    single {
        CompletedWorkoutCorrectionUseCase(
            workouts = get<SqlWorkoutRepository>(),
            corrections = get<SqlWorkoutRepository>(),
            configurations = get<SqlFoundationStore>(),
            personalRecords = get<PersonalRecordDerivationUseCase>()
        )
    }
    single {
        RoutineUseCases(
            workouts = get<SqlWorkoutRepository>(),
            routines = get<SqlRoutineRepository>(),
            activeUx = get<SqlWorkoutRepository>(),
            personalRecords = get<PersonalRecordDerivationUseCase>(),
            preferences = get<SqlFoundationStore>(),
            fullAccess = get<FullAccessUseCases>(),
            configurationManagement = get<ExerciseLoggingComposition>().management
        )
    }
    single {
        ExerciseCatalogUseCases(
            exercises = get<SqlExerciseRepository>(),
            workouts = get<SqlWorkoutRepository>(),
            loggingConfigurations = get<ExerciseLoggingComposition>().configurations
        )
    }
    single { ExerciseCatalogInitializer(get<SqlExerciseRepository>()) }
    single { ProgressStateHolder(get<SqlProgressRepository>(), get<SqlWorkoutRepository>(), get<SqlFoundationStore>()) }
    single { WeightInputUseCases() }
}
