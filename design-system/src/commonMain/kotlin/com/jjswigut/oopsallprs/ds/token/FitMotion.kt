package com.jjswigut.oopsallprs.ds.token

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable

/**
 * Named spring feel — the single source of truth for motion. Damping/stiffness are stored as raw
 * values so typed specs can be built for any animated type (Float, Color, Dp, …). The `*` Float
 * specs are convenience accessors for the common `animateFloatAsState` case; for other types use
 * the `*Spec<T>()` builders.
 */
@Immutable
data class FitMotion(
    val snappyDamping: Float = 0.7f,
    val snappyStiffness: Float = Spring.StiffnessHigh,
    val bouncyDamping: Float = 0.45f,
    val bouncyStiffness: Float = Spring.StiffnessMedium,
    val smoothDamping: Float = 1f,
    val smoothStiffness: Float = Spring.StiffnessMediumLow,
    val gentleDamping: Float = 0.9f,
    val gentleStiffness: Float = Spring.StiffnessLow,
) {
    val snappy: SpringSpec<Float> get() = snappySpec()
    val bouncy: SpringSpec<Float> get() = bouncySpec()
    val smooth: SpringSpec<Float> get() = smoothSpec()
    val gentle: SpringSpec<Float> get() = gentleSpec()

    fun <T> snappySpec(): SpringSpec<T> = spring(snappyDamping, snappyStiffness)
    fun <T> bouncySpec(): SpringSpec<T> = spring(bouncyDamping, bouncyStiffness)
    fun <T> smoothSpec(): SpringSpec<T> = spring(smoothDamping, smoothStiffness)
    fun <T> gentleSpec(): SpringSpec<T> = spring(gentleDamping, gentleStiffness)
}
