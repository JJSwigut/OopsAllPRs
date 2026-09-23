package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupRevision
import kotlinx.datetime.Instant

class LocalRevisionCalculator {
    fun revision(pkg: BackupPackage): BackupRevision {
        val summary = pkg.summary.toDomain()
        val latest = listOfNotNull(summary.latestUpdatedTimestamp, summary.latestWorkoutTimestamp)
            .maxByOrNull { it.toEpochMilliseconds() }
            ?: Instant.fromEpochMilliseconds(0)
        return BackupRevision(value = BackupSnapshotIdentity.revision(pkg), timestamp = latest, summary = summary)
    }
}
