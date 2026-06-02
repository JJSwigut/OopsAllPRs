package com.jjswigut.oopsallprs.ds.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Semantic haptic intents. Platforms map these to the closest native feedback. */
enum class HapticType { Tick, Light, Medium, Heavy, Success, Warning, Selection }

/** Plays haptic feedback without the caller knowing the platform. */
fun interface HapticFeedback {
    fun perform(type: HapticType)
}

/** No-op fallback used outside FitTheme and when haptics are disabled. */
val NoHaptics = HapticFeedback { }

/** The active haptics engine. Provided by FitTheme. */
val LocalHaptics = staticCompositionLocalOf { NoHaptics }

/** Platform engine factory. Android binds to the current View; iOS uses UIKit generators. */
@Composable
expect fun rememberHapticFeedback(): HapticFeedback
