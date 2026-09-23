package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.repository.ExportRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.navigation.PaletteMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileStateHolderTest {
    @Test
    fun hydrateLoadsPersistedWeightUnit() = runTest {
        val store = InMemoryFoundationStore()
        store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
        val holder = ProfileStateHolder(preferences = store, exports = store)

        holder.hydrate()

        assertTrue(holder.state.value.isHydrated)
        assertEquals(WeightUnit.KILOGRAMS, holder.state.value.weightUnit)
    }

    @Test
    fun setWeightUnitPersistsForNextProfileStateHolder() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)

        holder.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
        val recreated = ProfileStateHolder(preferences = store, exports = store)
        recreated.hydrate()

        assertEquals(WeightUnit.KILOGRAMS, recreated.state.value.weightUnit)
        assertNull(recreated.state.value.exportError)
    }

    @Test
    fun saveWeightStepPersistsDraftAndClosesPicker() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        holder.openWeightStepPicker()
        holder.setDraftWeightStep(2.5)
        holder.saveWeightStep().successValue()

        assertEquals(2.5, holder.state.value.weightStep)
        assertFalse(holder.state.value.isWeightStepPickerVisible)
        assertEquals(2.5, store.weightStep(WeightUnit.POUNDS))
    }

    @Test
    fun cancelWeightStepPickerKeepsSavedValue() = runTest {
        val holder = ProfileStateHolder(preferences = InMemoryFoundationStore())
        holder.hydrate()

        holder.openWeightStepPicker()
        holder.setDraftWeightStep(2.5)
        holder.cancelWeightStepPicker()

        assertEquals(5.0, holder.state.value.weightStep)
        assertEquals(5.0, holder.state.value.draftWeightStep)
    }

    @Test
    fun changingUnitLoadsThatUnitStep() = runTest {
        val store = InMemoryFoundationStore()
        store.setWeightStep(WeightUnit.POUNDS, 2.5).successValue()
        store.setWeightStep(WeightUnit.KILOGRAMS, 1.25).successValue()
        val holder = ProfileStateHolder(preferences = store, exports = store)
        holder.hydrate()

        holder.setWeightUnit(WeightUnit.KILOGRAMS).successValue()

        assertEquals(WeightUnit.KILOGRAMS, holder.state.value.weightUnit)
        assertEquals(1.25, holder.state.value.weightStep)
        assertNull(holder.state.value.weightStepError)
    }

    @Test
    fun exportPublishesMetadataAndCallsHandoffForEachType() = runTest {
        val store = InMemoryFoundationStore()
        val handedOff = mutableListOf<ExportFile>()
        val holder = ProfileStateHolder(
            preferences = store,
            exports = store,
            exportHandoff = { handedOff += it }
        )

        holder.hydrate()
        ExportType.values().forEach { type ->
            holder.export(type).successValue()
            val lastExport = assertNotNull(holder.state.value.lastExport)
            assertEquals(type, lastExport.type)
            assertEquals(WeightUnit.POUNDS, lastExport.weightUnit)
            assertTrue(lastExport.fileName.endsWith(".csv"))
            assertFalse(holder.state.value.isExporting)
            assertNull(holder.state.value.exportError)
        }

        assertEquals(ExportType.values().size, handedOff.size)
    }

    @Test
    fun exportFailureLeavesRecoverableError() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(
            preferences = store,
            exports = FailingExportRepository()
        )

        holder.export(ExportType.WORKOUTS)

        assertFalse(holder.state.value.isExporting)
        assertNull(holder.state.value.lastExport)
        assertEquals("Export unavailable", holder.state.value.exportError)
    }

    @Test
    fun exportHandoffFailureLeavesRecoverableError() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(
            preferences = store,
            exports = store,
            exportHandoff = { error("iOS export handoff requires a UIViewController presenter") }
        )

        holder.export(ExportType.WORKOUTS)

        assertFalse(holder.state.value.isExporting)
        assertNull(holder.state.value.lastExport)
        assertEquals(
            "iOS export handoff requires a UIViewController presenter",
            holder.state.value.exportError
        )
    }

    @Test
    fun interactionPreferencesUpdateRuntimeState() {
        val holder = ProfileStateHolder()

        holder.setPaletteMode(PaletteMode.LIGHT)
        holder.setHapticsEnabled(false)
        holder.setReduceMotion(true)

        assertEquals(PaletteMode.LIGHT, holder.state.value.paletteMode)
        assertFalse(holder.state.value.hapticsEnabled)
        assertTrue(holder.state.value.reduceMotion)
    }

    private class FailingExportRepository : ExportRepository {
        override suspend fun export(type: ExportType, unit: WeightUnit): FoundationResult<ExportFile> =
            foundationFailure(FoundationError.Persistence("Export unavailable"))
    }
}
