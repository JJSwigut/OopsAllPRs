package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExerciseManagementStateHolderTest {
    @Test
    fun customExerciseCanBeCreatedRenamedSearchedAndArchived() = runTest {
        val harness = FoundationHarness()
        val holder = ExerciseManagementStateHolder(harness.exerciseCatalog)
        holder.open()
        holder.beginCreate()
        holder.updateDraftName("Seal Row")
        holder.updateDraftBodyweight(false)
        val exerciseId = holder.saveDraft(instant(1_000)).successValue()

        assertEquals(listOf("Seal Row"), holder.state.value.rows.map { it.displayName })
        holder.beginEdit(holder.state.value.rows.single())
        holder.updateDraftName("Chest Supported Row")
        holder.updateDraftBodyweight(false)
        holder.saveDraft(instant(2_000)).successValue()
        holder.search("chest")

        assertEquals("Chest Supported Row", holder.state.value.rows.single().displayName)
        assertTrue(harness.exerciseCatalog.search("chest").any { it.id == exerciseId })

        holder.requestArchive(holder.state.value.rows.single())
        holder.confirmArchive(instant(3_000)).successValue()

        assertFalse(harness.exerciseCatalog.search("chest").any { it.id == exerciseId })
        assertTrue(holder.state.value.rows.isEmpty())
    }
}
