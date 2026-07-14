package com.jjswigut.oopsallprs.data.export

import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit

const val EXPORT_FORMAT_VERSION: Int = 2

internal fun Long?.durationExportLabel(): String {
    if (this == null) return ""
    val totalSeconds = coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

internal fun PersonalRecord.exportDurationMillis(): String =
    value.takeIf { evidenceMetric() == ProgressEvidenceMetric.LONGEST_DURATION }
        ?.toLong()
        ?.toString()
        .orEmpty()

internal fun PersonalRecord.exportValueLabel(unit: WeightUnit): String = when (evidenceMetric()) {
    ProgressEvidenceMetric.WEIGHT_FOR_REPS -> {
        val weightLabel = weight?.displayValue(unit)?.toString() ?: value.toString()
        val repsLabel = reps?.let { " x $it" }.orEmpty()
        "$weightLabel ${unit.name.lowercase()}$repsLabel"
    }
    ProgressEvidenceMetric.REPS -> "${reps ?: value.toInt()} reps"
    ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX ->
        "${WeightKg(value).displayValue(unit)} ${unit.name.lowercase()}"
    ProgressEvidenceMetric.VOLUME ->
        "${WeightKg(value).displayValue(unit)} ${unit.name.lowercase()} volume"
    ProgressEvidenceMetric.LONGEST_DURATION -> value.toLong().durationExportLabel()
    ProgressEvidenceMetric.LONGEST_DISTANCE -> "$value m"
}

internal fun LoggingConfiguration.loadRoleCode(): String =
    measures.firstOrNull { it.kind == MeasureKind.LOAD }?.loadRole?.wireCode?.value.orEmpty()

internal fun Int?.rpeExportValue(): String = this?.let { (it / 10.0).toString() }.orEmpty()

private fun PersonalRecord.evidenceMetric(): ProgressEvidenceMetric =
    requireNotNull(ProgressEvidenceMetric.fromWireCode(metricCode.value)) {
        "Unknown progress metric code: ${metricCode.value}"
    }
