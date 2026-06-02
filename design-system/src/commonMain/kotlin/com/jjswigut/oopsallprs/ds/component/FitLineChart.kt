package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.theme.FitTheme

data class FitLineChartPoint(
    val value: Double,
    val valueLabel: String,
    val axisLabel: String
)

@Composable
fun FitLineChart(
    points: List<FitLineChartPoint>,
    emptyLabel: String,
    modifier: Modifier = Modifier
) {
    val summary = when {
        points.isEmpty() -> emptyLabel
        points.size == 1 -> "One progress point: ${points.first().valueLabel}"
        else -> "Progress chart from ${points.first().valueLabel} to ${points.last().valueLabel}"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentAlignment = Alignment.Center
        ) {
            if (points.isEmpty()) {
                BasicText(
                    text = emptyLabel,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.onSurfaceMuted)
                )
            } else {
                ChartCanvas(points)
            }
        }
        if (points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FitTheme.spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = points.first().axisLabel,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.onSurfaceMuted)
                )
                BasicText(
                    text = points.last().axisLabel,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.onSurfaceMuted)
                )
            }
        }
    }
}

@Composable
private fun ChartCanvas(points: List<FitLineChartPoint>) {
    val accent = FitTheme.colors.accent
    val muted = FitTheme.colors.onSurfaceMuted
    Canvas(modifier = Modifier.fillMaxWidth().height(168.dp)) {
        val left = 10.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 16.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val chartHeight = (bottom - top).coerceAtLeast(1f)
        val minValue = points.minOf { it.value }
        val maxValue = points.maxOf { it.value }
        val range = (maxValue - minValue).takeIf { it > 0.0001 } ?: 1.0

        repeat(3) { index ->
            val y = top + chartHeight * index.toFloat() / 2f
            drawLine(
                color = muted.copy(alpha = 0.18f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val offsets = points.mapIndexed { index, point ->
            val x = if (points.size == 1) {
                (left + right) / 2f
            } else {
                left + (right - left) * index.toFloat() / (points.lastIndex).toFloat()
            }
            val normalized = ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f)
            Offset(x = x, y = bottom - normalized * chartHeight)
        }

        if (offsets.size > 1) {
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = accent,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        offsets.forEach { offset ->
            drawCircle(
                color = accent.copy(alpha = 0.2f),
                radius = 8.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = accent,
                radius = 4.dp.toPx(),
                center = offset
            )
        }
    }
}
