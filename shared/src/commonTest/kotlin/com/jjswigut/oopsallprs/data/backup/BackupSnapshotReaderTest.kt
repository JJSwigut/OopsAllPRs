package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupSnapshotReaderTest {
    @Test
    fun snapshotIncludesCompletedLedgerExercisesPreferencesAndProgressContainers() = runTest {
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
            progress = harness.store
        )

        val pkg = reader.createPackage(instant(3_000)).successValue()

        assertEquals(1, pkg.summary.workoutCount)
        assertEquals(1, pkg.summary.setCount)
        assertFalse(pkg.summary.hasActiveWorkout)
        assertTrue(pkg.exercises.isNotEmpty())
        assertEquals("POUNDS", pkg.preferences.weightUnit)
        assertEquals(pkg.personalRecords.size + pkg.progressPoints.size, pkg.summary.progressRecordCount)
    }
}
