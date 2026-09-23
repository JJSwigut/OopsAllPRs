package com.jjswigut.oopsallprs.platform

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidLifetimePurchaseOptionSelectorTest {
    @Test
    fun selectsConfiguredBuyOptionAndKeepsPriceWithOfferToken() {
        val selected = AndroidLifetimePurchaseOptionSelector.select(
            listOf(
                option(
                    formattedPrice = "$9.99",
                    offerToken = "other-token",
                    purchaseOptionId = "other"
                ),
                option(
                    formattedPrice = "$14.99",
                    offerToken = "buy-token",
                    purchaseOptionId = AndroidLifetimePurchaseOptionSelector.EXPECTED_BUY_PURCHASE_OPTION_ID
                )
            )
        )

        assertEquals("$14.99", selected?.priceLabel)
        assertEquals("buy-token", selected?.token)
        assertEquals(AndroidLifetimePurchaseOptionSelector.EXPECTED_BUY_PURCHASE_OPTION_ID, selected?.purchaseOptionId)
    }

    @Test
    fun supportsSoleLegacyOfferWithoutPurchaseOptionId() {
        val selected = AndroidLifetimePurchaseOptionSelector.select(
            listOf(
                option(
                    formattedPrice = "$14.99",
                    offerToken = "legacy-token",
                    purchaseOptionId = null
                )
            )
        )

        assertEquals("$14.99", selected?.priceLabel)
        assertEquals("legacy-token", selected?.token)
    }

    @Test
    fun rejectsNonBaseBuyOffersAndMissingTokens() {
        val selected = AndroidLifetimePurchaseOptionSelector.select(
            listOf(
                option(offerToken = "rent-token", hasRentalDetails = true),
                option(offerToken = "preorder-token", hasPreorderDetails = true),
                option(offerToken = "discount-token", offerId = "launch-discount"),
                option(offerToken = " ")
            )
        )

        assertNull(selected)
    }

    @Test
    fun returnsNullForAmbiguousBaseBuyOptionsWithoutConfiguredId() {
        val selected = AndroidLifetimePurchaseOptionSelector.select(
            listOf(
                option(offerToken = "first-token", purchaseOptionId = "first"),
                option(offerToken = "second-token", purchaseOptionId = "second")
            )
        )

        assertNull(selected)
    }

    @Test
    fun missingAndBlankStorePricesFailWithoutSelectingAnotherPrice() {
        for (price in listOf(null, "", " \t\n", "\u00a0")) {
            val configuredOption = option(formattedPrice = price, offerToken = "buy-token")
            val options = listOf(
                option(formattedPrice = "$9.99", purchaseOptionId = "other"),
                configuredOption
            )

            val failure = assertIs<FoundationResult.Failure>(AndroidLifetimePurchaseOptionSelector.priceLabel(options))

            assertTrue(failure.error.message.contains("did not return a price"))
            assertEquals(configuredOption, AndroidLifetimePurchaseOptionSelector.select(options))
        }
    }

    @Test
    fun priceLoadFailsWhenNoEligibleOptionExists() {
        val unavailableOptions = listOf(
            emptyList(),
            listOf(option(hasRentalDetails = true)),
            listOf(option(hasPreorderDetails = true)),
            listOf(option(offerId = "discount")),
            listOf(option(offerToken = null)),
            listOf(option(offerToken = " ")),
            listOf(option(purchaseOptionId = "first"), option(purchaseOptionId = "second"))
        )
        for (options in unavailableOptions) {
            val failure = assertIs<FoundationResult.Failure>(AndroidLifetimePurchaseOptionSelector.priceLabel(options))

            assertTrue(failure.error.message.contains("no eligible lifetime buy purchase option"))
        }
    }

    @Test
    fun keepsLocalizedEuroPriceExactlyAsReturnedByStore() {
        val price = "14,99\u00a0\u20ac"
        val result = AndroidLifetimePurchaseOptionSelector.priceLabel(listOf(option(formattedPrice = price)))

        assertEquals(price, assertIs<FoundationResult.Success<String>>(result).value)
    }

    @Test
    fun keepsLocalizedYenPriceExactlyAsReturnedByStore() {
        val price = "\u00a51,800"
        val result = AndroidLifetimePurchaseOptionSelector.priceLabel(listOf(option(formattedPrice = price)))

        assertEquals(price, assertIs<FoundationResult.Success<String>>(result).value)
    }

    private fun option(
        formattedPrice: String? = "$14.99",
        offerToken: String? = "offer-token",
        purchaseOptionId: String? = AndroidLifetimePurchaseOptionSelector.EXPECTED_BUY_PURCHASE_OPTION_ID,
        offerId: String? = null,
        hasRentalDetails: Boolean = false,
        hasPreorderDetails: Boolean = false
    ): AndroidLifetimePurchaseOption =
        AndroidLifetimePurchaseOption(
            formattedPrice = formattedPrice,
            offerToken = offerToken,
            purchaseOptionId = purchaseOptionId,
            offerId = offerId,
            hasRentalDetails = hasRentalDetails,
            hasPreorderDetails = hasPreorderDetails
        )
}
