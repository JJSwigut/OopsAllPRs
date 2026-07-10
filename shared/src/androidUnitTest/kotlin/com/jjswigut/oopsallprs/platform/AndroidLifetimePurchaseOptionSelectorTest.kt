package com.jjswigut.oopsallprs.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    private fun option(
        formattedPrice: String = "$14.99",
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
