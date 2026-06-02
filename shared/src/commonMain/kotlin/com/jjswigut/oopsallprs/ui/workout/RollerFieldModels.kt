package com.jjswigut.oopsallprs.ui.workout

import kotlin.math.roundToInt

data class RollerFieldState(
    val value: Double,
    val range: ClosedFloatingPointRange<Double>,
    val step: Double,
    val unitLabel: String = "",
    val isDirectEntry: Boolean = false,
    val directEntryText: String = "",
    val errorMessage: String? = null
) {
    fun stepBy(deltaSteps: Int): RollerFieldState =
        copy(value = snap(value + (step * deltaSteps)))

    fun updateRaw(rawValue: Double): RollerFieldState =
        copy(value = snap(rawValue), errorMessage = null)

    fun updateDirectEntry(text: String): RollerFieldState {
        val parsed = text.toDoubleOrNull()
        return if (parsed == null) {
            copy(isDirectEntry = true, directEntryText = text, errorMessage = "Enter a valid number")
        } else {
            copy(
                value = snap(parsed),
                isDirectEntry = true,
                directEntryText = text,
                errorMessage = null
            )
        }
    }

    private fun snap(rawValue: Double): Double {
        val clamped = rawValue.coerceIn(range.start, range.endInclusive)
        val steps = ((clamped - range.start) / step).roundToInt()
        return (range.start + (steps * step)).coerceIn(range.start, range.endInclusive)
    }
}
