package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class AppNavigationStateHolder(
    private val lifecycle: WorkoutLifecycleUseCases,
    initialState: AppShellState = AppShellState()
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<AppShellState> = _state

    suspend fun hydrate(
        session: ActiveSessionState?,
        now: Instant = Clock.System.now()
    ) {
        val resume = ActiveWorkoutResume.fromSession(session, now)
        val route = AppRoute.fromPersisted(session?.lastOpenedRoute, hasActiveWorkout = resume != null)
        val selected = route.topLevel ?: _state.value.previousTopLevelDestination
        _state.value = _state.value.copy(
            selectedDestination = selected,
            previousTopLevelDestination = selected,
            activeWorkoutResume = resume,
            isActiveWorkoutPresented = route.isActiveWorkout && resume != null,
            errorMessage = null
        )
    }

    suspend fun selectDestination(
        destination: TopLevelDestination,
        now: Instant = Clock.System.now()
    ) {
        _state.value = _state.value.copy(
            selectedDestination = destination,
            previousTopLevelDestination = destination,
            isActiveWorkoutPresented = false,
            errorMessage = null
        )
        persistRoute(destination.route, now)
    }

    suspend fun presentActiveWorkout(now: Instant = Clock.System.now()) {
        if (_state.value.activeWorkoutResume == null) {
            _state.value = _state.value.copy(errorMessage = "No active workout to resume")
            return
        }
        _state.value = _state.value.copy(
            previousTopLevelDestination = _state.value.selectedDestination,
            isActiveWorkoutPresented = true,
            errorMessage = null
        )
        persistRoute(AppRoute.ACTIVE_WORKOUT_ROUTE, now)
    }

    suspend fun dismissActiveWorkout(now: Instant = Clock.System.now()) {
        val destination = _state.value.previousTopLevelDestination
        _state.value = _state.value.copy(
            selectedDestination = destination,
            isActiveWorkoutPresented = false,
            errorMessage = null
        )
        persistRoute(destination.route, now)
    }

    fun setLayoutClass(layoutClass: NavigationLayoutClass) {
        _state.value = _state.value.copy(layoutClass = layoutClass)
    }

    fun setPreferences(
        reduceMotion: Boolean,
        hapticsEnabled: Boolean,
        paletteMode: PaletteMode
    ) {
        _state.value = _state.value.copy(
            reduceMotion = reduceMotion,
            hapticsEnabled = hapticsEnabled,
            paletteMode = paletteMode
        )
    }

    fun activeSessionChanged(
        session: ActiveSessionState?,
        now: Instant = Clock.System.now()
    ) {
        val resume = ActiveWorkoutResume.fromSession(session, now)
        _state.value = if (resume == null) {
            _state.value.copy(
                activeWorkoutResume = null,
                isActiveWorkoutPresented = false
            )
        } else {
            _state.value.copy(activeWorkoutResume = resume)
        }
    }

    suspend fun handle(intent: NavigationIntent) {
        when (intent) {
            is NavigationIntent.ActiveSessionChanged -> activeSessionChanged(intent.session, intent.now)
            NavigationIntent.DismissActiveWorkout -> dismissActiveWorkout()
            is NavigationIntent.Hydrate -> hydrate(intent.session, intent.now)
            NavigationIntent.PresentActiveWorkout -> presentActiveWorkout()
            is NavigationIntent.SelectDestination -> selectDestination(intent.destination)
            is NavigationIntent.SetLayoutClass -> setLayoutClass(intent.layoutClass)
            is NavigationIntent.SetPreferences -> setPreferences(
                reduceMotion = intent.reduceMotion,
                hapticsEnabled = intent.hapticsEnabled,
                paletteMode = intent.paletteMode
            )
        }
    }

    private suspend fun persistRoute(route: String, now: Instant) {
        when (val result = lifecycle.saveLastOpenedRoute(route, now)) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(errorMessage = result.error.message)
            }
            is FoundationResult.Success -> Unit
        }
    }
}
