package com.jjswigut.oopsallprs.ui.accessibility

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jjswigut.oopsallprs.ds.theme.FitTheme

@Composable
fun Modifier.foundationTouchTarget(label: String): Modifier =
    defaultMinSize(minWidth = FitTheme.size.touchMin, minHeight = FitTheme.size.touchMin)
        .semantics { contentDescription = label }

data class MotionPreference(
    val reduceMotion: Boolean = false
)
