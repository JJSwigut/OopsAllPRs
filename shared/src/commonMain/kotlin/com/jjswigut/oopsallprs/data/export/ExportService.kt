package com.jjswigut.oopsallprs.data.export

import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.repository.ExportRepository

class ExportService(
    private val repository: ExportRepository
) {
    suspend fun export(type: ExportType, unit: WeightUnit): FoundationResult<ExportFile> =
        repository.export(type, unit)
}
