package com.jjswigut.oopsallprs.ui.profile

const val PRIVACY_POLICY_URL = "https://jjswigut.github.io/oops-all-prs-site/privacy/"

fun openPrivacyPolicy(openUrl: (String) -> Unit) {
    openUrl(PRIVACY_POLICY_URL)
}
