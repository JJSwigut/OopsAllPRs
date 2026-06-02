package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId

data class ExercisePickerResultRow(
    val exerciseCatalogId: FoundationId,
    val displayName: String,
    val subtitle: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
    val isUserCreated: Boolean
) {
    fun toReference(): ExerciseReference =
        ExerciseReference(
            exerciseCatalogId = exerciseCatalogId,
            displayNameSnapshot = displayName,
            isBodyweight = isBodyweight,
            loggingMode = loggingMode
        )
}

data class CustomExerciseDraft(
    val name: String = "",
    val loggingMode: ExerciseLoggingMode = ExerciseLoggingMode.WEIGHTED
) {
    val isBodyweight: Boolean = loggingMode == ExerciseLoggingMode.BODYWEIGHT || loggingMode == ExerciseLoggingMode.TIMED
}

fun ExerciseCatalogItem.toPickerRow(): ExercisePickerResultRow =
    ExercisePickerResultRow(
        exerciseCatalogId = id,
        displayName = displayName,
        subtitle = exerciseContextSubtitle(muscleGroup, equipment, isBodyweight),
        isBodyweight = isBodyweight,
        loggingMode = loggingMode,
        isUserCreated = isUserCreated
    )
