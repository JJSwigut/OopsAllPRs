package com.jjswigut.oopsallprs.ui.routine

import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.exercise.ExercisePickerResultRow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoutineTimedEditorTest {
    @Test
    fun timedExerciseStartsWithBlankTimedSetAndCanSaveTargetDuration() = runTest {
        val harness = FoundationHarness()
        val holder = RoutineStateHolder(harness.routines, harness.exerciseCatalog)
        holder.beginCreateRoutine()
        holder.updateEditorName("Core")
        holder.addEditorExercise(timedRow())
        val exerciseDraftId = holder.state.value.editorDraft!!.exercises.single().draftId
        val setDraftId = holder.state.value.editorDraft!!.exercises.single().plannedSets.single().draftId

        val draftSet = holder.state.value.editorDraft!!.exercises.single().plannedSets.single()
        assertEquals(SetKind.TIMED, draftSet.setKind)
        assertNull(draftSet.targetReps)
        assertNull(draftSet.targetWeight)
        assertNull(draftSet.targetDurationMs)

        holder.updateEditorSetDuration(exerciseDraftId, setDraftId, 45_000L)
        val routine = holder.saveEditor(instant(2_000)).successValue()

        assertEquals(45_000L, routine.exercises.single().plannedSets.single().targetDurationMs)
        assertNull(routine.exercises.single().plannedSets.single().targetReps)
        assertNull(routine.exercises.single().plannedSets.single().targetWeight)
    }

    private fun timedRow(): ExercisePickerResultRow =
        ExercisePickerResultRow(
            exerciseCatalogId = FoundationId("exercise-plank"),
            displayName = "Plank",
            subtitle = "Core",
            isBodyweight = true,
            loggingMode = ExerciseLoggingMode.TIMED,
            isUserCreated = false
        )
}
