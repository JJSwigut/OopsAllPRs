package com.jjswigut.oopsallprs.domain.model

import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlin.math.round

data class WeightStepPreference(
    val poundsStep: Double = DEFAULT_POUNDS_STEP,
    val kilogramsStep: Double = DEFAULT_KILOGRAMS_STEP
) {
    fun stepFor(unit: WeightUnit): Double =
        when (unit) {
            WeightUnit.POUNDS -> poundsStep
            WeightUnit.KILOGRAMS -> kilogramsStep
        }

    fun withStep(unit: WeightUnit, step: Double): FoundationResult<WeightStepPreference> {
        validate(unit, step)?.let { return foundationFailure(it) }
        val normalized = normalize(step)
        return foundationSuccess(
            when (unit) {
                WeightUnit.POUNDS -> copy(poundsStep = normalized)
                WeightUnit.KILOGRAMS -> copy(kilogramsStep = normalized)
            }
        )
    }

    companion object {
        const val DEFAULT_POUNDS_STEP = 5.0
        const val DEFAULT_KILOGRAMS_STEP = 2.5
        const val MIN_STEP = 0.25
        const val MAX_POUNDS_STEP = 100.0
        const val MAX_KILOGRAMS_STEP = 50.0

        fun defaultFor(unit: WeightUnit): Double =
            when (unit) {
                WeightUnit.POUNDS -> DEFAULT_POUNDS_STEP
                WeightUnit.KILOGRAMS -> DEFAULT_KILOGRAMS_STEP
            }

        fun validate(unit: WeightUnit, step: Double): FoundationError? {
            val normalized = normalize(step)
            val max = when (unit) {
                WeightUnit.POUNDS -> MAX_POUNDS_STEP
                WeightUnit.KILOGRAMS -> MAX_KILOGRAMS_STEP
            }
            return when {
                !normalized.isFinite() -> FoundationError.Validation("Weight step must be a number")
                normalized < MIN_STEP -> FoundationError.Validation("Weight step must be at least ${formatWeightStep(MIN_STEP)}")
                normalized > max -> FoundationError.Validation("Weight step must be ${formatWeightStep(max)} or less")
                else -> null
            }
        }

        fun normalize(step: Double): Double =
            round(step * 100.0) / 100.0
    }
}

fun formatWeightStep(step: Double): String {
    val normalized = WeightStepPreference.normalize(step)
    val whole = normalized.toLong()
    return if (normalized == whole.toDouble()) {
        whole.toString()
    } else {
        normalized.toString().trimEnd('0').trimEnd('.')
    }
}
