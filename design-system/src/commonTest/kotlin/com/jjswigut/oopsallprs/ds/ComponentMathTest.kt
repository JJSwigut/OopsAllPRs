package com.jjswigut.oopsallprs.ds

import com.jjswigut.oopsallprs.ds.component.RollerMath
import com.jjswigut.oopsallprs.ds.component.fitSelectionStateDescription
import com.jjswigut.oopsallprs.ds.component.fitToggleStateDescription
import com.jjswigut.oopsallprs.ds.component.sliderFraction
import com.jjswigut.oopsallprs.ds.component.stepValue
import com.jjswigut.oopsallprs.ds.component.valueFromFraction
import com.jjswigut.oopsallprs.ds.motion.formatCountDigits
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentMathTest {

    @Test
    fun roller_builds_inclusive_value_list_by_step() {
        assertEquals(listOf(40f, 45f, 50f, 55f, 60f), RollerMath.values(min = 40f, max = 60f, step = 5f))
    }

    @Test
    fun roller_index_and_value_are_inverse() {
        assertEquals(2, RollerMath.indexOf(value = 50f, min = 40f, step = 5f))
        assertEquals(55f, RollerMath.valueAt(index = 3, min = 40f, step = 5f))
    }

    @Test
    fun roller_index_clamps_into_bounds() {
        assertEquals(0, RollerMath.clampIndex(-3, size = 5))
        assertEquals(4, RollerMath.clampIndex(99, size = 5))
    }

    @Test
    fun stepper_increments_and_clamps() {
        assertEquals(12, stepValue(current = 10, delta = +1, step = 2, min = 0, max = 20))
        assertEquals(20, stepValue(current = 19, delta = +1, step = 5, min = 0, max = 20))
        assertEquals(0, stepValue(current = 1, delta = -1, step = 5, min = 0, max = 20))
    }

    @Test
    fun slider_fraction_maps_and_clamps() {
        assertEquals(0.5f, sliderFraction(value = 50f, min = 0f, max = 100f))
        assertEquals(0f, sliderFraction(value = -10f, min = 0f, max = 100f))
        assertEquals(1f, sliderFraction(value = 200f, min = 0f, max = 100f))
        assertEquals(50f, valueFromFraction(0.5f, min = 0f, max = 100f))
    }

    @Test
    fun animated_count_splits_digits() {
        assertEquals(listOf("1", "8", "5"), formatCountDigits(185))
        assertEquals(listOf("0"), formatCountDigits(0))
    }

    @Test
    fun accessibility_state_descriptions_are_stable() {
        assertEquals("Selected", fitSelectionStateDescription(true))
        assertEquals("Not selected", fitSelectionStateDescription(false))
        assertEquals("On", fitToggleStateDescription(true))
        assertEquals("Off", fitToggleStateDescription(false))
    }
}
