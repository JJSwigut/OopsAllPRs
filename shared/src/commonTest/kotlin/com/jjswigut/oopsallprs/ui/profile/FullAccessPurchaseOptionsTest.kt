package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.domain.model.FullAccessOfferState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import kotlin.test.Test
import kotlin.test.assertEquals

class FullAccessPurchaseOptionsTest {
    @Test
    fun lifetimeUnlockStatesTheImplementedPaidValue() {
        assertEquals(
            "Unlimited workout logging, backup, restore, and sync.",
            FULL_ACCESS_BENEFITS_SUMMARY
        )
    }

    @Test
    fun unavailableOfferUsesActionableCustomerCopy() {
        assertEquals(
            "The store isn't available right now. Try again in a moment.",
            purchaseOfferMessage(FullAccessOfferState.Unavailable("Service connection is disconnected."))
        )
    }

    @Test
    fun availableOfferKeepsTheStoreTerms() {
        val offer = FullAccessStoreOffer(
            title = "Lifetime unlock",
            priceLabel = "$9.99",
            termsLabel = "One-time purchase"
        )

        assertEquals("One-time purchase", purchaseOfferMessage(FullAccessOfferState.Available(offer)))
    }

    @Test
    fun purchaseOperationMessagesNeverExposePlatformDiagnostics() {
        assertEquals(
            "We couldn't restore a purchase. Check your store account and try again.",
            purchaseOperationMessage("Could not restore Google Play purchases. Service connection is disconnected.")
        )
        assertEquals("The purchase was canceled.", purchaseOperationMessage("User canceled purchase"))
    }
}
