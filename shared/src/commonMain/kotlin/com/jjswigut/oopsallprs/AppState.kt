package com.jjswigut.oopsallprs

import com.jjswigut.oopsallprs.data.exercise.defaultExerciseSeedCsv
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
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
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
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestNotificationScheduler
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

    companion object {
        fun create(
            databaseDriverFactory: PlatformDatabaseDriverFactory,
            fileExportHandoff: FileExportHandoff? = null,
            restNotificationScheduler: RestNotificationScheduler? = null,
            developerToolsEnabled: Boolean = false
        ): AppState =
            create(
                database = WorkoutDatabase(databaseDriverFactory.createDriver()),
                fileExportHandoff = fileExportHandoff,
                restNotificationScheduler = restNotificationScheduler,
                developerToolsEnabled = developerToolsEnabled
            )

        fun create(
            database: WorkoutDatabase,
            fileExportHandoff: FileExportHandoff? = null,
            restNotificationScheduler: RestNotificationScheduler? = null,
            developerToolsEnabled: Boolean = false
        ): AppState {
            val store = SqlFoundationStore(database)
            val workouts = SqlWorkoutRepository(store)
            val sets = SqlSetLedgerRepository(store)
            val routineRepo = SqlRoutineRepository(store)
            val exerciseRepo = SqlExerciseRepository(store)
            val progress = SqlProgressRepository(store)
            val previousDefaults = PreviousWorkoutDefaultsUseCase(workouts)
            val lifecycle = WorkoutLifecycleUseCases(
                workouts = workouts,
                sessions = workouts,
                routines = routineRepo,
                activeUx = workouts,
                preferences = store,
                notifications = restNotificationScheduler,
                previousDefaults = previousDefaults
            )
            val setLogging = SetLoggingUseCases(workouts, sets, store)
            val activePrFeedback = ActivePrFeedbackUseCase(progress)
            val personalRecordDerivation = PersonalRecordDerivationUseCase(progress)
            val routineUseCases = RoutineUseCases(workouts, routineRepo, workouts, personalRecordDerivation, store, restNotificationScheduler)
            val activeWorkout = ActiveWorkoutStateHolder(setLogging, lifecycle, workouts, activePrFeedback, previousDefaults)
            val exerciseCatalog = ExerciseCatalogUseCases(exerciseRepo, workouts)
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
                workoutHome = WorkoutHomeStateHolder(lifecycle, routineUseCases),
                activeWorkout = activeWorkout,
                exercisePicker = ExercisePickerStateHolder(exerciseCatalog, activeWorkout),
                exerciseManagement = ExerciseManagementStateHolder(exerciseCatalog),
                exerciseCatalog = exerciseCatalog,
                routines = RoutineStateHolder(routineUseCases, exerciseCatalog),
                progress = ProgressStateHolder(progress, workouts, store),
                history = HistoryStateHolder(workouts, progress, routineUseCases),
                profile = ProfileStateHolder(
                    preferences = store,
                    exports = store,
                    exportHandoff = fileExportHandoff?.let { handoff ->
                        { file -> handoff.share(file.fileName, file.content) }
                    }
                ),
                developerSeeds = developerSeeds
            )
        }

    }
}
