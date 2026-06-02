package com.jjswigut.oopsallprs.ds.foundation

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.token.FitGlowLevel

/**
 * The single faux-glass recipe used everywhere:
 *  1. translucent vertical gradient fill (lit from top),
 *  2. luminous hairline border brighter at the top,
 *  3. inner top sheen,
 *  4. cheap outer glow (expanded outline fill — no runtime blur).
 *
 * Pass token values from the call site, e.g.
 * `Modifier.glass(FitTheme.shapes.mediumShape, FitTheme.colors.surfaceGlass,
 *                 FitTheme.colors.border, FitTheme.colors.borderGlow, FitTheme.glow.medium)`.
 */
fun Modifier.glass(
    shape: Shape,
    fill: Color,
    borderColor: Color,
    glowColor: Color,
    glow: FitGlowLevel,
): Modifier = this
    .drawBehind {
        if (glow.alpha > 0f) {
            val spreadPx = glow.spread.toPx() + glow.radius.toPx() * 0.5f
            val outline = shape.createOutline(
                size = Size(size.width + spreadPx * 2, size.height + spreadPx * 2),
                layoutDirection = layoutDirection,
                density = this,
            )
            translate(-spreadPx, -spreadPx) {
                drawOutline(outline = outline, color = glowColor.copy(alpha = glow.alpha))
            }
        }
    }
    .drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            brush = Brush.verticalGradient(
                0f to fill,
                1f to lerp(fill, Color.Black, 0.18f).copy(alpha = fill.alpha),
            ),
        )
    }
    .border(width = 1.dp, brush = topBrightGradient(borderColor, glowColor), shape = shape)
    .clip(shape)
    .drawWithContent {
        drawContent()
        // inner top sheen
        val sheenHeight = size.height * 0.4f
        val sheenOutline = shape.createOutline(
            size = Size(size.width, sheenHeight),
            layoutDirection = layoutDirection,
            density = this,
        )
        drawOutline(
            outline = sheenOutline,
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f),
                1f to Color.Transparent,
                startY = 0f,
                endY = sheenHeight,
            ),
        )
    }

private fun topBrightGradient(base: Color, glow: Color): Brush =
    Brush.verticalGradient(
        0f to glow.copy(alpha = 0.55f),
        0.5f to base,
        1f to base.copy(alpha = base.alpha * 0.6f),
    )
