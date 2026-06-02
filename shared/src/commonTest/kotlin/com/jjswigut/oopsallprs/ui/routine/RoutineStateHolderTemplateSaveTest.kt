package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutineStateHolderTemplateSaveTest {
    @Test
    fun validatesTemplateNameAndSavesCompletedWorkout() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
        val holder = RoutineStateHolder(harness.routines)

        holder.beginTemplateSave(completed.id)
        val blank = holder.saveTemplate(instant(3_000))
        assertTrue(blank is FoundationResult.Failure)
        assertEquals("Template name is required", holder.state.value.saveDraft?.errorMessage)

        holder.updateTemplateName(" Push ")
        val saved = holder.saveTemplate(instant(4_000)).successValue()

        assertEquals("Push", saved.name)
        assertEquals(saved.id, holder.state.value.lastSavedTemplateId)
        assertTrue(holder.state.value.templateRows.any { it.templateId == saved.id })
    }
}
