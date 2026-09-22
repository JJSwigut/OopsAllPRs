package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullAccessDeveloperDemoTest {
    @Test
    fun developerDemoAccessUnlocksASeededLocalLedger() = runTest {
        val store = InMemoryFoundationStore()
        store.updateFullAccess { FullAccessState(completedFreeWorkouts = 10) }
        val access = FullAccessUseCases(store)

        access.enableDeveloperDemoAccess()

        val result = access.loadState()
        assertEquals(10, result.completedFreeWorkouts)
        assertTrue(result.hasFullAccess)
    }
}
