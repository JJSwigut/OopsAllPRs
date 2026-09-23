package com.jjswigut.oopsallprs.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveWorkoutOpenRequestTest {
    @Test
    fun requestAdvancesGeneration() {
        val request = ActiveWorkoutOpenRequest()

        request.request()
        request.request()

        assertEquals(2L, request.generation.value)
    }
}
