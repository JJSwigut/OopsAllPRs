package com.jjswigut.oopsallprs

import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.exercise.defaultExerciseSeedCsv
import com.jjswigut.oopsallprs.data.repository.SqlBackupRepository
import com.jjswigut.oopsallprs.data.repository.SqlBackupSyncRepository
import com.jjswigut.oopsallprs.data.repository.SqlExerciseRepository
import com.jjswigut.oopsallprs.data.repository.SqlFoundationStore
import com.jjswigut.oopsallprs.data.repository.SqlProgressRepository
import com.jjswigut.oopsallprs.data.repository.SqlRoutineRepository
import com.jjswigut.oopsallprs.data.repository.SqlSetLedgerRepository
import com.jjswigut.oopsallprs.data.repository.SqlWorkoutRepository
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.dev.DeveloperSeedStateHolder
import com.jjswigut.oopsallprs.dev.DeveloperSeedUseCase
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.usecase.ActivePrFeedbackUseCase
import com.jjswigut.oopsallprs.domain.usecase.CompletedWorkoutCorrectionUseCase
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.ExerciseLoggingConfigurationUseCases
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.PreviousWorkoutDefaultsUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerStateHolder
import com.jjswigut.oopsallprs.ui.exercise.ExerciseManagementStateHolder
import com.jjswigut.oopsallprs.ui.history.HistoryStateHolder
import com.jjswigut.oopsallprs.ui.navigation.AppNavigationStateHolder
import com.jjswigut.oopsallprs.ui.profile.ProfileStateHolder
import com.jjswigut.oopsallprs.ui.progress.ProgressStateHolder
import com.jjswigut.oopsallprs.ui.routine.RoutineStateHolder
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import com.jjswigut.oopsallprs.ui.workout.WorkoutHomeStateHolder
import com.jjswigut.oopsallprs.platform.FileExportHandoff
import com.jjswigut.oopsallprs.platform.BackupDocumentHandoff
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import com.jjswigut.oopsallprs.di.createExerciseLoggingComposition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class AppState(
    val workoutLifecycle: WorkoutLifecycleUseCases,
    val setLogging: SetLoggingUseCases,
    val navigation: AppNavigationStateHolder,
    val workoutHome: WorkoutHomeStateHolder,
    val activeWorkout: ActiveWorkoutStateHolder,
    val exercisePicker: ExercisePickerStateHolder,
    val exerciseManagement: ExerciseManagementStateHolder,
    val exerciseCatalog: ExerciseCatalogUseCases,
    val routines: RoutineStateHolder,
    val progress: ProgressStateHolder,
    val history: HistoryStateHolder,
    val profile: ProfileStateHolder,
    private val fullAccess: FullAccessUseCases,
    val exerciseLoggingConfiguration: ExerciseLoggingConfigurationUseCases? = null,
    val developerSeeds: DeveloperSeedStateHolder? = null
) {
    private val _activeSession = MutableStateFlow<ActiveSessionState?>(null)
    val activeSession: StateFlow<ActiveSessionState?> = _activeSession

    suspend fun hydrate(now: Instant = Clock.System.now()) {
        exerciseCatalog.ensureSeeded(defaultExerciseSeedCsv())
        val restored = workoutLifecycle.restoreActiveSession(now)
        _activeSession.value = restored
        workoutHome.hydrate()
        routines.refresh()
        restored?.activeWorkoutId?.let { activeWorkout.hydrate(it) }
        navigation.hydrate(restored, now)
        profile.hydrate()
    }

    suspend fun refreshActiveSession(now: Instant = Clock.System.now()) {
        val restored = workoutLifecycle.restoreActiveSession(now)
        _activeSession.value = restored
        navigation.hydrate(restored, now)
    }

    suspend fun checkBackupSyncOnLaunchOrResume() {
        profile.checkLinkedBackup()
    }

    suspend fun refreshFullAccessEntitlements() {
        fullAccess.refreshEntitlements()
        workoutHome.refreshFullAccess()
        profile.hydrate()
    }

    companion object {
        fun create(
            databaseDriverFactory: PlatformDatabaseDriverFactory,
            fileExportHandoff: FileExportHandoff? = null,
            backupDocumentHandoff: BackupDocumentHandoff? = null,
            fullAccessBilling: FullAccessBillingAdapter? = null,
            restAlertScheduler: RestAlertScheduler? = null,
            developerToolsEnabled: Boolean = false
        ): AppState =
            create(
                database = WorkoutDatabase(databaseDriverFactory.createDriver()),
                fileExportHandoff = fileExportHandoff,
                backupDocumentAdapter = backupDocumentHandoff,
                fullAccessBilling = fullAccessBilling,
                restAlertScheduler = restAlertScheduler,
                developerToolsEnabled = developerToolsEnabled
            )

        fun create(
            database: WorkoutDatabase,
            fileExportHandoff: FileExportHandoff? = null,
            backupDocumentAdapter: BackupDocumentAdapter? = null,
            fullAccessBilling: FullAccessBillingAdapter? = null,
            restAlertScheduler: RestAlertScheduler? = null,
            developerToolsEnabled: Boolean = false
        ): AppState {
            val store = SqlFoundationStore(database)
            val backupRepository = SqlBackupRepository(database, store)
            val backupSyncRepository = SqlBackupSyncRepository(database)
            val backupSync = BackupSyncCoordinator(backupRepository, backupSyncRepository, backupDocumentAdapter)
            val workouts = SqlWorkoutRepository(store)
            val sets = SqlSetLedgerRepository(store)
            val routineRepo = SqlRoutineRepository(store)
            val exerciseRepo = SqlExerciseRepository(store)
            val exerciseLogging = createExerciseLoggingComposition(
                repositoryCapabilities = store,
                exercises = exerciseRepo,
                workouts = workouts,
                activeUx = workouts
            )
            val progress = SqlProgressRepository(store)
            val fullAccess = FullAccessUseCases(store, fullAccessBilling)
            val previousDefaults = PreviousWorkoutDefaultsUseCase(workouts, store)
            val lifecycle = WorkoutLifecycleUseCases(
                workouts = workouts,
                sessions = workouts,
                routines = routineRepo,
                activeUx = workouts,
                preferences = store,
                notifications = restAlertScheduler,
                previousDefaults = previousDefaults
            )
            val setLogging = SetLoggingUseCases(
                workouts = workouts,
                setLedger = sets,
                preferences = store,
                configurationManagement = exerciseLogging.management,
                activeUx = workouts
            )
            val activePrFeedback = ActivePrFeedbackUseCase(progress, store)
            val personalRecordDerivation = PersonalRecordDerivationUseCase(progress, store)
            val completedWorkoutCorrections = CompletedWorkoutCorrectionUseCase(
                workouts = workouts,
                corrections = workouts,
                configurations = store,
                personalRecords = personalRecordDerivation
            )
            val routineUseCases = RoutineUseCases(
                workouts,
                routineRepo,
                workouts,
                personalRecordDerivation,
                store,
                restAlertScheduler,
                fullAccess,
                exerciseLogging.management
            )
            val activeWorkout = ActiveWorkoutStateHolder(
                setLogging = setLogging,
                lifecycle = lifecycle,
                activeUx = workouts,
                activePrFeedback = activePrFeedback,
                previousDefaults = previousDefaults,
                configurationManagement = exerciseLogging.management,
                preferences = store
            )
            val exerciseCatalog = ExerciseCatalogUseCases(exerciseRepo, workouts, exerciseLogging.configurations)
            val developerSeeds = if (developerToolsEnabled) {
                DeveloperSeedStateHolder(
                    DeveloperSeedUseCase(
                        exerciseCatalog = exerciseCatalog,
                        workoutLifecycle = lifecycle,
                        setLogging = setLogging,
                        routines = routineUseCases,
                        workouts = workouts,
                        progress = progress
                    )
                )
            } else {
                null
            }
            return AppState(
                workoutLifecycle = lifecycle,
                setLogging = setLogging,
                navigation = AppNavigationStateHolder(lifecycle),
                workoutHome = WorkoutHomeStateHolder(lifecycle, routineUseCases, fullAccess),
                activeWorkout = activeWorkout,
                exercisePicker = ExercisePickerStateHolder(exerciseCatalog, activeWorkout),
                exerciseManagement = ExerciseManagementStateHolder(exerciseCatalog),
                exerciseCatalog = exerciseCatalog,
                routines = RoutineStateHolder(routineUseCases, exerciseCatalog),
                progress = ProgressStateHolder(progress, workouts, store),
                history = HistoryStateHolder(
                    workouts,
                    progress,
                    routineUseCases,
                    completedWorkoutCorrections,
                    store
                ),
                profile = ProfileStateHolder(
                    preferences = store,
                    exports = store,
                    exportHandoff = fileExportHandoff?.let { handoff ->
                        { file -> handoff.share(file.fileName, file.content) }
                    },
                    backupSync = backupSync,
                    fullAccess = fullAccess,
                    onRestTimerSurfacePreferenceChanged = {
                        lifecycle.refreshRestAlertForPreference()
                    }
                ),
                fullAccess = fullAccess,
                exerciseLoggingConfiguration = exerciseLogging.management,
                developerSeeds = developerSeeds
            )
        }

    }
}
