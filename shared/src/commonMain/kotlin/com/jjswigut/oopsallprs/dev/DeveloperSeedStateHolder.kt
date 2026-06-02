package com.jjswigut.oopsallprs.dev

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeveloperSeedStateHolder(
    private val useCase: DeveloperSeedUseCase
) {
    private val _state = MutableStateFlow(DeveloperSeedState())
    val state: StateFlow<DeveloperSeedState> = _state

    suspend fun load(scenario: DeveloperSeedScenario): DeveloperSeedResult {
        _state.value = _state.value.copy(loadingScenario = scenario)
        val result = useCase.load(scenario)
        _state.value = _state.value.copy(
            loadingScenario = null,
            lastResult = result
        )
        return result
    }
}
