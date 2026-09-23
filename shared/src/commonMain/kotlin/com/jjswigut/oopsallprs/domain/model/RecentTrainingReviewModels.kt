package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

/** A device-local, half-open interval used to report recent completed training. */
data class RecentTrainingWindow(
    val startInclusive: Instant,
    val endExclusive: Instant,
    val label: String = "Last 7 days"
) {
    init {
        require(startInclusive < endExclusive) { "Recent training window must not be empty or reversed" }
    }

    fun includes(finishedAt: Instant): Boolean =
        finishedAt >= startInclusive && finishedAt < endExclusive
}

/**
 * A local read model for recent completed-training facts. It deliberately
 * retains source workouts and granular records rather than inventing a score.
 */
data class RecentTrainingReview(
    val window: RecentTrainingWindow,
    val completedWorkouts: List<CompletedWorkout>,
    val personalRecords: List<PersonalRecord>,
    val progressReadings: List<ProgressionSummary>
) {
    val completedWorkoutCount: Int get() = completedWorkouts.size
    val loggedSetCount: Int get() = completedWorkouts.sumOf { workout ->
        workout.exercises.sumOf { exercise -> exercise.loggedSets.count { it.isLogged } }
    }
    val totalDurationMs: Long get() = completedWorkouts.sumOf { workout ->
        workout.durationMs.takeIf { it > 0L } ?: 0L
    }
}
