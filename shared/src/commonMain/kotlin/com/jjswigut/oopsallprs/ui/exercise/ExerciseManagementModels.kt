package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.FoundationId

data class ManagedExerciseRow(
    val exerciseCatalogId: FoundationId,
    val displayName: String,
    val subtitle: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode,
    val isUserCreated: Boolean,
    val canEdit: Boolean,
    val canArchive: Boolean
)

data class ExerciseEditDraft(
    val exerciseCatalogId: FoundationId? = null,
    val name: String = "",
    val loggingMode: ExerciseLoggingMode = ExerciseLoggingMode.WEIGHTED,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean = exerciseCatalogId != null
    val canSave: Boolean = name.isNotBlank() && !isSaving
    val isBodyweight: Boolean = loggingMode == ExerciseLoggingMode.BODYWEIGHT || loggingMode == ExerciseLoggingMode.TIMED
}

fun ExerciseCatalogItem.toManagedExerciseRow(): ManagedExerciseRow =
    ManagedExerciseRow(
        exerciseCatalogId = id,
        displayName = displayName,
        subtitle = exerciseContextSubtitle(muscleGroup, equipment, isBodyweight),
        isBodyweight = isBodyweight,
        loggingMode = loggingMode,
        isUserCreated = isUserCreated,
        canEdit = isUserCreated,
        canArchive = isUserCreated
    )

fun ManagedExerciseRow.toEditDraft(): ExerciseEditDraft =
    ExerciseEditDraft(
        exerciseCatalogId = exerciseCatalogId,
        name = displayName,
        loggingMode = loggingMode
    )
