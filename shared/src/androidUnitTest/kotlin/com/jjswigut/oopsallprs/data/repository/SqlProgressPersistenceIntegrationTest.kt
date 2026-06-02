package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlProgressPersistenceIntegrationTest {
    @Test
    fun personalRecordsAndProgressPointsKeepSourceIdsAfterRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val completedId = createCompletedMixedWorkout(repos)
        val originalRecords = repos.progress.personalRecords()

        val recovered = harness.repositories()
        val records = recovered.progress.personalRecords()
        val points = recovered.progress.progressPoints()
        val bestWeight = assertNotNull(records.firstOrNull { it.recordKind == PersonalRecordKind.WEIGHT_FOR_REPS })

        assertEquals(originalRecords.map { it.recordKind }.toSet(), records.map { it.recordKind }.toSet())
        assertEquals(completedId, bestWeight.sourceWorkoutId)
        assertTrue(bestWeight.sourceSetId.value.startsWith("set-"))
        assertTrue(points.any { it.sourceWorkoutId == completedId && it.sourceSetId == bestWeight.sourceSetId })
    }
}
