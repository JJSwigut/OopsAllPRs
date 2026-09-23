package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.CompletedWorkoutCorrectionUseCase
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlCompletedWorkoutCorrectionTest {
    @Test
    fun correctionPersistsAcrossReloadExportAndBackupRestoreAndRebuildsPr() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val repos = source.repositories()
        seedBackupExercises(repos)
        val older = repos.completedWeightedWorkout(100.0, 1_000, 2_000)
        val newest = repos.completedWeightedWorkout(120.0, 3_000, 4_000)
        val original = repos.workouts.completedWorkout(newest.id)!!
        val originalSet = original.exercises.single().loggedSets.single()
        val added = originalSet.copy(
            id = com.jjswigut.oopsallprs.domain.model.FoundationId("corrected-added-set"),
            position = OrderedPosition(1),
            weight = WeightKg(80.0),
            reps = 10,
            loggedAt = original.finishedAt,
            createdAt = instant(5_000),
            updatedAt = instant(5_000),
            editedAt = null
        )
        val corrected = original.copy(exercises = listOf(original.exercises.single().copy(loggedSets = listOf(
            originalSet.copy(weight = WeightKg(90.0), updatedAt = instant(5_000), editedAt = instant(5_000)),
            added
        ))))

        repos.corrections.save(corrected).successValue()

        val reloaded = source.repositories()
        assertEquals(listOf(WeightKg(90.0), WeightKg(80.0)), reloaded.workouts.completedWorkout(newest.id)!!.exercises.single().loggedSets.map { it.weight })
        assertTrue(reloaded.progress.personalRecords().none { it.sourceWorkoutId == newest.id && it.weight == WeightKg(120.0) })
        assertTrue(reloaded.progress.personalRecords().any { it.sourceWorkoutId == older.id && it.weight == WeightKg(100.0) })
        val export = reloaded.store.export(ExportType.WORKOUTS, WeightUnit.KILOGRAMS).successValue()
        assertTrue("90.0" in export.content)
        assertTrue("120.0" !in export.content)

        val backup = SqlBackupRepository(source.database, reloaded.store).createPackage().successValue()
        val destination = SqlFoundationStoreTestHarness()
        val destinationRepos = destination.repositories()
        SqlBackupRepository(destination.database, destinationRepos.store).restore(backup).successValue()
        assertEquals(listOf(WeightKg(90.0), WeightKg(80.0)), destination.repositories().workouts.completedWorkout(newest.id)!!.exercises.single().loggedSets.map { it.weight })
    }

    @Test
    fun transactionFailureRollsBackSetLedgerAndProgress() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val initial = harness.repositories()
        val completed = initial.completedWeightedWorkout(100.0, 1_000, 2_000)
        val beforeRecords = initial.progress.personalRecords()
        val faultingStore = SqlFoundationStore(harness.database, correctionFaultInjector = { error("simulated correction failure") })
        val faultingWorkouts = SqlWorkoutRepository(faultingStore)
        val useCase = CompletedWorkoutCorrectionUseCase(
            faultingWorkouts,
            faultingWorkouts,
            faultingStore,
            PersonalRecordDerivationUseCase(SqlProgressRepository(faultingStore), faultingStore)
        )
        val original = faultingWorkouts.completedWorkout(completed.id)!!
        val set = original.exercises.single().loggedSets.single()

        val result = useCase.save(original.copy(exercises = listOf(original.exercises.single().copy(
            loggedSets = listOf(set.copy(weight = WeightKg(50.0), updatedAt = instant(3_000), editedAt = instant(3_000)))
        ))))

        assertTrue(result is FoundationResult.Failure)
        val recovered = harness.repositories()
        assertEquals(WeightKg(100.0), recovered.workouts.completedWorkout(completed.id)!!.exercises.single().loggedSets.single().weight)
        assertEquals(beforeRecords.map { it.sourceSetId }.toSet(), recovered.progress.personalRecords().map { it.sourceSetId }.toSet())
    }
}

private suspend fun SqlRepositoryBundle.completedWeightedWorkout(
    weight: Double,
    startedAt: Long,
    finishedAt: Long
) = run {
    val workout = lifecycle.startEmpty(instant(startedAt)).successValue()
    val exercise = setLogging.addExercise(
        workout.id,
        com.jjswigut.oopsallprs.domain.model.ExerciseReference(
            com.jjswigut.oopsallprs.domain.model.FoundationId("exercise-bench"),
            "Bench Press",
            false
        ),
        instant(startedAt + 100)
    ).successValue()
    setLogging.confirmSet(
        workout.id,
        exercise.id,
        com.jjswigut.oopsallprs.domain.model.SetKind.WEIGHTED,
        5,
        WeightKg(weight),
        0,
        instant(startedAt + 200)
    ).successValue()
    routineUseCases.finishWorkout(workout.id, instant(finishedAt)).successValue().workout
}
