package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplateSaveRecoveryTest {
    @Test
    fun successfulSaveUsesReturnedRoutineWithoutPostCommitRefresh() = runTest {
        val harness = FoundationHarness()
        val completedId = completedWorkout(harness)
        val existing = harness.routines.saveCompletedWorkoutAsRoutine(completedId, "Existing", instant(2_500)).successValue()
        var failReads = false
        var reads = 0
        var writes = 0
        val repository = object : RoutineRepository by harness.store {
            override suspend fun routines(): List<ReusableRoutine> {
                reads++
                if (failReads) error("Post-commit list unavailable")
                return harness.store.routines()
            }

            override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
                writes++
                return harness.store.saveRoutine(routine)
            }
        }
        val holder = RoutineStateHolder(RoutineUseCases(harness.store, repository))
        holder.refresh()
        assertEquals(listOf(existing), holder.state.value.routines)
        failReads = true
        holder.beginTemplateSave(completedId)
        holder.updateTemplateName("New template")

        val saved = holder.saveTemplate(instant(3_000)).successValue()

        assertEquals(1, reads, "Saving must not require another repository list read")
        assertEquals(1, writes)
        assertEquals(saved, harness.store.routine(saved.id))
        assertEquals(listOf(saved, existing), holder.state.value.routines)
        assertEquals(listOf(saved.id, existing.id), holder.state.value.templateRows.map { it.templateId })
        assertNull(holder.state.value.saveDraft)
        assertNull(holder.state.value.errorMessage)
        assertEquals(saved.id, holder.state.value.lastSavedTemplateId)
    }

    @Test
    fun thrownSaveExceptionLeavesReadyDraftWithoutInventingConfirmation() = runTest {
        val harness = FoundationHarness()
        val completedId = completedWorkout(harness)
        val repository = object : RoutineRepository by harness.store {
            override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
                harness.store.saveRoutine(routine).successValue()
                error("Lost response after commit")
            }
        }
        val holder = RoutineStateHolder(RoutineUseCases(harness.store, repository))
        holder.beginTemplateSave(completedId)
        holder.updateTemplateName("Uncertain template")

        val result = holder.saveTemplate(instant(3_000))

        val failure = assertIs<FoundationResult.Failure>(result)
        val draft = assertNotNull(holder.state.value.saveDraft)
        assertFalse(draft.isSaving)
        assertEquals(completedId, draft.completedWorkoutId)
        assertEquals("Uncertain template", draft.name)
        assertEquals(failure.error.message, draft.errorMessage)
        assertTrue(failure.error.message.contains("confirm", ignoreCase = true))
        assertTrue(failure.error.message.contains("Check Templates"))
        assertNull(holder.state.value.lastSavedTemplateId)
        assertTrue(holder.state.value.routines.isEmpty())
        assertTrue(holder.state.value.templateRows.isEmpty())
        assertEquals(1, harness.store.routines().size, "A thrown response does not prove the write failed")
        holder.updateTemplateName("Editable again")
        assertEquals("Editable again", holder.state.value.saveDraft?.name)
    }

    @Test
    fun canceledSaveRethrowsAndReleasesBusyDraftWithoutInventingConfirmation() = runTest {
        val harness = FoundationHarness()
        val completedId = completedWorkout(harness)
        val entered = CompletableDeferred<Unit>()
        val repository = object : RoutineRepository by harness.store {
            override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
                harness.store.saveRoutine(routine).successValue()
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        val holder = RoutineStateHolder(RoutineUseCases(harness.store, repository))
        holder.beginTemplateSave(completedId)
        holder.updateTemplateName("Interrupted template")
        var returnedNormally = false
        val saving = async {
            holder.saveTemplate(instant(3_000)).also { returnedNormally = true }
        }
        try {
            entered.await()
            assertTrue(assertNotNull(holder.state.value.saveDraft).isSaving)

            saving.cancelAndJoin()

            assertFailsWith<CancellationException> { saving.await() }
            assertFalse(returnedNormally, "Cancellation must propagate rather than become a returned Failure")
            val draft = assertNotNull(holder.state.value.saveDraft)
            assertFalse(draft.isSaving)
            assertEquals(completedId, draft.completedWorkoutId)
            assertEquals("Interrupted template", draft.name)
            assertTrue(assertNotNull(draft.errorMessage).contains("Check Templates"))
            assertNull(holder.state.value.lastSavedTemplateId)
            assertTrue(holder.state.value.routines.isEmpty())
            assertTrue(holder.state.value.templateRows.isEmpty())
            assertEquals(1, harness.store.routines().size)
            holder.cancelTemplateSave()
            assertNull(holder.state.value.saveDraft)
        } finally {
            saving.cancelAndJoin()
        }
    }

    @Test
    fun beginningAnotherDraftClearsPriorSavedTemplateConfirmation() = runTest {
        val harness = FoundationHarness()
        val completedId = completedWorkout(harness)
        val holder = RoutineStateHolder(harness.routines)
        holder.beginTemplateSave(completedId)
        holder.updateTemplateName("First template")
        val saved = holder.saveTemplate(instant(3_000)).successValue()
        assertEquals(saved.id, holder.state.value.lastSavedTemplateId)

        holder.beginTemplateSave(completedId)

        val draft = assertNotNull(holder.state.value.saveDraft)
        assertEquals(completedId, draft.completedWorkoutId)
        assertEquals("", draft.name)
        assertFalse(draft.isSaving)
        assertNull(draft.errorMessage)
        assertNull(holder.state.value.lastSavedTemplateId)
        assertEquals(listOf(saved), holder.state.value.routines)
        assertEquals(listOf(saved.id), holder.state.value.templateRows.map { it.templateId })
    }

    private suspend fun completedWorkout(harness: FoundationHarness): FoundationId {
        val activeId = harness.workoutWithLoggedWeightedSet()
        return harness.routines.finishWorkout(activeId, instant(2_000)).successValue().workout.id
    }
}
