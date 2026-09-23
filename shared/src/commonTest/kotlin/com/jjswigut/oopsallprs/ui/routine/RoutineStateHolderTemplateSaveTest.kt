package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutineStateHolderTemplateSaveTest {
    @Test
    fun cancelTemplateDraftDoesNotChangeHistoryOrCreateARoutine() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        val holder = RoutineStateHolder(harness.routines)
        holder.beginTemplateSave(completed.id)
        holder.updateTemplateName("Not saved")

        holder.cancelTemplateSave()

        assertNull(holder.state.value.saveDraft)
        assertTrue(harness.store.routines().isEmpty())
        assertEquals(completed, harness.store.completedWorkout(completed.id))
        holder.beginTemplateSave(completed.id)
        assertEquals("", holder.state.value.saveDraft?.name)
    }

    @Test
    fun repeatedSaveAndDraftChangesCannotCreateTwoTemplatesWhileSaving() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        val repository = object : RoutineRepository by harness.store {
            override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
                writes++
                entered.complete(Unit)
                release.await()
                return harness.store.saveRoutine(routine)
            }
        }
        val holder = RoutineStateHolder(RoutineUseCases(harness.store, repository))
        holder.beginTemplateSave(completed.id)
        holder.updateTemplateName("Push")
        val first = async { holder.saveTemplate(instant(3_000)).successValue() }
        entered.await()

        assertTrue(holder.saveTemplate(instant(3_001)) is FoundationResult.Failure)
        holder.cancelTemplateSave()
        holder.updateTemplateName("Changed")
        holder.beginTemplateSave(FoundationId("different-workout"))
        assertEquals("Push", holder.state.value.saveDraft?.name)
        assertEquals(completed.id, holder.state.value.saveDraft?.completedWorkoutId)
        assertTrue(holder.state.value.saveDraft?.isSaving == true)
        release.complete(Unit)
        val saved = first.await()

        assertEquals(1, writes)
        assertEquals(listOf(saved), harness.store.routines())
        assertNull(holder.state.value.saveDraft)
        assertEquals(saved.id, holder.state.value.lastSavedTemplateId)
        assertEquals(completed.id, saved.sourceCompletedWorkoutId)
    }

    @Test
    fun validatesTemplateNameAndSavesCompletedWorkout() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
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
