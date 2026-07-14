package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressDerivationVersions
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SqlProgressEvidencePersistenceTest {
    @Test
    fun metricCodeAndDerivationVersionRoundTripAndDriveDistanceExport() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val store = harness.repositories().store
        val record = distanceRecord()
        val point = distancePoint()
        store.replaceRecords(listOf(record), listOf(point)).successValue()

        val recovered = harness.repositories().store
        assertEquals(record, recovered.personalRecords().single())
        assertEquals(point, recovered.progressPoints().single())
        val export = recovered.export(ExportType.PERSONAL_RECORDS, WeightUnit.KILOGRAMS).successValue()
        val row = export.content.lineSequence().drop(1).single()
        assertTrue(row.contains(",longest_distance,2"))
        assertTrue(row.contains(",5000.0 m,"))
        assertTrue(!row.contains("1:23:20"))
    }

    @Test
    fun unknownMetricCodeFailsClosedOnRead() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        harness.database.progressQueriesQueries.insertPersonalRecord(
            "bad-record", "exercise", "TIME", null, null, 1.0, "workout", "set",
            1_000, 1_000, "future_metric", 2
        )

        val error = runCatching { harness.repositories().store.personalRecords() }.exceptionOrNull()

        assertIs<IllegalArgumentException>(error)
        assertTrue(error.message.orEmpty().contains("Unknown progress metric code"))
    }

    private fun distanceRecord() = PersonalRecord(
        FoundationId("distance-record"), FoundationId("exercise"), PersonalRecordKind.TIME,
        null, null, 5000.0, FoundationId("workout"), FoundationId("set"),
        instant(1_000), instant(1_000), ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode,
        ProgressDerivationVersions.CONFIGURATION_CAPTURE
    )

    private fun distancePoint() = ProgressPoint(
        FoundationId("distance-point"), FoundationId("exercise"), FoundationId("workout"),
        FoundationId("set"), ProgressMetric.TIME, 5000.0, null, null, instant(1_000),
        ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode, ProgressDerivationVersions.CONFIGURATION_CAPTURE
    )
}
