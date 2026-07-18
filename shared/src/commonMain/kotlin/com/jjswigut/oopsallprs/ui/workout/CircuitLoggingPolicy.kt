package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition

internal enum class RestAfterLogging {
    KEEP,
    CLEAR,
    START_CONFIGURED
}

internal data class LoggingTransition(
    val nextExerciseId: FoundationId?,
    val restAfterLogging: RestAfterLogging,
    val showExerciseOverview: Boolean = false
)

internal fun ActiveWorkout.loggingTransitionAfter(
    exerciseInstanceId: FoundationId,
    loggedPosition: OrderedPosition
): LoggingTransition {
    val current = exercises.firstOrNull { it.id == exerciseInstanceId }
        ?: return LoggingTransition(exerciseInstanceId, RestAfterLogging.KEEP)
    val group = current.groupContext
        ?: return LoggingTransition(
            nextExerciseId = exerciseInstanceId,
            restAfterLogging = if (current.rest.isEnabled) {
                RestAfterLogging.START_CONFIGURED
            } else {
                RestAfterLogging.KEEP
            }
        )
    val grouped = circuitMembers(group.groupId)
    val currentIndex = grouped.indexOfFirst { it.id == exerciseInstanceId }
    if (currentIndex == -1) {
        return LoggingTransition(exerciseInstanceId, RestAfterLogging.CLEAR)
    }

    if (currentIndex < grouped.lastIndex) {
        return LoggingTransition(
            nextExerciseId = grouped[currentIndex + 1].id,
            restAfterLogging = RestAfterLogging.CLEAR
        )
    }

    val completedRound = grouped.all { exercise ->
        exercise.sets.any { it.isLogged && it.position == loggedPosition }
    }
    val nextCircuitMember = nextUnfinishedCircuitMember(group.groupId)
    if (!completedRound) {
        return LoggingTransition(
            nextExerciseId = nextCircuitMember?.id ?: exerciseInstanceId,
            restAfterLogging = RestAfterLogging.CLEAR
        )
    }

    if (loggedPosition.value + 1 < group.rounds && nextCircuitMember != null) {
        return LoggingTransition(
            nextExerciseId = nextCircuitMember.id,
            restAfterLogging = if (current.rest.isEnabled) {
                RestAfterLogging.START_CONFIGURED
            } else {
                RestAfterLogging.CLEAR
            }
        )
    }

    if (nextCircuitMember != null) {
        return LoggingTransition(
            nextExerciseId = nextCircuitMember.id,
            restAfterLogging = RestAfterLogging.CLEAR
        )
    }

    val next = nextUnfinishedExerciseAfter(group.groupId)
    return LoggingTransition(
        nextExerciseId = next?.id ?: exerciseInstanceId,
        restAfterLogging = RestAfterLogging.CLEAR,
        showExerciseOverview = next == null
    )
}

internal fun ActiveWorkout.nextExerciseAfterCompletedCircuit(
    exerciseInstanceId: FoundationId
): ActiveExercise? {
    val groupId = exercises.firstOrNull { it.id == exerciseInstanceId }
        ?.groupContext
        ?.groupId
        ?: return null
    if (nextUnfinishedCircuitMember(groupId) != null) return null
    return nextUnfinishedExerciseAfter(groupId)
}

private fun ActiveWorkout.nextUnfinishedExerciseAfter(groupId: FoundationId): ActiveExercise? {
    val sorted = exercises.sortedBy { it.position.value }
    val lastGroupPosition = sorted
        .filter { it.groupContext?.groupId == groupId }
        .maxOfOrNull { it.position.value }
        ?: return null
    val visitedGroups = mutableSetOf<FoundationId>()
    sorted.filter { it.position.value > lastGroupPosition }.forEach { exercise ->
        val candidateGroupId = exercise.groupContext?.groupId
        if (candidateGroupId == null) return exercise
        if (visitedGroups.add(candidateGroupId)) {
            nextUnfinishedCircuitMember(candidateGroupId)?.let { return it }
        }
    }
    return null
}

private fun ActiveWorkout.nextUnfinishedCircuitMember(groupId: FoundationId): ActiveExercise? {
    val grouped = circuitMembers(groupId)
    val rounds = grouped.firstOrNull()?.groupContext?.rounds ?: return null
    repeat(rounds) { round ->
        grouped.forEach { exercise ->
            if (exercise.sets.none { it.isLogged && it.position.value == round }) {
                return exercise
            }
        }
    }
    return null
}

private fun ActiveWorkout.circuitMembers(groupId: FoundationId): List<ActiveExercise> =
    exercises
        .filter { it.groupContext?.groupId == groupId }
        .sortedBy { it.position.value }
