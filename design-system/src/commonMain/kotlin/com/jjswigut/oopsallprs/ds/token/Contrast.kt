package com.jjswigut.oopsallprs.ds.token

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** sRGB channel linearization per WCAG 2.x. */
private fun linearize(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

/** Relative luminance (0..1) of an opaque sRGB color, per WCAG 2.x. */
fun relativeLuminance(color: Color): Double {
    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** WCAG contrast ratio between two opaque colors. Range: 1.0 (identical) .. 21.0 (black/white). */
fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val lighter = max(la, lb)
    val darker = min(la, lb)
    return (lighter + 0.05) / (darker + 0.05)
}
