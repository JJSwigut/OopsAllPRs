package com.jjswigut.oopsallprs.domain.model

data class FinishWorkoutOutcome(
    val workout: CompletedWorkout,
    val newlyCompleted: Boolean,
    val warnings: List<FinishPostCommitWarning> = emptyList()
)

enum class FinishPostCommitWarning {
    TIMER_CLEANUP_FAILED,
    PROGRESS_REFRESH_FAILED
}
