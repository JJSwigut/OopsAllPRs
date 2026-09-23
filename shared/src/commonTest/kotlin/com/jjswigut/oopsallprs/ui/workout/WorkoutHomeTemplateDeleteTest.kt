package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutHomeTemplateDeleteTest {
    @Test
    fun confirmingTemplateDeleteRemovesTemplateFromTrainList() = runTest {
        val harness = FoundationHarness()
        val completed = harness.completedWorkoutForTemplate()
        harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Bench day", instant(2_500)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle, harness.routines)

        holder.hydrate()
        val templateId = holder.state.value.templates.single().templateId
        holder.requestTemplateDelete(templateId)
        holder.confirmTemplateDelete().successValue()

        assertTrue(holder.state.value.templates.isEmpty())
        assertNull(holder.state.value.pendingDeleteTemplate)
    }
}

private suspend fun FoundationHarness.completedWorkoutForTemplate() = run {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(1_100)).successValue()
    setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200))
    routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
}
