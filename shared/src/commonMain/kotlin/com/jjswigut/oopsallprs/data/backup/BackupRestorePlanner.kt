package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.repository.BackupRepository

class BackupRestorePlanner(
    private val repository: BackupRepository
) {
    suspend fun plan(pkg: BackupPackage) = repository.restorePlan(pkg)
}
