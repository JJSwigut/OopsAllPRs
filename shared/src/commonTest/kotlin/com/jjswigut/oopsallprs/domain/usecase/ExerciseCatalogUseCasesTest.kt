package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseCatalogUseCasesTest {
    @Test
    fun blankSearchReturnsDefaultLocalResults() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()

        val results = harness.exerciseCatalog.search("")

        assertEquals(4, results.size)
        assertTrue(results.any { it.displayName == "Bench Press" })
    }

    @Test
    fun searchFindsSeededExercisesByLocalCatalogName() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()

        val results = harness.exerciseCatalog.search("bench")

        assertEquals(listOf("Bench Press"), results.map { it.displayName })
    }
}
