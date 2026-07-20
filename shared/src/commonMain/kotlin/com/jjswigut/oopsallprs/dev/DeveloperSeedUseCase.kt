package com.jjswigut.oopsallprs.dev

import com.jjswigut.oopsallprs.data.exercise.defaultExerciseSeedCsv
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.canonicalExerciseName
import com.jjswigut.oopsallprs.domain.usecase.ExerciseCatalogUseCases
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class DeveloperSeedUseCase(
    private val exerciseCatalog: ExerciseCatalogUseCases,
    private val workoutLifecycle: WorkoutLifecycleUseCases,
    private val setLogging: SetLoggingUseCases,
    private val routines: RoutineUseCases,
    private val workouts: WorkoutRepository,
    private val progress: ProgressRepository,
    private val seedCsvProvider: suspend () -> String = { defaultExerciseSeedCsv() },
    private val clock: () -> Instant = { Clock.System.now() }
) {
    suspend fun load(scenario: DeveloperSeedScenario): DeveloperSeedResult =
        when (scenario) {
            DeveloperSeedScenario.PROGRESS -> loadProgressDemo()
            DeveloperSeedScenario.ROUTINES -> loadRoutineDemo()
            DeveloperSeedScenario.ACTIVE_RECOVERY -> loadActiveRecoveryDemo()
        }

    suspend fun loadProgressDemo(): DeveloperSeedResult =
        runScenario(DeveloperSeedScenario.PROGRESS) {
            val exercises = resolveExercises()
            val plans = progressWorkoutPlans()
            val existingFinishedAt = workouts.completedWorkouts()
                .map { it.finishedAt }
                .toSet()
            val missingPlans = plans.filterNot { it.finishedAt in existingFinishedAt }
            if (missingPlans.isEmpty()) {
                return@runScenario skipped("Progress demo already loaded")
            }

            val progressRoutines = ensureProgressRoutines(exercises)
            missingPlans.forEach { plan ->
                val routine = progressRoutines.getValue(plan.routineName)
                val active = workoutLifecycle.startFromRoutine(routine.id, plan.startedAt).successValue()
                plan.sets.forEachIndexed { index, set ->
                    val activeExercise = active.exercises.first { exercise ->
                        exercise.reference.displayNameSnapshot == set.exercise.resolve(exercises).displayName
                    }
                    setLogging.confirmSet(
                        activeWorkoutId = active.id,
                        exerciseInstanceId = activeExercise.id,
                        setKind = set.kind,
                        reps = set.reps,
                        weight = set.weightKg?.let(::WeightKg),
                        position = index,
                        durationMs = set.durationMs,
                        loggedAt = Instant.fromEpochMilliseconds(plan.startedAt.toEpochMilliseconds() + (index + 1) * MINUTE_MS)
                    ).successValue()
                }
                routines.finishWorkout(active.id, plan.finishedAt).successValue()
            }

            val records = progress.personalRecords().size
            loaded("Loaded ${missingPlans.size} demo workouts and $records PR records")
        }

    suspend fun loadRoutineDemo(): DeveloperSeedResult =
        runScenario(DeveloperSeedScenario.ROUTINES) {
            val exercises = resolveExercises()
            val existingNames = routines.listRoutines().map { it.name }.toSet()
            val routinePlans = routineDemoPlans()
            val missing = routinePlans.filterNot { it.name in existingNames }
            if (missing.isEmpty()) {
                return@runScenario skipped("Routines demo already loaded")
            }
            missing.forEach { plan -> saveRoutine(plan, exercises) }
            loaded("Loaded ${missing.size} demo routines")
        }

    suspend fun loadActiveRecoveryDemo(): DeveloperSeedResult =
        runScenario(DeveloperSeedScenario.ACTIVE_RECOVERY) {
            val current = workoutLifecycle.currentActiveWorkout()
            if (current != null) {
                return@runScenario if (current.routineSnapshotName == ACTIVE_RECOVERY_ROUTINE_NAME) {
                    skipped("Active recovery demo already loaded")
                } else {
                    failed("An active workout is already in progress")
                }
            }

            val exercises = resolveExercises()
            val routine = ensureRoutine(activeRecoveryRoutinePlan(), exercises)
            val startedAt = Instant.fromEpochMilliseconds(clock().toEpochMilliseconds() - ACTIVE_RECOVERY_ELAPSED_MS)
            val active = workoutLifecycle.startFromRoutine(routine.id, startedAt).successValue()
            val firstExercise = active.exercises.first()
            setLogging.confirmSet(
                activeWorkoutId = active.id,
                exerciseInstanceId = firstExercise.id,
                setKind = SetKind.WEIGHTED,
                reps = 5,
                weight = WeightKg(87.5),
                position = 0,
                loggedAt = Instant.fromEpochMilliseconds(startedAt.toEpochMilliseconds() + MINUTE_MS)
            ).successValue()
            loaded("Loaded active recovery workout")
        }

    private suspend fun resolveExercises(): DemoExercises {
        exerciseCatalog.ensureSeeded(seedCsvProvider()).successValue()
        return DemoExercises(
            bench = resolveExercise("Barbell Bench Press - Medium Grip"),
            squat = resolveExercise("Barbell Squat"),
            deadlift = resolveExercise("Barbell Deadlift"),
            pullUp = resolveExercise("Wide-Grip Rear Pull-Up"),
            plank = resolveExercise("Plank")
        )
    }

    private suspend fun resolveExercise(name: String): ExerciseCatalogItem =
        exerciseCatalog.search(name)
            .firstOrNull { it.canonicalName == canonicalExerciseName(name) }
            ?: throw SeedException("Required seed exercise missing: $name")

    private suspend fun ensureProgressRoutines(exercises: DemoExercises): Map<String, ReusableRoutine> =
        listOf(progressPushRoutinePlan(), progressLowerRoutinePlan())
            .associate { plan -> plan.name to ensureRoutine(plan, exercises) }

    private suspend fun ensureRoutine(
        plan: DemoRoutinePlan,
        exercises: DemoExercises
    ): ReusableRoutine =
        routines.listRoutines().firstOrNull { it.name == plan.name }
            ?: saveRoutine(plan, exercises)

    private suspend fun saveRoutine(
        plan: DemoRoutinePlan,
        exercises: DemoExercises
    ): ReusableRoutine =
        routines.saveRoutine(
            routineId = null,
            name = plan.name,
            exercises = plan.exercises.mapIndexed { exerciseIndex, exercise ->
                val routineExerciseId = FoundationId("${plan.slug}-exercise-$exerciseIndex")
                RoutineExercise(
                    id = routineExerciseId,
                    routineId = FoundationId("${plan.slug}-draft"),
                    exerciseCatalogId = exercise.exercise.resolve(exercises).id,
                    displayNameSnapshot = exercise.exercise.resolve(exercises).displayName,
                    position = OrderedPosition(exerciseIndex),
                    rest = RestConfiguration(durationSeconds = exercise.restSeconds),
                    plannedSets = exercise.sets.mapIndexed { setIndex, set ->
                        RoutineSetTemplate(
                            id = FoundationId("${plan.slug}-set-$exerciseIndex-$setIndex"),
                            routineExerciseId = routineExerciseId,
                            position = OrderedPosition(setIndex),
                            targetWeight = set.weightKg?.let(::WeightKg),
                            targetReps = set.reps.takeIf { set.kind != SetKind.TIMED },
                            targetDurationMs = set.durationMs.takeIf { set.kind == SetKind.TIMED },
                            setKind = set.kind
                        )
                    }
                )
            },
            now = DEMO_CREATED_AT
        ).successValue()

    private fun progressPushRoutinePlan(): DemoRoutinePlan =
        DemoRoutinePlan(
            name = PROGRESS_PUSH_ROUTINE_NAME,
            slug = "demo-progress-push",
            exercises = listOf(
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.BENCH,
                    restSeconds = 180,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 80.0),
                        DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 8, weightKg = 72.5)
                    )
                ),
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.PULL_UP,
                    restSeconds = 120,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 8, weightKg = null),
                        DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 6, weightKg = null)
                    )
                ),
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.PLANK,
                    restSeconds = 60,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.PLANK, SetKind.TIMED, reps = 0, weightKg = null, durationMs = 45_000L)
                    )
                )
            )
        )

    private fun progressLowerRoutinePlan(): DemoRoutinePlan =
        DemoRoutinePlan(
            name = PROGRESS_LOWER_ROUTINE_NAME,
            slug = "demo-progress-lower",
            exercises = listOf(
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.SQUAT,
                    restSeconds = 240,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.SQUAT, SetKind.WEIGHTED, reps = 5, weightKg = 100.0),
                        DemoSetPlan(DemoExercise.SQUAT, SetKind.WEIGHTED, reps = 5, weightKg = 95.0)
                    )
                ),
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.DEADLIFT,
                    restSeconds = 300,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.DEADLIFT, SetKind.WEIGHTED, reps = 3, weightKg = 125.0)
                    )
                )
            )
        )

    private fun routineDemoPlans(): List<DemoRoutinePlan> =
        listOf(
            DemoRoutinePlan(
                name = "Demo: Upper Strength",
                slug = "demo-upper-strength",
                exercises = listOf(
                    DemoRoutineExercisePlan(
                        exercise = DemoExercise.BENCH,
                        restSeconds = 180,
                        sets = listOf(
                            DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 82.5),
                            DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 82.5),
                            DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 82.5)
                        )
                    ),
                    DemoRoutineExercisePlan(
                        exercise = DemoExercise.PULL_UP,
                        restSeconds = 120,
                        sets = listOf(
                            DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 8, weightKg = null),
                            DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 8, weightKg = null)
                        )
                    ),
                    DemoRoutineExercisePlan(
                        exercise = DemoExercise.PLANK,
                        restSeconds = 60,
                        sets = listOf(
                            DemoSetPlan(DemoExercise.PLANK, SetKind.TIMED, reps = 0, weightKg = null, durationMs = 60_000L)
                        )
                    )
                )
            ),
            DemoRoutinePlan(
                name = "Demo: Lower Strength",
                slug = "demo-lower-strength",
                exercises = listOf(
                    DemoRoutineExercisePlan(
                        exercise = DemoExercise.SQUAT,
                        restSeconds = 240,
                        sets = listOf(
                            DemoSetPlan(DemoExercise.SQUAT, SetKind.WEIGHTED, reps = 5, weightKg = 105.0),
                            DemoSetPlan(DemoExercise.SQUAT, SetKind.WEIGHTED, reps = 5, weightKg = 105.0)
                        )
                    ),
                    DemoRoutineExercisePlan(
                        exercise = DemoExercise.DEADLIFT,
                        restSeconds = 300,
                        sets = listOf(
                            DemoSetPlan(DemoExercise.DEADLIFT, SetKind.WEIGHTED, reps = 3, weightKg = 130.0)
                        )
                    )
                )
            )
        )

    private fun activeRecoveryRoutinePlan(): DemoRoutinePlan =
        DemoRoutinePlan(
            name = ACTIVE_RECOVERY_ROUTINE_NAME,
            slug = "demo-active-recovery",
            exercises = listOf(
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.BENCH,
                    restSeconds = 180,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 87.5),
                        DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 87.5)
                    )
                ),
                DemoRoutineExercisePlan(
                    exercise = DemoExercise.PULL_UP,
                    restSeconds = 120,
                    sets = listOf(
                        DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 9, weightKg = null)
                    )
                )
            )
        )

    private fun progressWorkoutPlans(): List<DemoWorkoutPlan> =
        listOf(
            DemoWorkoutPlan(
                routineName = PROGRESS_PUSH_ROUTINE_NAME,
                startedAt = day(0),
                finishedAt = day(0, minutes = 48),
                sets = listOf(
                    DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 80.0),
                    DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 8, weightKg = 72.5),
                    DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 6, weightKg = null),
                    DemoSetPlan(DemoExercise.PLANK, SetKind.TIMED, reps = 0, weightKg = null, durationMs = 45_000L)
                )
            ),
            DemoWorkoutPlan(
                routineName = PROGRESS_LOWER_ROUTINE_NAME,
                startedAt = day(3),
                finishedAt = day(3, minutes = 52),
                sets = listOf(
                    DemoSetPlan(DemoExercise.SQUAT, SetKind.WEIGHTED, reps = 5, weightKg = 100.0),
                    DemoSetPlan(DemoExercise.DEADLIFT, SetKind.WEIGHTED, reps = 3, weightKg = 125.0)
                )
            ),
            DemoWorkoutPlan(
                routineName = PROGRESS_PUSH_ROUTINE_NAME,
                startedAt = day(7),
                finishedAt = day(7, minutes = 50),
                sets = listOf(
                    DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 82.5),
                    DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 8, weightKg = null),
                    DemoSetPlan(DemoExercise.PLANK, SetKind.TIMED, reps = 0, weightKg = null, durationMs = 60_000L)
                )
            ),
            DemoWorkoutPlan(
                routineName = PROGRESS_LOWER_ROUTINE_NAME,
                startedAt = day(10),
                finishedAt = day(10, minutes = 55),
                sets = listOf(
                    DemoSetPlan(DemoExercise.SQUAT, SetKind.WEIGHTED, reps = 5, weightKg = 105.0),
                    DemoSetPlan(DemoExercise.DEADLIFT, SetKind.WEIGHTED, reps = 3, weightKg = 132.5)
                )
            ),
            DemoWorkoutPlan(
                routineName = PROGRESS_PUSH_ROUTINE_NAME,
                startedAt = day(14),
                finishedAt = day(14, minutes = 47),
                sets = listOf(
                    DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 5, weightKg = 87.5),
                    DemoSetPlan(DemoExercise.BENCH, SetKind.WEIGHTED, reps = 3, weightKg = 90.0),
                    DemoSetPlan(DemoExercise.PULL_UP, SetKind.BODYWEIGHT, reps = 10, weightKg = null),
                    DemoSetPlan(DemoExercise.PLANK, SetKind.TIMED, reps = 0, weightKg = null, durationMs = 75_000L)
                )
            )
        )

    private fun day(offset: Int, minutes: Int = 0): Instant =
        Instant.fromEpochMilliseconds(DEMO_BASE_MS + offset * DAY_MS + minutes * MINUTE_MS)

    private suspend fun runScenario(
        scenario: DeveloperSeedScenario,
        block: suspend ScenarioScope.() -> DeveloperSeedResult
    ): DeveloperSeedResult =
        try {
            ScenarioScope(scenario).block()
        } catch (failure: SeedException) {
            DeveloperSeedResult(scenario, DeveloperSeedOutcome.FAILED, failure.message ?: "Developer seed failed")
        }

    private fun <T> FoundationResult<T>.successValue(): T =
        when (this) {
            is FoundationResult.Failure -> throw SeedException(error.message)
            is FoundationResult.Success -> value
        }

    private inner class ScenarioScope(val currentScenario: DeveloperSeedScenario) {
        fun loaded(message: String): DeveloperSeedResult =
            DeveloperSeedResult(currentScenario, DeveloperSeedOutcome.LOADED, message)

        fun skipped(message: String): DeveloperSeedResult =
            DeveloperSeedResult(currentScenario, DeveloperSeedOutcome.SKIPPED, message)

        fun failed(message: String): DeveloperSeedResult =
            DeveloperSeedResult(currentScenario, DeveloperSeedOutcome.FAILED, message)
    }

    private data class DemoExercises(
        val bench: ExerciseCatalogItem,
        val squat: ExerciseCatalogItem,
        val deadlift: ExerciseCatalogItem,
        val pullUp: ExerciseCatalogItem,
        val plank: ExerciseCatalogItem
    )

    private enum class DemoExercise {
        BENCH,
        SQUAT,
        DEADLIFT,
        PULL_UP,
        PLANK;

        fun resolve(exercises: DemoExercises): ExerciseCatalogItem =
            when (this) {
                BENCH -> exercises.bench
                SQUAT -> exercises.squat
                DEADLIFT -> exercises.deadlift
                PULL_UP -> exercises.pullUp
                PLANK -> exercises.plank
            }
    }

    private data class DemoSetPlan(
        val exercise: DemoExercise,
        val kind: SetKind,
        val reps: Int,
        val weightKg: Double?,
        val durationMs: Long? = null
    )

    private data class DemoRoutineExercisePlan(
        val exercise: DemoExercise,
        val restSeconds: Int,
        val sets: List<DemoSetPlan>
    )

    private data class DemoRoutinePlan(
        val name: String,
        val slug: String,
        val exercises: List<DemoRoutineExercisePlan>
    )

    private data class DemoWorkoutPlan(
        val routineName: String,
        val startedAt: Instant,
        val finishedAt: Instant,
        val sets: List<DemoSetPlan>
    )

    private class SeedException(message: String) : RuntimeException(message)

    private companion object {
        const val PROGRESS_PUSH_ROUTINE_NAME = "Demo: Progress Push"
        const val PROGRESS_LOWER_ROUTINE_NAME = "Demo: Progress Lower"
        const val ACTIVE_RECOVERY_ROUTINE_NAME = "Demo: Active Recovery"
        const val DEMO_BASE_MS = 1_714_521_600_000L
        const val DAY_MS = 86_400_000L
        const val MINUTE_MS = 60_000L
        const val ACTIVE_RECOVERY_ELAPSED_MS = 25 * MINUTE_MS
        val DEMO_CREATED_AT: Instant = Instant.fromEpochMilliseconds(DEMO_BASE_MS)
    }
}
