package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupPackageCodecTest {
    @Test
    fun backupPackageRoundTripsAsPlainJson() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
        val reader = BackupSnapshotReader(
            workouts = harness.store,
            sessions = harness.store,
            activeUx = harness.store,
            routines = harness.store,
            exercises = harness.store,
            preferences = harness.store,
            progress = harness.store,
            deviceId = "test-device"
        )
        val codec = BackupPackageCodec()
        val pkg = reader.createPackage(instant(3_000)).successValue()

        val json = codec.encode(pkg).successValue()
        val decoded = codec.decode(json).successValue()

        assertTrue(json.contains("\"formatVersion\""))
        assertTrue(json.contains("Bench Press"))
        assertEquals(pkg.lastLocalRevision, decoded.lastLocalRevision)
        assertEquals(pkg.summary.workoutCount, decoded.summary.workoutCount)
        assertEquals("test-device", decoded.deviceId)
    }
}
