package com.jjswigut.oopsallprs.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okio.ByteString.Companion.encodeUtf8

object BackupSnapshotIdentity {
    const val PREFIX: String = "snapshot-v1:"

    fun isContentRevision(value: String?): Boolean =
        value != null && value.startsWith(PREFIX) && value.length == PREFIX.length + 64 &&
            value.substring(PREFIX.length).all { it in '0'..'9' || it in 'a'..'f' }

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        // Identity must remain computable even for data the backup codec will reject.
        allowSpecialFloatingPointValues = true
    }
    private val envelopeFields = setOf(
        "createdAt", "deviceId", "lastLocalRevision", "appSchemaVersion", "formatVersion", "summary"
    )

    fun revision(pkg: BackupPackage): String {
        val normalized = pkg.copy(
            // Measure and effort-list order is part of a logging configuration's identity.
            loggingConfigurations = pkg.loggingConfigurations.sortedBy { it.id },
            userExerciseConfigurations = pkg.userExerciseConfigurations.sortedBy { it.exerciseDefinitionId },
            exercises = pkg.exercises.sortedBy { it.id },
            routines = pkg.routines.sortedBy { it.id }.map { routine ->
                routine.copy(exercises = routine.exercises
                    .sortedWith(compareBy({ it.position }, { it.id }))
                    .map { exercise ->
                        exercise.copy(plannedSets = exercise.plannedSets
                            .sortedWith(compareBy({ it.position }, { it.id })))
                    })
            },
            activeWorkout = pkg.activeWorkout?.let { workout ->
                workout.copy(exercises = workout.exercises
                    .sortedWith(compareBy({ it.position }, { it.id }))
                    .map { exercise ->
                        exercise.copy(sets = exercise.sets
                            .sortedWith(compareBy({ it.position }, { it.id })))
                    })
            },
            activeSetDrafts = pkg.activeSetDrafts.sortedBy { it.draftId },
            completedWorkouts = pkg.completedWorkouts.sortedBy { it.id }.map { workout ->
                workout.copy(exercises = workout.exercises
                    .sortedWith(compareBy({ it.position }, { it.id }))
                    .map { exercise ->
                        exercise.copy(loggedSets = exercise.loggedSets
                            .sortedWith(compareBy({ it.position }, { it.id })))
                    })
            },
            personalRecords = pkg.personalRecords.sortedBy { it.id },
            progressPoints = pkg.progressPoints.sortedBy { it.id },
            exportMetadata = pkg.exportMetadata.sortedBy { it.id }
        )
        val content = JsonObject(json.encodeToJsonElement(BackupPackageDto.serializer(), normalized)
            .jsonObject.filterKeys { it !in envelopeFields })
        val canonical = json.encodeToString(JsonElement.serializer(), content.canonicalKeys())
        return "$PREFIX${canonical.encodeUtf8().sha256().hex()}"
    }

    private fun JsonElement.canonicalKeys(): JsonElement = when (this) {
        is JsonObject -> JsonObject(keys.sorted().associateWith { getValue(it).canonicalKeys() })
        is JsonArray -> JsonArray(map { it.canonicalKeys() })
        else -> this
    }
}
