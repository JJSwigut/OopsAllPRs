package com.jjswigut.oopsallprs.ds.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme

/**
 * Oversized hero CTA. Accent gradient fill with a slow pulsing glow (paused under reduce-motion)
 * and a heavy press haptic.
 */
@Composable
fun FitStartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = FitTheme.shapes.pillShape
    val accent = FitTheme.colors.accent
    val glow = FitTheme.colors.accentGlow
    val reduceMotion = FitTheme.reduceMotion

    val pulse by if (reduceMotion) {
        rememberStatic(0.4f)
    } else {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
            label = "pulseAlpha",
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { role = Role.Button }
            .drawBehind {
                drawRoundRect(
                    color = glow.copy(alpha = pulse),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .background(
                brush = Brush.verticalGradient(
                    0f to lerp(accent, Color.White, 0.15f),
                    1f to accent,
                ),
                shape = shape,
            )
            .pressable(enabled = enabled, haptic = HapticType.Heavy, pressedScale = 0.97f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = FitTheme.type.title.copy(
                color = FitTheme.colors.onAccent,
                textAlign = TextAlign.Center
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberStatic(value: Float): State<Float> = remember { mutableFloatStateOf(value) }
