package com.jjswigut.oopsallprs.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ActiveWorkoutOpenRequest {
    private val _generation = MutableStateFlow(0L)
    internal val generation: StateFlow<Long> = _generation

    fun request() {
        _generation.value += 1
    }
}
