package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class CompactSetInputModelTest {
    @Test
    fun repsStepperNeverDropsBelowOne() {
        assertEquals(1, nextPositiveInt(1, -1))
        assertEquals(6, nextPositiveInt(5, 1))
    }

    @Test
    fun weightStepperClampsAtZeroAndFormatsCompactly() {
        assertEquals(0.0, nextWeightKg(WeightKg(1.0), -2.5).value)
        assertEquals("20", formatWeightKg(WeightKg(20.0)))
        assertEquals("22.5", formatWeightKg(WeightKg(22.5)))
    }

    @Test
    fun weightStepperUsesDisplayUnitForFormattingAndChanges() {
        val oneHundredFivePounds = WeightKg.fromDisplay(105.0, WeightUnit.POUNDS)

        assertEquals("105", formatDisplayWeight(oneHundredFivePounds, WeightUnit.POUNDS))
        assertEquals("47.6", formatDisplayWeight(oneHundredFivePounds, WeightUnit.KILOGRAMS))
        assertEquals("lb", weightUnitLabel(WeightUnit.POUNDS))
        assertEquals(5.0, weightStep(WeightUnit.POUNDS))
        assertEquals("110", formatDisplayWeight(nextDisplayWeight(oneHundredFivePounds, 5.0, WeightUnit.POUNDS), WeightUnit.POUNDS))
    }

    @Test
    fun customDisplayStepChangesWeightByThatAmount() {
        val oneHundredPounds = WeightKg.fromDisplay(100.0, WeightUnit.POUNDS)

        assertEquals(
            "102.5",
            formatDisplayWeight(nextDisplayWeight(oneHundredPounds, 2.5, WeightUnit.POUNDS), WeightUnit.POUNDS)
        )
        assertEquals(
            "97.5",
            formatDisplayWeight(nextDisplayWeight(oneHundredPounds, -2.5, WeightUnit.POUNDS), WeightUnit.POUNDS)
        )
    }
}
