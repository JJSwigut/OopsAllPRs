package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.DecimalInputParser

class WeightInputUseCases {
    fun parseWeight(input: String, unit: WeightUnit): FoundationResult<WeightKg> =
        when (val parsed = DecimalInputParser.parse(input)) {
            is FoundationResult.Failure -> parsed
            is FoundationResult.Success -> foundationSuccess(WeightKg.fromDisplay(parsed.value, unit))
        }
}
