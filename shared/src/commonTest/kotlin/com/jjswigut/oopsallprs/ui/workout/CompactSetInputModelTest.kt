package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    @Test
    fun optionalRepsParsingTreatsBlankAsNullAndPositiveTextAsAValue() {
        assertNull(parseRepsInput("").value)
        assertNull(parseRepsInput("  ").value)
        assertEquals(12, parseRepsInput("12").value)
        assertNotNull(parseRepsInput("0").errorMessage)
        assertNotNull(parseRepsInput("abc").errorMessage)
    }

    @Test
    fun addedLoadParsingDistinguishesBlankZeroPositiveAndNegative() {
        val blank = parseWeightInput("", WeightUnit.KILOGRAMS, LoadRole.ADDED_TO_BODYWEIGHT)
        val zero = parseWeightInput("0", WeightUnit.KILOGRAMS, LoadRole.ADDED_TO_BODYWEIGHT)
        val positive = parseWeightInput("12.5", WeightUnit.KILOGRAMS, LoadRole.ADDED_TO_BODYWEIGHT)
        val negative = parseWeightInput("-1", WeightUnit.KILOGRAMS, LoadRole.ADDED_TO_BODYWEIGHT)

        assertNull(blank.value)
        assertEquals(WeightKg(0.0), zero.value)
        assertEquals(WeightKg(12.5), positive.value)
        assertNull(negative.value)
        assertEquals("Added bodyweight load cannot be negative", negative.errorMessage)
    }

    @Test
    fun effortParsingSupportsRirRpeAndOptionalFailureOutcome() {
        assertEquals(Effort(rir = 0), parseEffortInput("0", EffortKind.RIR).value)
        assertEquals(Effort(rir = 10), parseEffortInput("10", EffortKind.RIR).value)
        assertNotNull(parseEffortInput("11", EffortKind.RIR).errorMessage)
        assertEquals(Effort(rpeTenths = 85), parseEffortInput("8.5", EffortKind.RPE).value)
        assertNotNull(parseEffortInput("0.5", EffortKind.RPE).errorMessage)
        assertNull(parseEffortInput("", EffortKind.RIR).value)
        assertNull(parseEffortInput("", EffortKind.RPE).value)
    }

    @Test
    fun optionalDistanceParsingUsesNullForBlankAndRejectsZero() {
        assertNull(parseDistanceInput("").value)
        assertEquals(100.5, parseDistanceInput("100.5").value)
        assertNull(parseDistanceInput("0").value)
        assertNotNull(parseDistanceInput("0").errorMessage)
        assertNotNull(parseDistanceInput("-5").errorMessage)
    }
}
