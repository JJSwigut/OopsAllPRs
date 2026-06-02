package com.jjswigut.oopsallprs.ds.token

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii. Glass leans on generous rounding. `pill` is effectively fully-rounded. */
@Immutable
data class FitShapes(
    val sm: Dp = 10.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val pill: Dp = 999.dp,
) {
    val smallShape get() = RoundedCornerShape(sm)
    val mediumShape get() = RoundedCornerShape(md)
    val largeShape get() = RoundedCornerShape(lg)
    val pillShape get() = RoundedCornerShape(pill)
}
