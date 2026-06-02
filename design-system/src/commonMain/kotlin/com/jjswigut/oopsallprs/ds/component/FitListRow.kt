package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/** Large, glass-friendly row. Tappable when [onClick] is provided; height >= touchMin. */
@Composable
fun FitListRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val clickable = if (onClick != null) {
        Modifier.pressable(haptic = HapticType.Light, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = FitTheme.size.touchMin)
            .then(clickable)
            .padding(horizontal = FitTheme.spacing.lg, vertical = FitTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
        content = content,
    )
}
