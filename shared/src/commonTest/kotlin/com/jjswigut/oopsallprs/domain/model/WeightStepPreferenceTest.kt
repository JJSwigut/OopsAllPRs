package com.jjswigut.oopsallprs.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeightStepPreferenceTest {
    @Test
    fun defaultsPreserveCurrentStepperBehavior() {
        val preference = WeightStepPreference()

        assertEquals(5.0, preference.stepFor(WeightUnit.POUNDS))
        assertEquals(2.5, preference.stepFor(WeightUnit.KILOGRAMS))
    }

    @Test
    fun fractionalPoundsStepDoesNotChangeKilogramsStep() {
        val result = WeightStepPreference().withStep(WeightUnit.POUNDS, 2.5)

        val preference = (result as FoundationResult.Success).value
        assertEquals(2.5, preference.stepFor(WeightUnit.POUNDS))
        assertEquals(2.5, preference.stepFor(WeightUnit.KILOGRAMS))
    }

    @Test
    fun invalidStepFailsValidation() {
        val result = WeightStepPreference().withStep(WeightUnit.POUNDS, 0.0)

        assertTrue(result is FoundationResult.Failure)
    }
}
