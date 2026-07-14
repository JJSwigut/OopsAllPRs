package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitIconButton
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
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
    if (":" !in value) return value.toLongOrNull()?.takeIf { it >= 0L }?.times(1_000L)
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

fun parseRepsInput(raw: String): MeasureInputUpdate<Int> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return MeasureInputUpdate(rawValue = raw, value = null)
    val value = trimmed.toIntOrNull()
        ?: return MeasureInputUpdate(raw, null, "Enter valid reps")
    if (value <= 0) return MeasureInputUpdate(raw, null, "Reps must be positive")
    return MeasureInputUpdate(raw, value)
}

fun parseWeightInput(raw: String, unit: WeightUnit, loadRole: LoadRole): MeasureInputUpdate<WeightKg> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return MeasureInputUpdate(rawValue = raw, value = null)
    val value = trimmed.toDoubleOrNull()
        ?: return MeasureInputUpdate(raw, null, "Enter a valid ${loadRole.inputLabel().lowercase()}")
    if (!value.isFinite()) return MeasureInputUpdate(raw, null, "Load must be finite")
    if (value < 0.0) return MeasureInputUpdate(raw, null, "${loadRole.validationLabel()} cannot be negative")
    return MeasureInputUpdate(raw, WeightKg.fromDisplay(value, unit))
}

fun parseEffortInput(raw: String, kind: EffortKind): MeasureInputUpdate<Effort> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return MeasureInputUpdate(rawValue = raw, value = null)
    return when (kind) {
        EffortKind.RIR -> {
            val value = trimmed.toIntOrNull()
            if (value == null || value !in 0..10) {
                MeasureInputUpdate(raw, null, "RIR must be between 0 and 10")
            } else {
                MeasureInputUpdate(raw, Effort(rir = value))
            }
        }
        EffortKind.RPE -> {
            val value = trimmed.toDoubleOrNull()
            if (value == null || !value.isFinite() || value !in 1.0..10.0) {
                MeasureInputUpdate(raw, null, "RPE must be between 1.0 and 10.0")
            } else {
                MeasureInputUpdate(raw, Effort(rpeTenths = (value * 10.0).roundToInt()))
            }
        }
        EffortKind.FAILURE_OUTCOME -> MeasureInputUpdate(raw, null, "Choose a failure outcome")
    }
}

fun parseDistanceInput(raw: String): MeasureInputUpdate<Double> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return MeasureInputUpdate(rawValue = raw, value = null)
    val value = trimmed.toDoubleOrNull()
        ?: return MeasureInputUpdate(raw, null, "Enter a valid distance")
    if (!value.isFinite() || value <= 0.0) {
        return MeasureInputUpdate(raw, null, "Distance must be positive")
    }
    return MeasureInputUpdate(raw, value)
}

@Composable
fun CompactSetInput(
    draft: SetRowDraft,
    onRepsChange: (Int?) -> Unit,
    onWeightChange: (WeightKg?) -> Unit,
    onDurationChange: (Long?) -> Unit = {},
    onDistanceChange: (Double?) -> Unit = {},
    onDistanceInputChange: (MeasureInputUpdate<Double>) -> Unit = {},
    onWeightInputChange: (MeasureInputUpdate<WeightKg>) -> Unit = {},
    onEffortChange: (MeasureInputUpdate<Effort>) -> Unit = {},
    onTimerToggle: () -> Unit = {},
    onLog: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    weightStepAmount: Double = weightStep(weightUnit),
    loadCalculatorKind: LoadCalculatorKind? = null,
    onOpenLoadCalculator: (() -> Unit)? = null,
    actionLabel: String = "Log set",
    pendingLabel: String = "Logging...",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            draft.loggingConfiguration.measures.forEach { measure ->
                when (measure.kind) {
                    MeasureKind.REPETITIONS -> NumericStepper(
                        label = "Reps",
                        value = draft.reps?.toString().orEmpty(),
                        onDecrease = {
                            if (measure.requirement == MeasureRequirement.OPTIONAL && (draft.reps ?: 0) <= 1) {
                                onRepsChange(null)
                            } else {
                                onRepsChange(nextPositiveInt(draft.reps, -1))
                            }
                        },
                        onIncrease = { onRepsChange(nextPositiveInt(draft.reps, 1)) },
                        keyboardType = KeyboardType.Number,
                        onTextChange = { raw -> onRepsChange(parseRepsInput(raw).value) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MeasureKind.LOAD -> {
                        val loadRole = requireNotNull(measure.loadRole)
                        val label = loadRole.displayLabel(weightUnit)
                        val canOpenCalculator = loadCalculatorKind != null && onOpenLoadCalculator != null
                        NumericStepper(
                            label = label,
                            value = draft.weightInput ?: formatDisplayWeight(draft.weight, weightUnit),
                            onDecrease = {
                                onWeightChange(nextDisplayWeight(draft.weight, -weightStepAmount, weightUnit))
                            },
                            onIncrease = {
                                onWeightChange(nextDisplayWeight(draft.weight, weightStepAmount, weightUnit))
                            },
                            keyboardType = KeyboardType.Decimal,
                            onTextChange = { raw -> onWeightInputChange(parseWeightInput(raw, weightUnit, loadRole)) },
                            placeholder = if (measure.requirement == MeasureRequirement.OPTIONAL) "Optional" else label,
                            modifier = Modifier.fillMaxWidth(),
                            trailingContent = if (canOpenCalculator) {
                                {
                                    FitIconButton(
                                        onClick = { onOpenLoadCalculator?.invoke() },
                                        contentDescription = "Open weight calculator"
                                    ) { CalculatorGlyph() }
                                }
                            } else {
                                null
                            }
                        )
                    }
                    MeasureKind.DURATION -> TimedDurationInput(
                        draft = draft,
                        onDurationChange = onDurationChange,
                        onTimerToggle = onTimerToggle,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MeasureKind.DISTANCE -> NumericStepper(
                        label = "Distance (m)",
                        value = draft.distanceInput ?: draft.distanceMeters?.formatCompact().orEmpty(),
                        onDecrease = {
                            val next = (draft.distanceMeters ?: 0.0) - 5.0
                            onDistanceChange(next.takeIf { it > 0.0 })
                        },
                        onIncrease = { onDistanceChange((draft.distanceMeters ?: 0.0) + 5.0) },
                        keyboardType = KeyboardType.Decimal,
                        onTextChange = { onDistanceInputChange(parseDistanceInput(it)) },
                        placeholder = "Meters",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            draft.loggingConfiguration.observedEffort?.kinds?.firstOrNull()?.let { effortKind ->
                EffortInput(
                    kind = effortKind,
                    draft = draft,
                    onEffortChange = onEffortChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        (draft.inputError ?: draft.inlineError)?.let { error ->
            FoundationText(text = error, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
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
        val measureKinds = draft.loggingConfiguration.measures.map { it.kind }
        if (measureKinds == listOf(MeasureKind.REPETITIONS) && draft.loggingConfiguration.observedEffort == null) {
            FoundationMutedText("Reps only")
        } else if (measureKinds == listOf(MeasureKind.DURATION) && draft.loggingConfiguration.observedEffort == null) {
            FoundationMutedText("Time only")
        }
    }
}

@Composable
private fun EffortInput(
    kind: EffortKind,
    draft: SetRowDraft,
    onEffortChange: (MeasureInputUpdate<Effort>) -> Unit,
    modifier: Modifier = Modifier
) {
    when (kind) {
        EffortKind.RIR, EffortKind.RPE -> {
            val label = if (kind == EffortKind.RIR) "RIR" else "RPE"
            val current = draft.effortInput ?: when (kind) {
                EffortKind.RIR -> draft.observedEffort?.rir?.toString().orEmpty()
                EffortKind.RPE -> draft.observedEffort?.rpeTenths?.let { (it / 10.0).formatCompact() }.orEmpty()
                EffortKind.FAILURE_OUTCOME -> ""
            }
            NumericStepper(
                label = label,
                value = current,
                onDecrease = {
                    val raw = if (kind == EffortKind.RIR) {
                        ((draft.observedEffort?.rir ?: 0) - 1).coerceAtLeast(0).toString()
                    } else {
                        ((draft.observedEffort?.rpeTenths ?: 10) - 5).coerceAtLeast(10)
                            .let { (it / 10.0).formatCompact() }
                    }
                    onEffortChange(parseEffortInput(raw, kind))
                },
                onIncrease = {
                    val raw = if (kind == EffortKind.RIR) {
                        ((draft.observedEffort?.rir ?: -1) + 1).coerceAtMost(10).toString()
                    } else {
                        ((draft.observedEffort?.rpeTenths ?: 5) + 5).coerceAtMost(100)
                            .let { (it / 10.0).formatCompact() }
                    }
                    onEffortChange(parseEffortInput(raw, kind))
                },
                keyboardType = KeyboardType.Decimal,
                onTextChange = { onEffortChange(parseEffortInput(it, kind)) },
                placeholder = "Optional",
                modifier = modifier
            )
        }
        EffortKind.FAILURE_OUTCOME -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            FoundationMutedText("Failure")
            val options = listOf("Not set", "Reached", "Not reached")
            val selected = when (draft.observedEffort?.failureOutcome) {
                null -> 0
                FailureOutcome.REACHED -> 1
                FailureOutcome.NOT_REACHED -> 2
            }
            FitSegmentedControl(
                options = options,
                selectedIndex = selected,
                onSelect = { index ->
                    val effort = when (index) {
                        1 -> Effort(failureOutcome = FailureOutcome.REACHED)
                        2 -> Effort(failureOutcome = FailureOutcome.NOT_REACHED)
                        else -> null
                    }
                    onEffortChange(MeasureInputUpdate(options[index], effort))
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Failure outcome" }
            )
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
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
    modifier: Modifier = Modifier,
    placeholder: String = label,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoundationMutedText(label, modifier = Modifier.weight(0.85f))
            FitIconButton(onClick = onDecrease, contentDescription = "Decrease $label") {
                BasicText("-", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
            }
            FitTextField(
                value = value,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1.4f).semantics { contentDescription = label },
                placeholder = placeholder,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                selectAllOnFocus = true
            )
            FitIconButton(onClick = onIncrease, contentDescription = "Increase $label") {
                BasicText("+", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
            }
            Box(
                modifier = Modifier.sizeIn(
                    minWidth = FitTheme.size.touchMin,
                    minHeight = FitTheme.size.touchMin
                ),
                contentAlignment = Alignment.Center
            ) { trailingContent?.invoke() }
        }
    }
}

@Composable
private fun CalculatorGlyph() {
    val color = FitTheme.colors.onSurface
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.7.dp.toPx())
        val width = size.width
        val height = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(width * 0.18f, height * 0.08f),
            size = Size(width * 0.64f, height * 0.84f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = stroke
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(width * 0.28f, height * 0.18f),
            size = Size(width * 0.44f, height * 0.18f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
            style = stroke
        )
        val left = width * 0.32f
        val right = width * 0.68f
        val top = height * 0.50f
        val bottom = height * 0.76f
        drawLine(color, Offset(left, top), Offset(right, top), strokeWidth = stroke.width)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), strokeWidth = stroke.width)
        drawLine(color, Offset(left, top), Offset(left, bottom), strokeWidth = stroke.width)
        drawLine(color, Offset(width * 0.50f, top), Offset(width * 0.50f, bottom), strokeWidth = stroke.width)
        drawLine(color, Offset(right, top), Offset(right, bottom), strokeWidth = stroke.width)
    }
}

internal fun Double.formatCompact(): String {
    val oneDecimal = round(this * 10.0) / 10.0
    val whole = oneDecimal.roundToInt()
    return if (abs(oneDecimal - whole.toDouble()) < 0.0001) whole.toString() else oneDecimal.toString()
}

internal fun LoadRole.displayLabel(unit: WeightUnit): String =
    "${inputLabel()} (${weightUnitLabel(unit)})"

private fun LoadRole.inputLabel(): String =
    when (this) {
        LoadRole.ADDED_TO_BODYWEIGHT -> "Added weight"
        LoadRole.ASSISTANCE -> "Assistance"
        LoadRole.EXTERNAL_RESISTANCE -> "Weight"
        LoadRole.LEGACY_UNSPECIFIED -> "Weight"
    }

private fun LoadRole.validationLabel(): String =
    when (this) {
        LoadRole.ADDED_TO_BODYWEIGHT -> "Added bodyweight load"
        LoadRole.ASSISTANCE -> "Assistance load"
        LoadRole.EXTERNAL_RESISTANCE -> "External resistance"
        LoadRole.LEGACY_UNSPECIFIED -> "Load"
    }
