package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlRestTimerPersistenceTest {
    @Test
    fun activeExerciseRestPreferencesAndSessionRecoverAfterRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        repos.store.setDefaultRestSeconds(180).successValue()
        repos.store.setRestSoundEnabled(false).successValue()
        repos.store.setRestTimerSurfaceEnabled(true).successValue()
        val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = repos.setLogging.addExercise(
            workout.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
            instant(1_100)
        ).successValue()
        repos.setLogging.updateExerciseRest(
            workout.id,
            exercise.id,
            RestConfiguration(durationSeconds = 75, autoStart = false),
            instant(1_200)
        ).successValue()
        repos.lifecycle.startRestTimer(workout.id, FoundationId("set-origin"), durationSeconds = 90, now = instant(1_300)).successValue()

        val recovered = harness.repositories()
        val active = assertNotNull(recovered.lifecycle.currentActiveWorkout())
        val session = assertNotNull(recovered.store.load())

        assertEquals(RestConfiguration(durationSeconds = 75, autoStart = false), active.exercises.single().rest)
        assertEquals(instant(91_300), session.restEndsAt)
        assertEquals(180, recovered.store.defaultRestSeconds())
        assertFalse(recovered.store.restSoundEnabled())
        assertTrue(recovered.store.restTimerSurfaceEnabled())
    }
}
