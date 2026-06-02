package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitIconButton
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

fun nextPositiveInt(current: Int?, delta: Int): Int =
    ((current ?: 0) + delta).coerceAtLeast(1)

fun nextWeightKg(current: WeightKg?, delta: Double): WeightKg =
    nextDisplayWeight(current, delta, WeightUnit.KILOGRAMS)

fun formatWeightKg(weight: WeightKg?): String =
    formatDisplayWeight(weight, WeightUnit.KILOGRAMS)

fun nextDisplayWeight(current: WeightKg?, delta: Double, unit: WeightUnit): WeightKg {
    val nextDisplay = ((current?.displayValue(unit) ?: 0.0) + delta).coerceAtLeast(0.0)
    return WeightKg.fromDisplay(nextDisplay, unit)
}

fun formatDisplayWeight(weight: WeightKg?, unit: WeightUnit): String =
    weight?.displayValue(unit)?.formatCompact().orEmpty()

fun weightUnitLabel(unit: WeightUnit): String =
    when (unit) {
        WeightUnit.KILOGRAMS -> "kg"
        WeightUnit.POUNDS -> "lb"
    }

fun weightStep(unit: WeightUnit): Double =
    WeightStepPreference.defaultFor(unit)

fun nextDurationMs(current: Long?, deltaMs: Long): Long =
    ((current ?: 0L) + deltaMs).coerceAtLeast(0L)

fun parseDurationInput(raw: String): Long? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (":" !in value) {
        return value.toLongOrNull()?.takeIf { it >= 0L }?.times(1_000L)
    }
    val parts = value.split(":").map { it.trim() }
    if (parts.any { it.isEmpty() }) return null
    val numbers = parts.map { it.toLongOrNull() ?: return null }
    val seconds = when (numbers.size) {
        2 -> numbers[0] * 60L + numbers[1]
        3 -> numbers[0] * 3_600L + numbers[1] * 60L + numbers[2]
        else -> return null
    }
    return seconds.takeIf { it >= 0L }?.times(1_000L)
}

fun formatDurationMs(durationMs: Long?): String {
    val totalSeconds = ((durationMs ?: 0L) / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

@Composable
fun CompactSetInput(
    draft: SetRowDraft,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onDurationChange: (Long?) -> Unit = {},
    onTimerToggle: () -> Unit = {},
    onLog: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    weightStepAmount: Double = weightStep(weightUnit),
    actionLabel: String = "Log set",
    pendingLabel: String = "Logging...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            if (draft.setKind == SetKind.TIMED) {
                TimedDurationInput(
                    draft = draft,
                    onDurationChange = onDurationChange,
                    onTimerToggle = onTimerToggle,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                NumericStepper(
                    label = "Reps",
                    value = draft.reps?.toString().orEmpty(),
                    onDecrease = { onRepsChange(nextPositiveInt(draft.reps, -1)) },
                    onIncrease = { onRepsChange(nextPositiveInt(draft.reps, 1)) },
                    keyboardType = KeyboardType.Number,
                    onTextChange = { raw ->
                        raw.toIntOrNull()?.takeIf { it > 0 }?.let(onRepsChange)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (draft.setKind == SetKind.WEIGHTED || draft.weight != null) {
                    NumericStepper(
                        label = "Weight ${weightUnitLabel(weightUnit)}",
                        value = formatDisplayWeight(draft.weight, weightUnit),
                        onDecrease = { onWeightChange(nextDisplayWeight(draft.weight, -weightStepAmount, weightUnit)) },
                        onIncrease = { onWeightChange(nextDisplayWeight(draft.weight, weightStepAmount, weightUnit)) },
                        keyboardType = KeyboardType.Decimal,
                        onTextChange = { raw ->
                            raw.toDoubleOrNull()
                                ?.takeIf { it >= 0.0 }
                                ?.let { onWeightChange(WeightKg.fromDisplay(it, weightUnit)) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        draft.inlineError?.let { error ->
            FoundationText(
                text = error,
                style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
            )
        }
        FitButton(
            text = when {
                draft.isPending -> pendingLabel
                draft.inlineError != null -> "Retry"
                else -> actionLabel
            },
            onClick = onLog,
            modifier = Modifier.fillMaxWidth(),
            enabled = !draft.isPending,
            style = FitButtonStyle.Primary
        )
        if (draft.setKind == SetKind.BODYWEIGHT && draft.weight == null) {
            FoundationMutedText("Reps only")
        } else if (draft.setKind == SetKind.TIMED) {
            FoundationMutedText("Time only")
        }
    }
}

@Composable
private fun TimedDurationInput(
    draft: SetRowDraft,
    onDurationChange: (Long?) -> Unit,
    onTimerToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        NumericStepper(
            label = "Time",
            value = formatDurationMs(draft.effectiveDurationMs()),
            onDecrease = { onDurationChange(nextDurationMs(draft.effectiveDurationMs(), -15_000L)) },
            onIncrease = { onDurationChange(nextDurationMs(draft.effectiveDurationMs(), 15_000L)) },
            keyboardType = KeyboardType.Number,
            onTextChange = { raw -> parseDurationInput(raw)?.let(onDurationChange) },
            modifier = Modifier.fillMaxWidth()
        )
        FitButton(
            text = if (draft.isTimerRunning) "Stop timer" else "Start timer",
            onClick = onTimerToggle,
            modifier = Modifier.fillMaxWidth(),
            style = FitButtonStyle.Secondary
        )
    }
}

@Composable
private fun NumericStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    keyboardType: KeyboardType,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoundationMutedText(label, modifier = Modifier.weight(0.85f))
            FitIconButton(
                onClick = onDecrease,
                contentDescription = "Decrease $label"
            ) {
                BasicText("-", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
            }
            FitTextField(
                value = value,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1.4f),
                placeholder = label,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                selectAllOnFocus = true
            )
            FitIconButton(
                onClick = onIncrease,
                contentDescription = "Increase $label"
            ) {
                BasicText("+", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
            }
        }
    }
}

private fun Double.formatCompact(): String {
    val oneDecimal = round(this * 10.0) / 10.0
    val whole = oneDecimal.roundToInt()
    return if (abs(oneDecimal - whole.toDouble()) < 0.0001) whole.toString() else oneDecimal.toString()
}
