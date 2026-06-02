package com.jjswigut.oopsallprs.ui.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RollerFieldModelTest {
    @Test
    fun stepBySnapsAndClampsToRange() {
        val state = RollerFieldState(
            value = 5.0,
            range = 0.0..10.0,
            step = 2.5
        )

        assertEquals(7.5, state.stepBy(1).value)
        assertEquals(0.0, state.stepBy(-10).value)
        assertEquals(10.0, state.updateRaw(9.6).value)
    }

    @Test
    fun validDirectEntrySnapsValueAndKeepsEntryText() {
        val state = RollerFieldState(
            value = 5.0,
            range = 0.0..10.0,
            step = 2.5
        )

        val next = state.updateDirectEntry("7.4")

        assertTrue(next.isDirectEntry)
        assertEquals("7.4", next.directEntryText)
        assertEquals(7.5, next.value)
        assertNull(next.errorMessage)
    }

    @Test
    fun invalidDirectEntryPreservesValueAndReportsError() {
        val state = RollerFieldState(
            value = 5.0,
            range = 0.0..10.0,
            step = 2.5
        )

        val next = state.updateDirectEntry("heavy")

        assertTrue(next.isDirectEntry)
        assertEquals("heavy", next.directEntryText)
        assertEquals(5.0, next.value)
        assertNotNull(next.errorMessage)
    }
}
