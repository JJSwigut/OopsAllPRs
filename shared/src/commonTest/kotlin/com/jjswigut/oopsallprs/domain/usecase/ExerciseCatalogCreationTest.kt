package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseCatalogCreationTest {
    @Test
    fun createCustomExercisePersistsSearchableLocalItem() = runTest {
        val harness = FoundationHarness()

        val item = harness.exerciseCatalog.createCustomExercise(
            name = "Sled Push",
            isBodyweight = false,
            now = instant(1_000)
        ).successValue()

        assertEquals("Sled Push", item.displayName)
        assertTrue(item.isUserCreated)
        assertEquals(listOf("Sled Push"), harness.exerciseCatalog.search("sled").map { it.displayName })
    }

    @Test
    fun createCustomExerciseRejectsBlankName() = runTest {
        val harness = FoundationHarness()

        val result = harness.exerciseCatalog.createCustomExercise("   ", isBodyweight = false)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(0, harness.exerciseCatalog.defaultResults().size)
    }
}
