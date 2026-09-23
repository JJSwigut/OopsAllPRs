package com.jjswigut.oopsallprs.ui.common

fun Int.countLabel(singular: String, plural: String = "${singular}s"): String =
    "$this ${if (this == 1) singular else plural}"
