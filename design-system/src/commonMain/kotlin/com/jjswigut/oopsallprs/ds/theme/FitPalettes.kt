package com.jjswigut.oopsallprs.ds.theme

import androidx.compose.ui.graphics.Color
import com.jjswigut.oopsallprs.ds.token.FitColors

/** Reference signature palettes. New palettes (e.g. "Ember") are added as more entries here. */
object FitPalettes {

    val IceDark = FitPalette(
        name = "Ice Dark",
        isDark = true,
        colors = FitColors(
            background = Color(0xFF06080F),
            surface = Color(0xFF0E1422),
            surfaceGlass = Color(0x1AFFFFFF),   // ~10% white, composited over surface
            border = Color(0x1FFFFFFF),         // ~12% white
            borderGlow = Color(0x665EE7FF),     // ~40% cyan
            onSurface = Color(0xFFEAF2FF),
            onSurfaceMuted = Color(0xFF9DB2D6),
            accent = Color(0xFF5EE7FF),
            accentGlow = Color(0xFF5EE7FF),
            onAccent = Color(0xFF04121A),
            success = Color(0xFF4ADE80),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFFB7185),
        ),
    )

    val IceLight = FitPalette(
        name = "Ice Light",
        isDark = false,
        colors = FitColors(
            background = Color(0xFFF4F7FC),
            surface = Color(0xFFFFFFFF),
            surfaceGlass = Color(0xB3FFFFFF),   // ~70% white frosted glass
            border = Color(0x1F0A1B2A),         // ~12% ink
            borderGlow = Color(0x66067E92),     // ~40% deep cyan
            onSurface = Color(0xFF0E1422),
            onSurfaceMuted = Color(0xFF51607A),
            accent = Color(0xFF067E92),         // deepened so it reads on light
            accentGlow = Color(0xFF22B8CF),
            onAccent = Color(0xFFFFFFFF),
            success = Color(0xFF15803D),
            warning = Color(0xFFB45309),
            danger = Color(0xFFBE123C),
        ),
    )
}
