package com.jjswigut.oopsallprs.platform

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError

internal data class AndroidLifetimePurchaseOption(
    val formattedPrice: String?,
    val offerToken: String?,
    val purchaseOptionId: String?,
    val offerId: String?,
    val hasRentalDetails: Boolean,
    val hasPreorderDetails: Boolean
) {
    val priceLabel: String?
        get() = formattedPrice?.takeIf { it.isNotBlank() }

    val token: String?
        get() = offerToken?.takeIf { it.isNotBlank() }

    val isBaseBuyPurchaseOption: Boolean
        get() = !hasRentalDetails && !hasPreorderDetails && offerId.isNullOrBlank()
}

internal object AndroidLifetimePurchaseOptionSelector {
    const val EXPECTED_BUY_PURCHASE_OPTION_ID: String = "buy"

    fun priceLabel(options: List<AndroidLifetimePurchaseOption>): FoundationResult<String> {
        val selected = select(options) ?: return foundationFailure(
            FoundationError.Platform("Google Play has no eligible lifetime buy purchase option with an offer token.")
        )
        val price = selected.priceLabel ?: return foundationFailure(
            FoundationError.Platform("Google Play did not return a price for the lifetime purchase option. Try again later.")
        )
        return foundationSuccess(price)
    }

    fun select(options: List<AndroidLifetimePurchaseOption>): AndroidLifetimePurchaseOption? {
        val baseBuyOptions = options.filter { option ->
            option.isBaseBuyPurchaseOption && option.token != null
        }
        if (baseBuyOptions.isEmpty()) return null

        return baseBuyOptions.firstOrNull { option ->
            option.purchaseOptionId == EXPECTED_BUY_PURCHASE_OPTION_ID
        } ?: baseBuyOptions.singleOrNull()
    }
}
