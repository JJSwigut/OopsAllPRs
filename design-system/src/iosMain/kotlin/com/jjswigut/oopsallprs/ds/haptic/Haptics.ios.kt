package com.jjswigut.oopsallprs.ds.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
actual fun rememberHapticFeedback(): HapticFeedback = remember {
    val selection = UISelectionFeedbackGenerator()
    val notification = UINotificationFeedbackGenerator()
    val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    HapticFeedback { type ->
        when (type) {
            HapticType.Tick, HapticType.Selection -> selection.selectionChanged()
            HapticType.Light -> light.impactOccurred()
            HapticType.Medium -> medium.impactOccurred()
            HapticType.Heavy -> heavy.impactOccurred()
            HapticType.Success ->
                notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            HapticType.Warning ->
                notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
        }
    }
}
