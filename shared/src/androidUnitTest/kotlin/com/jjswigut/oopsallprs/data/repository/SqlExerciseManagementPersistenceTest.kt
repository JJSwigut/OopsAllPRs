package com.jjswigut.oopsallprs.data.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SqlExerciseManagementPersistenceTest {
    @Test
    fun customExerciseUpdateAndArchiveRecoverAfterRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val created = repos.exerciseCatalog.createCustomExercise("Seal Row", isBodyweight = false, instant(1_000)).successValue()
        repos.exerciseCatalog.updateCustomExercise(created.id, "Chest Supported Row", isBodyweight = false, instant(2_000)).successValue()

        var recovered = harness.repositories()
        val updated = recovered.exerciseCatalog.userCreatedExercises().single()

        assertEquals("Chest Supported Row", updated.displayName)
        recovered.exerciseCatalog.archiveCustomExercise(updated.id, instant(3_000)).successValue()
        recovered = harness.repositories()

        assertFalse(recovered.exerciseCatalog.search("chest").any { it.id == updated.id })
        assertEquals(emptyList(), recovered.exerciseCatalog.userCreatedExercises())
    }
}
