package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

enum class LoadCalculatorKind {
    BARBELL,
    DUMBBELL
}

data class BarbellLoadCalculatorState(
    val barWeight: Double,
    val platePairs: Map<Double, Int> = emptyMap()
) {
    fun addPlatePair(plateWeight: Double): BarbellLoadCalculatorState =
        copy(platePairs = platePairs + (plateWeight to ((platePairs[plateWeight] ?: 0) + 1)))

    fun clearPlates(): BarbellLoadCalculatorState =
        copy(platePairs = emptyMap())

    fun totalDisplayWeight(): Double =
        barWeight + platePairs.entries.sumOf { (plateWeight, count) -> plateWeight * count * 2.0 }

    fun toWeightKg(unit: WeightUnit): WeightKg =
        WeightKg.fromDisplay(totalDisplayWeight(), unit)
}

data class DumbbellLoadCalculatorState(
    val selectedWeight: Double? = null
) {
    fun select(weight: Double): DumbbellLoadCalculatorState =
        copy(selectedWeight = weight)

    fun toWeightKg(unit: WeightUnit): WeightKg? =
        selectedWeight?.let { WeightKg.fromDisplay(it, unit) }
}

fun loadCalculatorKind(equipment: String?, setKind: SetKind): LoadCalculatorKind? {
    if (setKind != SetKind.WEIGHTED) return null
    val normalized = equipment?.trim()?.lowercase().orEmpty()
    return when {
        "barbell" in normalized -> LoadCalculatorKind.BARBELL
        "dumbbell" in normalized -> LoadCalculatorKind.DUMBBELL
        else -> null
    }
}

fun loadCalculatorKind(equipment: String?, configuration: LoggingConfiguration): LoadCalculatorKind? {
    val load = configuration.measures.firstOrNull { it.kind == MeasureKind.LOAD } ?: return null
    if (load.loadRole != LoadRole.EXTERNAL_RESISTANCE) return null
    return loadCalculatorKind(equipment, SetKind.WEIGHTED)
}

fun defaultBarbellState(unit: WeightUnit): BarbellLoadCalculatorState =
    BarbellLoadCalculatorState(
        barWeight = when (unit) {
            WeightUnit.KILOGRAMS -> 20.0
            WeightUnit.POUNDS -> 45.0
        }
    )

fun barbellBarPresets(unit: WeightUnit): List<Double> =
    when (unit) {
        WeightUnit.KILOGRAMS -> listOf(20.0, 15.0, 10.0)
        WeightUnit.POUNDS -> listOf(45.0, 35.0, 15.0)
    }

fun barbellPlatePresets(unit: WeightUnit): List<Double> =
    when (unit) {
        WeightUnit.KILOGRAMS -> listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        WeightUnit.POUNDS -> listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    }

fun dumbbellPresets(unit: WeightUnit): List<Double> =
    when (unit) {
        WeightUnit.KILOGRAMS -> listOf(2.5, 5.0, 7.5, 10.0, 12.5, 15.0, 17.5, 20.0, 22.5, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0)
        WeightUnit.POUNDS -> listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0, 100.0)
    }

fun formatLoadValue(value: Double): String {
    val oneDecimal = round(value * 10.0) / 10.0
    val whole = oneDecimal.roundToInt()
    return if (abs(oneDecimal - whole.toDouble()) < 0.0001) whole.toString() else oneDecimal.toString()
}
