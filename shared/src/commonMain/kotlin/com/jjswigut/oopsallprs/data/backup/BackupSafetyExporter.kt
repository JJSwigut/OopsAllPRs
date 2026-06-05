package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.repository.BackupRepository

class BackupSafetyExporter(
    private val repository: BackupRepository
) {
    suspend fun createSafetyBackup() = repository.createPackage()
}
