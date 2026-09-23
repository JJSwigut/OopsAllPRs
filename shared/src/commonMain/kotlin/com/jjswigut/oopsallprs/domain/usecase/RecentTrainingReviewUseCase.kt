package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressionSummary
import com.jjswigut.oopsallprs.domain.model.RecentTrainingReview
import com.jjswigut.oopsallprs.domain.model.RecentTrainingWindow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class RecentTrainingReviewUseCase {
    fun project(
        workouts: List<CompletedWorkout>,
        personalRecords: List<PersonalRecord>,
        progressReadings: List<ProgressionSummary>,
        now: Instant,
        timeZone: TimeZone
    ): RecentTrainingReview {
        val window = windowAt(now, timeZone)
        val completedWorkouts = workouts.filter { window.includes(it.finishedAt) }
            .sortedByDescending { it.finishedAt }
        val completedWorkoutIds = completedWorkouts.map { it.id }.toSet()
        return RecentTrainingReview(
            window = window,
            completedWorkouts = completedWorkouts,
            personalRecords = personalRecords
                .filter { it.sourceWorkoutId in completedWorkoutIds }
                .sortedByDescending { it.achievedAt },
            progressReadings = progressReadings
        )
    }

    fun windowAt(now: Instant, timeZone: TimeZone): RecentTrainingWindow {
        val today = now.toLocalDateTime(timeZone).date
        return RecentTrainingWindow(
            startInclusive = today.minus(6, DateTimeUnit.DAY).atStartOfDayIn(timeZone),
            endExclusive = today.minus(-1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        )
    }
}
