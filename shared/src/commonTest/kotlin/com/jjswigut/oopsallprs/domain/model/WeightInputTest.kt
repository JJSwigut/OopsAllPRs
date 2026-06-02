package com.jjswigut.oopsallprs.domain.model

import com.jjswigut.oopsallprs.domain.usecase.WeightInputUseCases
import com.jjswigut.oopsallprs.testing.successValue
import kotlin.test.Test
import kotlin.test.assertTrue

class WeightInputTest {
    @Test
    fun poundsRoundTripToCanonicalKilograms() {
        val useCases = WeightInputUseCases()
        val parsed = useCases.parseWeight("220.462", WeightUnit.POUNDS).successValue()
        assertTrue(parsed.almostEquals(WeightKg(100.0), tolerance = 0.01))
    }

    @Test
    fun commaDecimalInputIsAccepted() {
        val useCases = WeightInputUseCases()
        val parsed = useCases.parseWeight("12,5", WeightUnit.KILOGRAMS).successValue()
        assertTrue(parsed.almostEquals(WeightKg(12.5)))
    }
}
