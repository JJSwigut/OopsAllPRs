package com.jjswigut.oopsallprs.ds.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A single depth level: blur radius, spread, and alpha for the outer glow that replaces elevation. */
@Immutable
data class FitGlowLevel(
    val radius: Dp,
    val spread: Dp,
    val alpha: Float,
)

/** Named depth levels. Consumed by the glass renderer via FitTheme.glow. */
@Immutable
data class FitGlow(
    val none: FitGlowLevel = FitGlowLevel(0.dp, 0.dp, 0f),
    val low: FitGlowLevel = FitGlowLevel(12.dp, 0.dp, 0.20f),
    val medium: FitGlowLevel = FitGlowLevel(24.dp, 2.dp, 0.30f),
    val high: FitGlowLevel = FitGlowLevel(40.dp, 4.dp, 0.42f),
)
