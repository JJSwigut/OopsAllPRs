package com.jjswigut.oopsallprs.testing

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.data.exercise.ExerciseSeedIngestion
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.PreviousWorkoutDefaultsUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import kotlinx.datetime.Instant

const val SAMPLE_CSV: String =
    "Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region\n" +
        "Bench Press,Chest,Barbell,Push,Strength,Beginner,Upper Body\n" +
        "Pull-Up,Back,Bodyweight,Pull,Bodyweight,Intermediate,Upper Body\n" +
        "Plank,Core,Bodyweight,Static Hold,Hypertrophy,Beginner,Upper Body\n" +
        "\"Cable Row, Seated\",Back,Cable,Pull,Strength,Beginner,Upper Body\n"

fun instant(ms: Long): Instant = Instant.fromEpochMilliseconds(ms)

fun <T> FoundationResult<T>.successValue(): T =
    when (this) {
        is FoundationResult.Failure -> error(error.message)
        is FoundationResult.Success -> value
    }

class FoundationHarness {
    val store = InMemoryFoundationStore()
    val previousDefaults = PreviousWorkoutDefaultsUseCase(store)
    val lifecycle = WorkoutLifecycleUseCases(store, store, store, activeUx = store, preferences = store, previousDefaults = previousDefaults)
    val setLogging = SetLoggingUseCases(store, store, store)
    val exerciseCatalog = ExerciseCatalogUseCases(store, store)
    val routines = RoutineUseCases(store, store, store, PersonalRecordDerivationUseCase(store), preferences = store)

    val weightedReference = ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false)
    val bodyweightReference = ExerciseReference(FoundationId("exercise-pullup"), "Pull-Up", isBodyweight = true)
    val timedReference = ExerciseReference(FoundationId("exercise-plank"), "Plank", isBodyweight = true, loggingMode = ExerciseLoggingMode.TIMED)
}

suspend fun FoundationHarness.seedExerciseCatalog() {
    ExerciseSeedIngestion(store).ingest(SAMPLE_CSV)
}

suspend fun FoundationHarness.workoutWithLoggedWeightedSet() = run {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(1_100)).successValue()
    setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, reps = 5, weight = WeightKg(100.0), position = 0, loggedAt = instant(1_200))
    workout.id
}
