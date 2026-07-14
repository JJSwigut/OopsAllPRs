package com.jjswigut.oopsallprs.ui.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class PrivacyPolicyTest {
    @Test
    fun opensApprovedPrivacyPolicyUrl() {
        var openedUrl: String? = null

        openPrivacyPolicy { openedUrl = it }

        assertEquals(
            "https://jjswigut.github.io/oops-all-prs-site/privacy/",
            openedUrl
        )
    }
}
