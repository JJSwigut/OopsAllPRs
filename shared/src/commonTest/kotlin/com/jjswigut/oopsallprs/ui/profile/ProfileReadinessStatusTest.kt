package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileReadinessStatusTest {
    @Test
    fun defaultReadinessLabelsStayLocalFirstAndAlphaSafe() {
        val status = LocalReadinessStatus()

        assertEquals("Local database", status.storageLabel)
        assertEquals("Cloud sync off", status.syncLabel)
        assertEquals("Android Auto Backup eligible", status.backupLabel)
        assertEquals("Timers recover if alerts are off", status.restNotificationLabel)
        assertEquals("CSV export available", status.exportLabel)
        assertEquals("Manual alpha smoke pending", status.alphaGateLabel)
    }

    @Test
    fun hydratePublishesReadinessLabelsWithoutCloudClaims() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(preferences = store, exports = store)

        holder.hydrate()

        val status = holder.state.value.localStatus
        assertEquals("Local database", status.storageLabel)
        assertEquals("Cloud sync off", status.syncLabel)
        assertEquals("Timers recover if alerts are off", status.restNotificationLabel)
        assertEquals("CSV export available", status.exportLabel)
    }
}
