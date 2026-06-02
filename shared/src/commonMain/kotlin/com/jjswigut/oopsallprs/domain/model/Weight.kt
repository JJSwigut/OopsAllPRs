package com.jjswigut.oopsallprs.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.round

private const val POUNDS_PER_KILOGRAM = 2.20462262185

enum class WeightUnit {
    KILOGRAMS,
    POUNDS
}

@JvmInline
value class WeightKg(val value: Double) {
    init {
        require(value >= 0.0) { "Weight cannot be negative" }
    }

    fun displayValue(unit: WeightUnit): Double =
        when (unit) {
            WeightUnit.KILOGRAMS -> value
            WeightUnit.POUNDS -> value * POUNDS_PER_KILOGRAM
        }

    fun rounded(scale: Int = 4): WeightKg {
        val factor = pow10(scale)
        return WeightKg(round(value * factor) / factor)
    }

    fun almostEquals(other: WeightKg, tolerance: Double = 0.0001): Boolean =
        abs(value - other.value) <= tolerance

    companion object {
        fun fromDisplay(value: Double, unit: WeightUnit): WeightKg =
            when (unit) {
                WeightUnit.KILOGRAMS -> WeightKg(value)
                WeightUnit.POUNDS -> WeightKg(value / POUNDS_PER_KILOGRAM)
            }.rounded()
    }
}

private fun pow10(scale: Int): Double {
    var value = 1.0
    repeat(scale) { value *= 10.0 }
    return value
}
