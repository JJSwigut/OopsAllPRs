package com.jjswigut.oopsallprs

import com.jjswigut.oopsallprs.testing.testAppState
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppStateDeveloperToolsTest {
    @Test
    fun developerToolsAreDisabledByDefault() {
        val appState = testAppState()

        assertNull(appState.developerSeeds)
    }

    @Test
    fun releaseCapabilityDoesNotCreateDeveloperSeeds() {
        val appState = testAppState(developerToolsEnabled = false)

        assertNull(appState.developerSeeds)
    }

    @Test
    fun debugCapabilityCreatesDeveloperSeeds() {
        val appState = testAppState(developerToolsEnabled = true)

        assertNotNull(appState.developerSeeds)
    }

}
