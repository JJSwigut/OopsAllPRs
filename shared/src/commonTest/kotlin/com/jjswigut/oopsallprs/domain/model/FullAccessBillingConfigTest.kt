package com.jjswigut.oopsallprs.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FullAccessBillingConfigTest {
    @Test
    fun billingConfigUsesLifetimeUnlockOnly() {
        assertEquals("lifetime_unlock", FullAccessBillingConfig.LIFETIME_UNLOCK_PRODUCT_ID)
        assertEquals("lifetime_unlock", FullAccessBillingProductIds.LIFETIME)
        assertEquals(10, FullAccessBillingConfig.FREE_COMPLETED_WORKOUT_LIMIT)
        assertEquals(10, FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT)
    }
}
