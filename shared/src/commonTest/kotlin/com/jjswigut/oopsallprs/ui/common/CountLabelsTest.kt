package com.jjswigut.oopsallprs.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals

class CountLabelsTest {
    @Test
    fun usesSingularOnlyForOne() {
        assertEquals("1 exercise", 1.countLabel("exercise"))
        assertEquals("0 exercises", 0.countLabel("exercise"))
        assertEquals("2 exercises", 2.countLabel("exercise"))
    }

    @Test
    fun supportsAProvidedPlural() {
        assertEquals("1 planned set", 1.countLabel("planned set", "planned sets"))
        assertEquals("3 planned sets", 3.countLabel("planned set", "planned sets"))
    }
}
