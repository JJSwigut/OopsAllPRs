package com.jjswigut.oopsallprs.data.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository

class ExerciseCatalogInitializer(
    repository: ExerciseRepository
) {
    private val ingestion = ExerciseSeedIngestion(repository)

    suspend fun initialize(csv: String): FoundationResult<ExerciseSeedImport> =
        ingestion.ingest(csv)
}
