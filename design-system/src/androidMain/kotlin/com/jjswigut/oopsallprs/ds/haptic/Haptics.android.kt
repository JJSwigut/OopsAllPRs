package com.jjswigut.oopsallprs.ds.haptic

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    return remember(view) {
        HapticFeedback { type ->
            view.performHapticFeedback(
                when (type) {
                    HapticType.Tick, HapticType.Selection -> HapticFeedbackConstants.CLOCK_TICK
                    HapticType.Light -> HapticFeedbackConstants.KEYBOARD_TAP
                    HapticType.Medium -> HapticFeedbackConstants.VIRTUAL_KEY
                    HapticType.Heavy -> HapticFeedbackConstants.LONG_PRESS
                    HapticType.Success -> HapticFeedbackConstants.CONFIRM
                    HapticType.Warning -> HapticFeedbackConstants.REJECT
                },
            )
        }
    }
}
