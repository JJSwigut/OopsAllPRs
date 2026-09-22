package com.jjswigut.oopsallprs.ui.common

import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun Instant.shortDateLabel(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = toLocalDateTime(timeZone)
    return "${local.month.shortLabel()} ${local.dayOfMonth}"
}

internal fun Instant.shortDateTimeLabel(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${local.monthNumber.twoDigits()}-${local.dayOfMonth.twoDigits()} " +
        "${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')

private fun Month.shortLabel(): String =
    when (this) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
    }
