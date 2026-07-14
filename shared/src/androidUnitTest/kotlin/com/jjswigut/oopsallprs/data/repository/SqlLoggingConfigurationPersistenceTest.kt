package com.jjswigut.oopsallprs.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.data.LoggingConfigurationIdentity
import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.EffortTarget
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionOrigin
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlLoggingConfigurationPersistenceTest {
    @Test
    fun freshDatabaseSeedsLegacyConfigurationsAndRoundTripsOrderedContent() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val store = harness.repositories().store

        assertEquals(LegacyLoggingConfigurations.all.toSet(), store.loggingConfigurations().toSet())
        assertEquals(customConfiguration, store.saveLoggingConfiguration(customConfiguration).successValue())
        assertEquals(customConfiguration, store.loggingConfiguration(customConfiguration.id))
        assertEquals(
            LoggingConfigurationIdentity.contentHash(customConfiguration),
            harness.database.loggingConfigurationQueriesQueries
                .selectLoggingConfiguration(customConfiguration.id.value)
                .executeAsOne()
                .content_hash
        )

        val recovered = harness.repositories().store
        assertEquals(customConfiguration, recovered.loggingConfiguration(customConfiguration.id))
    }

    @Test
    fun semanticDuplicateReusesCanonicalIdAndImmutableIdConflictIsControlled() = runTest {
        val store = SqlFoundationStoreTestHarness().repositories().store
        store.saveLoggingConfiguration(customConfiguration).successValue()

        val duplicate = customConfiguration.copy(id = LoggingConfigurationId("duplicate-content"))
        assertEquals(customConfiguration.id, store.saveLoggingConfiguration(duplicate).successValue().id)

        val conflict = customConfiguration.copy(
            measures = listOf(MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED))
        )
        val result = store.saveLoggingConfiguration(conflict)
        assertIs<FoundationResult.Failure>(result)
        assertIs<FoundationError.Conflict>(result.error)
        assertEquals(customConfiguration, store.loggingConfiguration(customConfiguration.id))
    }

    @Test
    fun activeSetAndDraftRoundTripDistanceEffortAndConfigurationAcrossProcessRecovery() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        var store = harness.repositories().store
        store.saveLoggingConfiguration(customConfiguration).successValue()
        val workoutId = FoundationId("workout-config")
        val exerciseId = FoundationId("active-config")
        val effort = Effort(rpeTenths = 85, rir = 2, failureOutcome = FailureOutcome.NOT_REACHED)
        val set = ExerciseSet(
            id = FoundationId("set-config"),
            exerciseInstanceId = exerciseId,
            position = OrderedPosition(0),
            setKind = SetKind.BODYWEIGHT,
            weight = WeightKg(12.5),
            reps = 8,
            loggedAt = instant(2_000),
            createdAt = instant(1_000),
            updatedAt = instant(2_000),
            captureConfigurationId = customConfiguration.id,
            distanceMeters = 20.0,
            observedEffort = effort
        )
        val exercise = ActiveExercise(
            id = exerciseId,
            activeWorkoutId = workoutId,
            reference = ExerciseReference(
                exerciseCatalogId = FoundationId("catalog-config"),
                displayNameSnapshot = "Weighted Carry",
                isBodyweight = true,
                loggingMode = ExerciseLoggingMode.BODYWEIGHT,
                resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                    customConfiguration,
                    LoggingConfigurationSource.WORKOUT_OVERRIDE
                )
            ),
            position = OrderedPosition(0),
            sets = listOf(set)
        )
        store.createActiveWorkout(
            ActiveWorkout(
                id = workoutId,
                startedAt = instant(1_000),
                exercises = listOf(exercise),
                createdAt = instant(1_000),
                updatedAt = instant(2_000)
            )
        ).successValue()
        val draft = PersistedSetDraft(
            draftId = FoundationId("draft-config"),
            activeWorkoutId = workoutId,
            exerciseInstanceId = exerciseId,
            position = OrderedPosition(1),
            setKind = SetKind.BODYWEIGHT,
            reps = 9,
            weight = WeightKg(15.0),
            updatedAt = instant(2_500),
            captureConfigurationId = customConfiguration.id,
            distanceMeters = 25.0,
            observedEffort = effort
        )
        store.saveSetDraft(draft).successValue()

        store = harness.repositories().store
        val recoveredExercise = assertNotNull(store.currentActiveWorkout()).exercises.single()
        assertEquals(customConfiguration, recoveredExercise.resolvedLoggingConfiguration.configuration)
        assertEquals(set, recoveredExercise.sets.single())
        assertEquals(listOf(draft), store.loadSetDrafts(workoutId))
    }

    @Test
    fun routineTargetsCatalogMetadataAndUserDefaultRecover() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        var store = harness.repositories().store
        val exercise = customExercise(customConfiguration)
        store.saveUserExercise(exercise).successValue()
        val configuredAt = instant(3_000)
        val userDefault = UserExerciseConfiguration(
            exerciseDefinitionId = exercise.id,
            configuration = customConfiguration,
            basedOnDefinitionRevision = exercise.definitionRevision,
            configuredAt = configuredAt
        )
        store.saveUserExerciseConfiguration(userDefault).successValue()
        val routine = ReusableRoutine(
            id = FoundationId("routine-config"),
            name = "Configured routine",
            exercises = listOf(
                RoutineExercise(
                    id = FoundationId("routine-exercise-config"),
                    routineId = FoundationId("routine-config"),
                    exerciseCatalogId = exercise.id,
                    displayNameSnapshot = exercise.displayName,
                    position = OrderedPosition(0),
                    plannedSets = listOf(
                        RoutineSetTemplate(
                            id = FoundationId("routine-set-config"),
                            routineExerciseId = FoundationId("routine-exercise-config"),
                            position = OrderedPosition(0),
                            targetWeight = WeightKg(10.0),
                            targetReps = 12,
                            setKind = SetKind.BODYWEIGHT,
                            targetDistanceMeters = 30.0,
                            effortTarget = EffortTarget.Rpe(80)
                        )
                    ),
                    resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                        customConfiguration,
                        LoggingConfigurationSource.USER_DEFAULT
                    )
                )
            ),
            createdAt = instant(3_100),
            updatedAt = instant(3_100)
        )
        store.saveRoutine(routine).successValue()

        store = harness.repositories().store
        assertEquals(exercise, store.exercise(exercise.id))
        assertEquals(userDefault, store.userExerciseConfiguration(exercise.id))
        assertEquals(routine, store.routine(routine.id))
        store.clearUserExerciseConfiguration(exercise.id).successValue()
        assertNull(store.userExerciseConfiguration(exercise.id))
    }

    @Test
    fun userDefaultRejectsStaleDefinitionRevision() = runTest {
        val store = SqlFoundationStoreTestHarness().repositories().store
        val exercise = customExercise(customConfiguration)
        store.saveUserExercise(exercise).successValue()

        val result = store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                exerciseDefinitionId = exercise.id,
                configuration = customConfiguration,
                basedOnDefinitionRevision = ExerciseDefinitionRevision(2),
                configuredAt = instant(2_000)
            )
        )

        assertIs<FoundationResult.Failure>(result)
        assertIs<FoundationError.Conflict>(result.error)
        assertNull(store.userExerciseConfiguration(exercise.id))
    }

    @Test
    fun userDefaultTreatsLegacyNullDefinitionRevisionAsRevisionOne() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val store = harness.repositories().store
        harness.database.exerciseQueriesQueries.insertExercise(
            id = "legacy-catalog",
            canonical_name = "legacy press",
            display_name = "Legacy Press",
            muscle_group = "Chest",
            equipment = "Barbell",
            movement_pattern = "Push",
            exercise_type = "Strength",
            experience_level = "Beginner",
            body_region = "Upper Body",
            is_bodyweight = 0,
            logging_mode = "WEIGHTED",
            is_user_created = 1,
            created_at = 1_000,
            updated_at = 1_000,
            archived_at = null,
            source_seed_version = null,
            user_notes = null
        )
        val userDefault = UserExerciseConfiguration(
            exerciseDefinitionId = FoundationId("legacy-catalog"),
            configuration = LegacyLoggingConfigurations.weighted,
            basedOnDefinitionRevision = ExerciseDefinitionRevision(1),
            configuredAt = instant(2_000)
        )

        assertEquals(userDefault, store.saveUserExerciseConfiguration(userDefault).successValue())
        assertEquals(userDefault, store.userExerciseConfiguration(userDefault.exerciseDefinitionId))
    }

    @Test
    fun nullableLegacyBodyweightLoadInfersUnspecifiedRole() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        harness.repositories()
        harness.database.workoutQueriesQueries.insertActiveWorkout(
            "legacy-workout", 1_000, null, null, "ACTIVE", 1_000, 1_000
        )
        harness.database.setQueriesQueries.insertActiveExercise(
            "legacy-exercise", "legacy-workout", "legacy-catalog", "Pull-Up", null,
            1, 0, "BODYWEIGHT", null, null, null, null, 120, 1
        )
        harness.database.setQueriesQueries.upsertExerciseSet(
            "legacy-set", "legacy-workout", "legacy-exercise", 0, "BODYWEIGHT",
            20.0, 6, null, 2_000, 1_000, 2_000, null
        )

        val recovered = assertNotNull(harness.repositories().store.currentActiveWorkout())
        assertEquals(
            LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.id,
            recovered.exercises.single().sets.single().captureConfigurationId
        )
        assertEquals(
            LoadRole.LEGACY_UNSPECIFIED,
            recovered.exercises.single().sets.single()
                .let { LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.measures[1].loadRole }
        )
    }

    @Test
    fun malformedReferencedConfigurationReturnsControlledAbsence() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        harness.repositories()
        harness.driver.executeStatement("DROP TRIGGER logging_configurations_no_update")
        harness.driver.executeStatement(
            "UPDATE logging_configurations SET content_hash = 'corrupt' WHERE id = 'legacy_weighted_v1'"
        )

        assertNull(harness.repositories().store.loggingConfiguration(LegacyLoggingConfigurations.weighted.id))
    }

    @Test
    fun migratedSchema10DatabaseReusesBackfilledConfigurations() = runTest {
        withMigratedFixture("bodyweight-with-load.db") { driver ->
            val store = SqlFoundationStore(WorkoutDatabase(driver))
            assertEquals(4, store.loggingConfigurations().size)
            assertEquals(
                LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.id.value,
                driver.singleString(
                    "SELECT capture_configuration_id FROM exercise_sets WHERE id = 'set-body-load'"
                )
            )
        }
    }

    @Test
    fun unknownFutureLegacyKindWithNullReferenceDoesNotCrashRecovery() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        harness.repositories()
        harness.database.workoutQueriesQueries.insertActiveWorkout(
            "future-workout", 1_000, null, null, "ACTIVE", 1_000, 1_000
        )
        harness.database.setQueriesQueries.insertActiveExercise(
            "future-exercise", "future-workout", "future-catalog", "Future", null,
            0, 0, "FUTURE_MODE", null, null, null, null, 120, 1
        )
        harness.database.setQueriesQueries.upsertExerciseSet(
            "future-set", "future-workout", "future-exercise", 0, "FUTURE_KIND",
            null, 5, null, 2_000, 1_000, 2_000, null
        )

        assertNull(harness.repositories().store.currentActiveWorkout())
    }

    @Test
    fun aggregateWritesRejectUnknownConfigurationReferencesBeforeForeignKeyWrites() = runTest {
        val store = SqlFoundationStoreTestHarness().repositories().store
        val workoutId = FoundationId("workout-missing-config")
        val exerciseId = FoundationId("exercise-missing-config")
        val missingId = LoggingConfigurationId("missing-config")
        val set = ExerciseSet(
            id = FoundationId("set-missing-config"),
            exerciseInstanceId = exerciseId,
            position = OrderedPosition(0),
            setKind = SetKind.BODYWEIGHT,
            weight = null,
            reps = 5,
            loggedAt = instant(2_000),
            createdAt = instant(1_000),
            updatedAt = instant(2_000),
            captureConfigurationId = missingId
        )
        val workout = ActiveWorkout(
            id = workoutId,
            startedAt = instant(1_000),
            exercises = listOf(
                ActiveExercise(
                    id = exerciseId,
                    activeWorkoutId = workoutId,
                    reference = ExerciseReference(
                        exerciseCatalogId = FoundationId("catalog-missing-config"),
                        displayNameSnapshot = "Missing",
                        isBodyweight = true
                    ),
                    position = OrderedPosition(0),
                    sets = listOf(set)
                )
            ),
            createdAt = instant(1_000),
            updatedAt = instant(2_000)
        )

        val result = store.createActiveWorkout(workout)

        assertIs<FoundationResult.Failure>(result)
        assertIs<FoundationError.Validation>(result.error)
        assertNull(store.currentActiveWorkout())

        val draftResult = store.saveSetDraft(
            PersistedSetDraft(
                draftId = FoundationId("draft-missing-config"),
                activeWorkoutId = workoutId,
                exerciseInstanceId = exerciseId,
                position = OrderedPosition(0),
                setKind = SetKind.BODYWEIGHT,
                reps = 5,
                weight = null,
                updatedAt = instant(2_000),
                captureConfigurationId = missingId
            )
        )
        assertIs<FoundationResult.Failure>(draftResult)
        assertIs<FoundationError.Validation>(draftResult.error)

        val setResult = store.confirmSet(workoutId, set)
        assertIs<FoundationResult.Failure>(setResult)
        assertIs<FoundationError.Validation>(setResult.error)
    }

    private fun customExercise(configuration: LoggingConfiguration): ExerciseCatalogItem =
        ExerciseCatalogItem(
            id = FoundationId("catalog-config"),
            canonicalName = "weighted carry",
            displayName = "Weighted Carry",
            muscleGroup = "Full Body",
            equipment = "Bodyweight",
            movementPattern = "Carry",
            exerciseType = "Bodyweight",
            experienceLevel = "Intermediate",
            bodyRegion = "Full Body",
            isBodyweight = true,
            loggingMode = ExerciseLoggingMode.BODYWEIGHT,
            isUserCreated = true,
            createdAt = instant(1_000),
            updatedAt = instant(1_000),
            origin = ExerciseDefinitionOrigin.USER,
            definitionRevision = ExerciseDefinitionRevision(3),
            seedKey = null,
            defaultLoggingConfiguration = configuration
        )

    private suspend fun withMigratedFixture(name: String, block: suspend (SqlDriver) -> Unit) {
        val resource = checkNotNull(javaClass.getResourceAsStream("/schema-10/$name"))
        val file = Files.createTempFile("logging-persistence-", ".db").toFile()
        resource.use { Files.copy(it, file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        try {
            driver.executeStatement("PRAGMA foreign_keys = ON")
            WorkoutDatabase.Schema.migrate(driver, 10, 11)
            block(driver)
        } finally {
            driver.close()
            file.delete()
        }
    }

    private fun SqlDriver.executeStatement(sql: String) {
        execute(null, sql, 0, null).value
    }

    private fun SqlDriver.singleString(sql: String): String? =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(cursor.getString(0))
            },
            parameters = 0,
            binders = null
        ).value

    private companion object {
        val customConfiguration = LoggingConfiguration(
            id = LoggingConfigurationId("bodyweight_added_distance_effort_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.ADDED_TO_BODYWEIGHT),
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.OPTIONAL)
            ),
            observedEffort = ObservedEffortSpec(
                listOf(EffortKind.RPE, EffortKind.RIR, EffortKind.FAILURE_OUTCOME)
            )
        )
    }
}
