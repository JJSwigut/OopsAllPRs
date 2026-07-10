package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import kotlinx.datetime.Instant

class LocalRevisionCalculator {
    fun revision(summary: SnapshotSummary, timestamps: List<Instant?>): BackupRevision {
        val latest = timestamps.filterNotNull().maxByOrNull { it.toEpochMilliseconds() }
            ?: Instant.fromEpochMilliseconds(0)
        val value = listOf(
            latest.toEpochMilliseconds().toString(),
            summary.workoutCount,
            summary.setCount,
            summary.routineCount,
            summary.customExerciseCount,
            summary.progressRecordCount,
            summary.hasActiveWorkout
        ).joinToString(":")
        return BackupRevision(value = value, timestamp = latest, summary = summary)
    }
}
