package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.testing.instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseDisplayMetadataTest {
    @Test
    fun bodyweightEquipmentIsNotRepeatedInPickerMetadata() {
        val row = catalogItem(
            muscleGroup = "Full Body",
            equipment = "Bodyweight",
            isBodyweight = true
        ).toPickerRow()

        assertEquals("Full Body", row.subtitle)
        assertEquals("Full Body · Bodyweight", exerciseMetadataLine(row.subtitle, row.isBodyweight, separator = " · "))
    }

    @Test
    fun customBodyweightExerciseShowsSingleBodyweightLabel() {
        val row = catalogItem(
            muscleGroup = "Custom",
            equipment = "Bodyweight",
            isBodyweight = true,
            isUserCreated = true
        ).toManagedExerciseRow()

        assertEquals("", row.subtitle)
        assertEquals("Bodyweight", exerciseMetadataLine(row.subtitle, row.isBodyweight, separator = " • "))
    }

    private fun catalogItem(
        muscleGroup: String,
        equipment: String,
        isBodyweight: Boolean,
        isUserCreated: Boolean = false
    ): ExerciseCatalogItem =
        ExerciseCatalogItem(
            id = FoundationId("exercise-test"),
            canonicalName = "test exercise",
            displayName = "Test Exercise",
            muscleGroup = muscleGroup,
            equipment = equipment,
            movementPattern = "General",
            exerciseType = if (isBodyweight) "Bodyweight" else "Strength",
            experienceLevel = "All",
            bodyRegion = "Full Body",
            isBodyweight = isBodyweight,
            isUserCreated = isUserCreated,
            createdAt = instant(1_000),
            updatedAt = instant(1_000)
        )
}
