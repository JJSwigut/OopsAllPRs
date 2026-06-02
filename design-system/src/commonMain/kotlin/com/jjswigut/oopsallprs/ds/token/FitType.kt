package com.jjswigut.oopsallprs.ds.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp

/**
 * Type scale bound to a single [fontFamily]. Numeric styles request tabular figures via
 * [TABULAR] so stat widths never jump. Defaults to the platform font; pass a bundled
 * variable font here (`FitTheme(palette, type = FitType(myFontFamily))`) to brand the system.
 */
@Immutable
data class FitType(
    val fontFamily: FontFamily = FontFamily.Default,
) {
    private val base = TextStyle(fontFamily = fontFamily)

    val displayXL = base.copy(fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR)
    val displayL = base.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR)
    val title = base.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    val body = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val label = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val caption = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)

    /** Stat style: large tabular numerals optimized for smoothly animated counts. */
    val stat = base.copy(
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = TABULAR,
        textMotion = TextMotion.Animated,
    )

    private companion object {
        const val TABULAR = "tnum"
    }
}
