package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileRestPreferenceTest {
    @Test
    fun hydrateLoadsRestPreferences() = runTest {
        val store = InMemoryFoundationStore()
        store.setDefaultRestSeconds(180).successValue()
        store.setRestSoundEnabled(false).successValue()
        val holder = ProfileStateHolder(preferences = store, exports = store)

        holder.hydrate()

        assertTrue(holder.state.value.isHydrated)
        assertEquals(180, holder.state.value.defaultRestSeconds)
        assertFalse(holder.state.value.restSoundEnabled)
    }

    @Test
    fun restPreferencesPersistForNextProfileStateHolder() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)

        holder.setDefaultRestSeconds(300).successValue()
        holder.setRestSoundEnabled(false).successValue()
        val recreated = ProfileStateHolder(preferences = store, exports = store)
        recreated.hydrate()

        assertEquals(300, recreated.state.value.defaultRestSeconds)
        assertFalse(recreated.state.value.restSoundEnabled)
    }
}
