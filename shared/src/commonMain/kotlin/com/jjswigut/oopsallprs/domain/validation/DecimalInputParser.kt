package com.jjswigut.oopsallprs.domain.validation

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess

object DecimalInputParser {
    fun parse(input: String): FoundationResult<Double> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return foundationFailure(FoundationError.Validation("Numeric input cannot be blank"))
        }

        val normalized = when {
            trimmed.count { it == ',' } == 1 && trimmed.count { it == '.' } == 0 -> trimmed.replace(',', '.')
            else -> trimmed
        }

        val value = normalized.toDoubleOrNull()
            ?: return foundationFailure(FoundationError.Validation("Invalid numeric input: $input"))

        if (value < 0.0) {
            return foundationFailure(FoundationError.Validation("Numeric input cannot be negative"))
        }

        return foundationSuccess(value)
    }
}
