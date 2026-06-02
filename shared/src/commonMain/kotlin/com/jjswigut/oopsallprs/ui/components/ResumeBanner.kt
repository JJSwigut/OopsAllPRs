package com.jjswigut.oopsallprs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationActionButton
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.navigation.ActiveWorkoutResume

@Composable
fun ResumeBanner(
    resume: ActiveWorkoutResume,
    onResume: () -> Unit,
    onDiscard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    FitCard(modifier = modifier.fillMaxWidth(), glow = FitTheme.glow.low) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FoundationText(
                        text = resume.displayTitle,
                        style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                    )
                    FoundationMutedText(text = resume.statusText())
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onDiscard?.let {
                    FitButton(
                        text = "Discard",
                        onClick = it,
                        modifier = Modifier.weight(1f),
                        style = FitButtonStyle.Secondary
                    )
                }
                FoundationActionButton(
                    label = "Resume",
                    onClick = onResume,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun ActiveWorkoutResume.statusText(): String {
    val rest = if (restRemainingMillis > 0L) {
        " | Rest ${formatRest(restRemainingMillis)}"
    } else {
        ""
    }
    return "Elapsed ${formatElapsed(elapsedMillis)}$rest"
}

private fun formatElapsed(milliseconds: Long): String {
    val totalMinutes = (milliseconds / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun formatRest(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
