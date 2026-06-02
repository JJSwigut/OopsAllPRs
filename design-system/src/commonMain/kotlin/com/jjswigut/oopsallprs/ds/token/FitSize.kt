package com.jjswigut.oopsallprs.ds.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Heights, touch targets, and stroke widths. `touchMin` is intentionally above the 48dp floor. */
@Immutable
data class FitSize(
    val touchMin: Dp = 56.dp,
    val controlHeight: Dp = 56.dp,
    val iconSize: Dp = 24.dp,
    val ringStroke: Dp = 12.dp,
    val hairline: Dp = 1.dp,
)
