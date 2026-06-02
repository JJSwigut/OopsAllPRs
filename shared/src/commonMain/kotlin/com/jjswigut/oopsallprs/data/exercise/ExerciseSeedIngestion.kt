package com.jjswigut.oopsallprs.data.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import kotlinx.datetime.Clock

class ExerciseSeedIngestion(
    private val repository: ExerciseRepository,
    private val sourceName: String = "oopsallprs-baseline"
) {
    suspend fun ingest(csv: String, sourceVersion: String = contentHash(csv)): FoundationResult<ExerciseSeedImport> {
        val report = ExerciseCsvParser.parse(csv)
        val now = Clock.System.now()
        val items = report.acceptedRows.map { row ->
            ExerciseCatalogItem(
                id = newFoundationId("exercise"),
                canonicalName = row.canonicalName,
                displayName = row.exerciseName,
                muscleGroup = row.muscleGroup,
                equipment = row.equipment,
                movementPattern = row.movementPattern,
                exerciseType = row.exerciseType,
                experienceLevel = row.experienceLevel,
                bodyRegion = row.bodyRegion,
                isBodyweight = row.isBodyweight,
                loggingMode = row.loggingMode,
                isUserCreated = false,
                createdAt = now,
                updatedAt = now,
                sourceSeedVersion = sourceVersion
            )
        }
        val import = ExerciseSeedImport(
            id = newFoundationId("seed"),
            sourceName = sourceName,
            sourceHash = sourceVersion,
            importedAt = now,
            rowCount = items.size,
            rejectedRowCount = report.rejectedRows.size,
            warnings = report.rejectedRows.map { "row ${it.rowNumber}: ${it.reason}" } +
                report.duplicateCanonicalNames.map { "duplicate canonical name: $it" }
        )
        return when (val saved = repository.saveSeedItems(items, import)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(saved.value)
        }
    }
}

fun contentHash(content: String): String =
    content.fold(0) { acc, char -> (acc * 31) + char.code }.toUInt().toString(16)
