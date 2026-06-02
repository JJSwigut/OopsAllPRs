package com.jjswigut.oopsallprs.ds.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.ds.foundation.fitGlass
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure, framework-free roller arithmetic. Kept separate so it is unit-tested on the JVM. */
object RollerMath {

    fun values(min: Float, max: Float, step: Float): List<Float> {
        require(step > 0f) { "step must be positive" }
        val out = ArrayList<Float>()
        var i = 0
        var v = min
        while (v <= max + step / 2f) {
            out.add(min + i * step)
            i++
            v = min + i * step
        }
        return out
    }

    fun indexOf(value: Float, min: Float, step: Float): Int =
        ((value - min) / step).roundToInt()

    fun valueAt(index: Int, min: Float, step: Float): Float =
        min + index * step

    fun clampIndex(index: Int, size: Int): Int =
        index.coerceIn(0, (size - 1).coerceAtLeast(0))
}

/**
 * Tactile rolling number picker. Drag up/down; the centered slot is the selected value, and
 * each value crossing center fires a [HapticType.Selection] detent. Fling + snap lands exactly
 * on a notch. Exposed as an adjustable progress node for accessibility.
 */
@Composable
fun FitRoller(
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
    unit: String = "",
    visibleCount: Int = 5,
    format: (Float) -> String = { v -> if (v % 1f == 0f) v.toInt().toString() else v.toString() },
) {
    val values = remember(min, max, step) { RollerMath.values(min, max, step) }
    val haptics = FitTheme.haptics
    val itemHeight = 64.dp
    val halfWindow = visibleCount / 2
    val listState = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(Unit) {
        listState.scrollToItem(RollerMath.clampIndex(RollerMath.indexOf(value, min, step), values.size))
    }

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - center) }
                ?.index ?: RollerMath.indexOf(value, min, step)
        }
    }

    LaunchedEffect(centeredIndex) {
        val v = RollerMath.valueAt(RollerMath.clampIndex(centeredIndex, values.size), min, step)
        if (v != value) {
            haptics.perform(HapticType.Selection)
            onValueChange(v)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FitTheme.size.touchMin)
            .height(itemHeight * visibleCount)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, min..max, ((max - min) / step).toInt())
                stateDescription = "${format(value)} $unit"
            },
        contentAlignment = Alignment.Center,
    ) {
        // Selection slot.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = FitTheme.spacing.md)
                .fitGlass(shape = FitTheme.shapes.mediumShape, glow = FitTheme.glow.medium)
                .background(FitTheme.colors.accent.copy(alpha = 0.10f), FitTheme.shapes.mediumShape),
        )

        LazyColumn(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * halfWindow),
        ) {
            itemsIndexed(values) { index, v ->
                val distance = abs(index - centeredIndex)
                val isCenter = distance == 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha((1f - distance * 0.28f).coerceIn(0.15f, 1f))
                        .scale((1f - distance * 0.12f).coerceIn(0.7f, 1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = format(v),
                        style = (if (isCenter) FitTheme.type.stat else FitTheme.type.title).copy(
                            color = if (isCenter) FitTheme.colors.accent else FitTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

/** Weight picker: defaults to a 2.5-unit step. */
@Composable
fun FitWeightRoller(
    weight: Float,
    onWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 500f,
    step: Float = 2.5f,
    unit: String = "lbs",
) = FitRoller(
    value = weight,
    onValueChange = onWeightChange,
    min = min,
    max = max,
    step = step,
    unit = unit,
    modifier = modifier,
)

/** Rep picker: whole-number steps. */
@Composable
fun FitRepRoller(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 100,
) = FitRoller(
    value = reps.toFloat(),
    onValueChange = { onRepsChange(it.toInt()) },
    min = min.toFloat(),
    max = max.toFloat(),
    step = 1f,
    unit = "reps",
    modifier = modifier,
)
