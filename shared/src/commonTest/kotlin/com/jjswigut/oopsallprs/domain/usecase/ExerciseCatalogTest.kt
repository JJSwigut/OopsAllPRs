package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.exercise.ExerciseSeedIngestion
import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseCatalogTest {
    @Test
    fun searchFindsSeededBodyweightExercises() = runTest {
        val store = InMemoryFoundationStore()
        ExerciseSeedIngestion(store).ingest(SAMPLE_CSV)
        val results = store.search("pull")
        assertEquals(1, results.size)
        assertTrue(results.single().isBodyweight)
    }
}
