package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadCalculatorModelsTest {
    @Test
    fun barbellPlatePairsAddToBothSidesPlusBar() {
        val state = BarbellLoadCalculatorState(barWeight = 45.0).addPlatePair(25.0)

        assertEquals(95.0, state.totalDisplayWeight())
        assertEquals(WeightKg.fromDisplay(95.0, WeightUnit.POUNDS), state.toWeightKg(WeightUnit.POUNDS))
    }

    @Test
    fun metricBarbellPlatePairsUseCurrentUnit() {
        val state = BarbellLoadCalculatorState(barWeight = 20.0).addPlatePair(25.0)

        assertEquals(70.0, state.totalDisplayWeight())
        assertEquals(WeightKg(70.0), state.toWeightKg(WeightUnit.KILOGRAMS))
    }

    @Test
    fun barbellMultipleTapsIncrementPlateCountsAndClearToBarOnly() {
        val state = BarbellLoadCalculatorState(barWeight = 45.0)
            .addPlatePair(25.0)
            .addPlatePair(25.0)

        assertEquals(145.0, state.totalDisplayWeight())
        assertEquals(45.0, state.clearPlates().totalDisplayWeight())
    }

    @Test
    fun dumbbellSelectionRecordsPerDumbbellWeight() {
        val state = DumbbellLoadCalculatorState().select(50.0)

        assertEquals(WeightKg.fromDisplay(50.0, WeightUnit.POUNDS), state.toWeightKg(WeightUnit.POUNDS))
    }

    @Test
    fun calculatorEligibilityRequiresWeightedBarbellOrDumbbellEquipment() {
        assertEquals(LoadCalculatorKind.BARBELL, loadCalculatorKind("Barbell", SetKind.WEIGHTED))
        assertEquals(LoadCalculatorKind.DUMBBELL, loadCalculatorKind("Dumbbells", SetKind.WEIGHTED))
        assertNull(loadCalculatorKind("Barbell", SetKind.BODYWEIGHT))
        assertNull(loadCalculatorKind("Cable", SetKind.WEIGHTED))
    }
}
