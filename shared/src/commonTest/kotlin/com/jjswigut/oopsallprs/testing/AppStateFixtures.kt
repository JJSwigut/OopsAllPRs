package com.jjswigut.oopsallprs.testing

import com.jjswigut.oopsallprs.AppState
import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.dev.DeveloperSeedStateHolder
import com.jjswigut.oopsallprs.dev.DeveloperSeedUseCase
import com.jjswigut.oopsallprs.domain.usecase.ActivePrFeedbackUseCase
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerStateHolder
import com.jjswigut.oopsallprs.ui.exercise.ExerciseManagementStateHolder
import com.jjswigut.oopsallprs.ui.history.HistoryStateHolder
import com.jjswigut.oopsallprs.ui.navigation.AppNavigationStateHolder
import com.jjswigut.oopsallprs.ui.profile.ProfileStateHolder
import com.jjswigut.oopsallprs.ui.progress.ProgressStateHolder
import com.jjswigut.oopsallprs.ui.routine.RoutineStateHolder
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import com.jjswigut.oopsallprs.ui.workout.WorkoutHomeStateHolder

fun testAppState(
    developerToolsEnabled: Boolean = false,
    store: InMemoryFoundationStore = InMemoryFoundationStore(),
    billing: FullAccessBillingAdapter? = null
): AppState {
    val lifecycle = WorkoutLifecycleUseCases(store, store, store, store)
    val setLogging = SetLoggingUseCases(store, store)
    val activePrFeedback = ActivePrFeedbackUseCase(store)
    val personalRecordDerivation = PersonalRecordDerivationUseCase(store)
    val fullAccess = FullAccessUseCases(store, billing)
    val routineUseCases = RoutineUseCases(store, store, store, personalRecordDerivation)
    val activeWorkout = ActiveWorkoutStateHolder(setLogging, lifecycle, store, activePrFeedback)
    val exerciseCatalog = ExerciseCatalogUseCases(store, store)
    val developerSeeds = if (developerToolsEnabled) {
        DeveloperSeedStateHolder(
            DeveloperSeedUseCase(
                exerciseCatalog = exerciseCatalog,
                workoutLifecycle = lifecycle,
                setLogging = setLogging,
                routines = routineUseCases,
                workouts = store,
                progress = store,
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
        progress = ProgressStateHolder(store, store, store),
        history = HistoryStateHolder(store, store),
        profile = ProfileStateHolder(store, store, fullAccess = fullAccess),
        fullAccess = fullAccess,
        developerSeeds = developerSeeds
    )
}
