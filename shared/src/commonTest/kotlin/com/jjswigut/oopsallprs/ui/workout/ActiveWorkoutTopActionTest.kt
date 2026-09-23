package com.jjswigut.oopsallprs.ui.workout

import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveWorkoutTopActionTest {
    @Test
    fun focusedExerciseReturnsToTheExerciseList() {
        assertEquals(
            ActiveWorkoutTopAction.SHOW_EXERCISES,
            activeWorkoutTopAction(isExerciseOverviewVisible = false, hasExercises = true)
        )
    }

    @Test
    fun overviewAndEmptyWorkoutOfferOnlyWorkoutClose() {
        assertEquals(
            ActiveWorkoutTopAction.CLOSE_WORKOUT,
            activeWorkoutTopAction(isExerciseOverviewVisible = true, hasExercises = true)
        )
        assertEquals(
            ActiveWorkoutTopAction.CLOSE_WORKOUT,
            activeWorkoutTopAction(isExerciseOverviewVisible = false, hasExercises = false)
        )
    }
}
