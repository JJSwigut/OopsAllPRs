package com.jjswigut.oopsallprs.platform

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
