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
    fun firstSetTimerPreferenceDefaultsOnAndPersistsChanges() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        assertTrue(holder.state.value.startWorkoutTimerWithFirstSet)

        holder.setStartWorkoutTimerWithFirstSet(false).successValue()
        val recreated = ProfileStateHolder(preferences = store, exports = store)
        recreated.hydrate()

        assertFalse(recreated.state.value.startWorkoutTimerWithFirstSet)
        assertFalse(store.startWorkoutTimerWithFirstSet())
    }

    @Test
    fun hydrateLoadsRestPreferences() = runTest {
        val store = InMemoryFoundationStore()
        store.setDefaultRestSeconds(180).successValue()
        store.setRestSoundEnabled(false).successValue()
        val holder = ProfileStateHolder(preferences = store, exports = store)

        holder.hydrate()

        assertTrue(holder.state.value.isHydrated)
        assertEquals(180, holder.state.value.defaultRestSeconds)
        assertEquals(180, holder.state.value.draftDefaultRestSeconds)
        assertFalse(holder.state.value.isDefaultRestPickerVisible)
        assertFalse(holder.state.value.restSoundEnabled)
        assertTrue(holder.state.value.restTimerSurfaceEnabled)
    }

    @Test
    fun openingRestPickerCopiesTheSavedValueIntoTheDraft() = runTest {
        val store = InMemoryFoundationStore()
        store.setDefaultRestSeconds(180).successValue()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        holder.openDefaultRestPicker()

        assertTrue(holder.state.value.isDefaultRestPickerVisible)
        assertEquals(180, holder.state.value.defaultRestSeconds)
        assertEquals(180, holder.state.value.draftDefaultRestSeconds)
    }

    @Test
    fun rollerChangesRemainDraftOnlyUntilSave() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        holder.openDefaultRestPicker()
        holder.setDraftDefaultRestSeconds(300)

        assertEquals(120, holder.state.value.defaultRestSeconds)
        assertEquals(300, holder.state.value.draftDefaultRestSeconds)
        assertEquals(120, store.defaultRestSeconds())
        assertTrue(holder.state.value.isDefaultRestPickerVisible)
    }

    @Test
    fun savingRestDraftPersistsAndClosesPicker() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        holder.openDefaultRestPicker()
        holder.setDraftDefaultRestSeconds(300)
        holder.saveDefaultRest().successValue()

        assertEquals(300, holder.state.value.defaultRestSeconds)
        assertEquals(300, holder.state.value.draftDefaultRestSeconds)
        assertFalse(holder.state.value.isDefaultRestPickerVisible)
        assertEquals(300, store.defaultRestSeconds())

        holder.setRestSoundEnabled(false).successValue()
        val recreated = ProfileStateHolder(preferences = store, exports = store)
        recreated.hydrate()

        assertEquals(300, recreated.state.value.defaultRestSeconds)
        assertFalse(recreated.state.value.restSoundEnabled)
    }

    @Test
    fun cancelOrDialogDismissKeepsSavedRestAndResetsDraft() = runTest {
        val store = InMemoryFoundationStore()
        store.setDefaultRestSeconds(180).successValue()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        holder.openDefaultRestPicker()
        holder.setDraftDefaultRestSeconds(300)
        holder.cancelDefaultRestPicker()

        assertEquals(180, holder.state.value.defaultRestSeconds)
        assertEquals(180, holder.state.value.draftDefaultRestSeconds)
        assertEquals(180, store.defaultRestSeconds())
        assertFalse(holder.state.value.isDefaultRestPickerVisible)
    }

    @Test
    fun activeTimerSurfaceDefaultsOnAndCanBePersistentlyDisabled() = runTest {
        val store = InMemoryFoundationStore()
        var refreshCount = 0
        val holder = ProfileStateHolder(
            preferences = store,
            exports = store,
            onRestTimerSurfacePreferenceChanged = { refreshCount += 1 }
        )
        holder.hydrate()

        assertTrue(holder.state.value.restTimerSurfaceEnabled)

        holder.setRestTimerSurfaceEnabled(false).successValue()
        val recreated = ProfileStateHolder(preferences = store, exports = store)
        recreated.hydrate()

        assertFalse(recreated.state.value.restTimerSurfaceEnabled)
        assertFalse(store.restTimerSurfaceEnabled())
        assertEquals(1, refreshCount)
    }
}
