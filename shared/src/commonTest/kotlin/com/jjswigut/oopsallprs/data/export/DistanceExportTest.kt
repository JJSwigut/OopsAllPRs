package com.jjswigut.oopsallprs.data.export

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressDerivationVersions
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceExportTest {
    @Test
    fun inMemoryDistanceMetricNeverUsesLegacyTimeFormatting() = runTest {
        val store = InMemoryFoundationStore()
        store.replaceRecords(
            listOf(
                PersonalRecord(
                    FoundationId("distance"), FoundationId("exercise"), PersonalRecordKind.TIME,
                    null, null, 5000.0, FoundationId("workout"), FoundationId("set"),
                    instant(1_000), instant(1_000), ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode,
                    ProgressDerivationVersions.CONFIGURATION_CAPTURE
                )
            ),
            emptyList()
        ).successValue()

        val export = store.export(ExportType.PERSONAL_RECORDS, WeightUnit.POUNDS).successValue()
        val row = export.content.lineSequence().drop(1).single()

        assertEquals(EXPORT_FORMAT_VERSION, export.snapshot.formatVersion)
        assertTrue(row.contains(",5000.0 m,"))
        assertTrue(!row.contains("1:23:20"))
    }
}
