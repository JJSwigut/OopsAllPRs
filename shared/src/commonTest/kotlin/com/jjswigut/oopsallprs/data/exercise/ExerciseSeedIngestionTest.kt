package com.jjswigut.oopsallprs.data.exercise

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseSeedIngestionTest {
    @Test
    fun acceptsBaselineRowsAndIsIdempotent() = runTest {
        val store = InMemoryFoundationStore()
        val ingestion = ExerciseSeedIngestion(store)
        ingestion.ingest(SAMPLE_CSV)
        val second = ingestion.ingest(SAMPLE_CSV).successValue()
        assertEquals(4, second.rowCount)
        assertEquals(4, store.all().size)
    }
}
