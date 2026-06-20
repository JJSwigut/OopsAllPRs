package com.jjswigut.oopsallprs.ui.common

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun Instant.shortDateLabel(): String = toString().substringBefore("T")

internal fun Instant.shortDateTimeLabel(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${local.monthNumber.twoDigits()}-${local.dayOfMonth.twoDigits()} " +
        "${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')
