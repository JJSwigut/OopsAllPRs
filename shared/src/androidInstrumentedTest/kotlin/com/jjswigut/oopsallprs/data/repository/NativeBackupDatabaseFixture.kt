package com.jjswigut.oopsallprs.data.repository

import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class NativeBackupDatabaseFixture : Closeable {
    // Deliberately use the instrumentation APK context, never the installed app context/factory.
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val databaseName = "backup-contract-${UUID.randomUUID()}.db"
    private val drivers = mutableListOf<AndroidSqliteDriver>()
    private var workerStopped = true

    init {
        check(context.packageName == "com.jjswigut.oopsallprs.backupcontract.test")
        check(!context.getDatabasePath(databaseName).exists())
    }

    val primary by lazy { openConnection() }

    fun openConnection(): NativeBackupConnection {
        val driver = AndroidSqliteDriver(WorkoutDatabase.Schema, context, databaseName)
        drivers += driver
        return NativeBackupConnection(WorkoutDatabase(driver))
    }

    fun <T> onWorker(block: suspend () -> T): T {
        val executor = Executors.newSingleThreadExecutor { task ->
            Thread(task, "backup-contract-writer").apply { isDaemon = true }
        }
        workerStopped = false
        try {
            return executor.submit(Callable { runBlocking { block() } }).get(10, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
            workerStopped = executor.awaitTermination(5, TimeUnit.SECONDS)
            check(workerStopped) { "Database worker did not stop; leaving its test database intact." }
        }
    }

    override fun close() {
        check(workerStopped) { "Refusing to close/delete a database still used by a test worker." }
        // Never delete while any connection remains open, including a failed close.
        drivers.asReversed().forEach { it.close() }
        drivers.clear()
        check(context.deleteDatabase(databaseName)) { "Could not delete owned test database $databaseName" }
    }
}

internal class NativeBackupConnection(val database: WorkoutDatabase) {
    val store = SqlFoundationStore(database)
    val backup = SqlBackupRepository(database, store)
    val sync = SqlBackupSyncRepository(database)

    suspend fun seedRecoverableContent() {
        listOf(exercise("bench", "Bench Press", false), exercise("pullup", "Pull-Up", true)).forEach {
            store.saveUserExercise(it).nativeSuccess()
        }
        store.setStartWorkoutTimerWithFirstSet(false).nativeSuccess()
        store.setRestTimerSurfaceEnabled(false).nativeSuccess()
        store.setWeightUnit(WeightUnit.KILOGRAMS).nativeSuccess()
        val lifecycle = WorkoutLifecycleUseCases(store, store, store, store, store)
        val logging = SetLoggingUseCases(store, store, store)
        val routines = RoutineUseCases(store, store, store, PersonalRecordDerivationUseCase(store), store)
        val workout = lifecycle.startEmpty(nativeInstant(1_000)).nativeSuccess()
        val weighted = logging.addExercise(
            workout.id, ExerciseReference(FoundationId("bench"), "Bench Press", isBodyweight = false), nativeInstant(1_100)
        ).nativeSuccess()
        val bodyweight = logging.addExercise(
            workout.id, ExerciseReference(FoundationId("pullup"), "Pull-Up", isBodyweight = true), nativeInstant(1_150)
        ).nativeSuccess()
        logging.groupExercisesAsCircuit(workout.id, listOf(weighted.id, bodyweight.id), nativeInstant(1_175)).nativeSuccess()
        logging.confirmSet(workout.id, weighted.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, nativeInstant(1_200)).nativeSuccess()
        logging.confirmSet(workout.id, bodyweight.id, SetKind.BODYWEIGHT, 12, null, 0, nativeInstant(1_250)).nativeSuccess()
        val completed = routines.finishWorkout(workout.id, nativeInstant(2_000)).nativeSuccess().workout
        routines.saveCompletedWorkoutAsRoutine(completed.id, "Native circuit", nativeInstant(2_100)).nativeSuccess()
        val active = lifecycle.startEmpty(nativeInstant(3_000)).nativeSuccess()
        val activeExercise = logging.addExercise(
            active.id, ExerciseReference(FoundationId("bench"), "Bench Press", isBodyweight = false), nativeInstant(3_100)
        ).nativeSuccess()
        val draft = PersistedSetDraft(
            draftId = FoundationId("native-draft"), activeWorkoutId = active.id,
            exerciseInstanceId = activeExercise.id, position = OrderedPosition(0),
            setKind = SetKind.WEIGHTED, reps = 6, weight = WeightKg(90.0), updatedAt = nativeInstant(3_200)
        )
        store.saveSetDraft(draft).nativeSuccess()
        store.saveUxSession(ActiveWorkoutUxSession(active.id, activeExercise.id, draft.draftId, nativeInstant(3_200))).nativeSuccess()
        store.export(ExportType.WORKOUTS, WeightUnit.KILOGRAMS).nativeSuccess()
    }

    private fun exercise(id: String, name: String, bodyweight: Boolean) = ExerciseCatalogItem(
        id = FoundationId(id),
        canonicalName = name.lowercase(),
        displayName = name,
        muscleGroup = if (bodyweight) "Back" else "Chest",
        equipment = if (bodyweight) "Bodyweight" else "Barbell",
        movementPattern = if (bodyweight) "Pull" else "Push",
        exerciseType = if (bodyweight) "Bodyweight" else "Strength",
        experienceLevel = "Intermediate",
        bodyRegion = "Upper Body",
        isBodyweight = bodyweight,
        isUserCreated = true,
        createdAt = nativeInstant(100),
        updatedAt = nativeInstant(100)
    )
}

internal fun nativeInstant(millis: Long): Instant = Instant.fromEpochMilliseconds(millis)

internal fun <T> FoundationResult<T>.nativeSuccess(): T = when (this) {
    is FoundationResult.Success -> value
    is FoundationResult.Failure -> error(error.message)
}
