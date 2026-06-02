package com.jjswigut.oopsallprs.ui.common

import kotlinx.datetime.Instant

internal fun Instant.shortDateLabel(): String = toString().substringBefore("T")
