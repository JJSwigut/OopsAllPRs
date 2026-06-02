package com.jjswigut.oopsallprs.data.exercise

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseSeedTimedClassificationTest {
    @Test
    fun classifiesStaticHoldSeedRowsAsTimed() = runTest {
        val store = InMemoryFoundationStore()
        ExerciseSeedIngestion(store).ingest(SAMPLE_CSV).successValue()

        val plank = store.search("plank").single()

        assertTrue(plank.isBodyweight)
        assertEquals(ExerciseLoggingMode.TIMED, plank.loggingMode)
    }
}
