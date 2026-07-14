package com.jjswigut.oopsallprs.domain.model

import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Instant

enum class SetKind {
    WEIGHTED,
    BODYWEIGHT,
    TIMED
}

data class ExerciseSet(
    val id: FoundationId,
    val exerciseInstanceId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val weight: WeightKg?,
    val reps: Int?,
    val loggedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val editedAt: Instant? = null,
    val durationMs: Long? = null,
    val captureConfigurationId: LoggingConfigurationId = setKind
        .toLegacyLoggingConfiguration(hasLegacyLoad = setKind == SetKind.BODYWEIGHT && weight != null)
        .id,
    val distanceMeters: Double? = null,
    val observedEffort: Effort? = null
) {
    val isLogged: Boolean = loggedAt != null

    fun validateForLogging(configuration: LoggingConfiguration? = null): FoundationError? =
        if (configuration == null) validateLegacyShape() else validateAgainst(configuration)

    private fun validateLegacyShape(): FoundationError? =
        when {
            setKind == SetKind.TIMED && (durationMs == null || durationMs <= 0L) -> FoundationError.Validation("Timed sets require a positive duration")
            setKind != SetKind.TIMED && (reps == null || reps <= 0) -> FoundationError.Validation("Logged sets require positive reps")
            setKind == SetKind.WEIGHTED && weight == null -> FoundationError.Validation("Weighted sets require a weight")
            setKind == SetKind.WEIGHTED && weight != null && weight.value < 0.0 -> FoundationError.Validation("Weighted sets cannot have negative weight")
            setKind == SetKind.BODYWEIGHT && weight != null && weight.value < 0.0 -> FoundationError.Validation("Bodyweight added load cannot be negative")
            else -> null
        }

    private fun validateAgainst(configuration: LoggingConfiguration): FoundationError? {
        if (captureConfigurationId != configuration.id) {
            return FoundationError.Validation("Set capture configuration does not match the persisted configuration")
        }
        val measuresByKind = configuration.measures.associateBy(MeasureSpec::kind)
        configuration.measures.forEach { measure ->
            val error = when (measure.kind) {
                MeasureKind.REPETITIONS -> validatePositiveMeasure("Repetitions", reps?.toDouble(), measure.requirement)
                MeasureKind.LOAD -> validateLoad(
                    value = weight,
                    requirement = measure.requirement,
                    loadRole = requireNotNull(measure.loadRole)
                )
                MeasureKind.DURATION -> validatePositiveMeasure("Duration", durationMs?.toDouble(), measure.requirement)
                MeasureKind.DISTANCE -> validatePositiveMeasure("Distance", distanceMeters, measure.requirement)
            }
            if (error != null) return error
        }
        if (reps != null && MeasureKind.REPETITIONS !in measuresByKind) {
            return FoundationError.Validation("Repetitions are disabled for this logging configuration")
        }
        if (weight != null && MeasureKind.LOAD !in measuresByKind) {
            return FoundationError.Validation("Load is disabled for this logging configuration")
        }
        if (durationMs != null && MeasureKind.DURATION !in measuresByKind) {
            return FoundationError.Validation("Duration is disabled for this logging configuration")
        }
        if (distanceMeters != null && MeasureKind.DISTANCE !in measuresByKind) {
            return FoundationError.Validation("Distance is disabled for this logging configuration")
        }
        return try {
            configuration.requireCanCapture(observedEffort)
            null
        } catch (error: IllegalArgumentException) {
            FoundationError.Validation(error.message ?: "Observed effort is invalid")
        }
    }

    private fun validatePositiveMeasure(
        label: String,
        value: Double?,
        requirement: MeasureRequirement
    ): FoundationError? =
        when {
            value == null && requirement == MeasureRequirement.REQUIRED -> FoundationError.Validation("$label is required")
            value != null && (!value.isFinite() || value <= 0.0) -> FoundationError.Validation("$label must be positive")
            else -> null
        }

    private fun validateLoad(
        value: WeightKg?,
        requirement: MeasureRequirement,
        loadRole: LoadRole
    ): FoundationError? =
        validateLoadMagnitude(value?.value, requirement, loadRole)
}

internal fun validateLoadMagnitude(
    value: Double?,
    requirement: MeasureRequirement,
    loadRole: LoadRole
): FoundationError? {
    val label = when (loadRole) {
        LoadRole.EXTERNAL_RESISTANCE -> "External resistance"
        LoadRole.ASSISTANCE -> "Assistance load"
        LoadRole.ADDED_TO_BODYWEIGHT -> "Added bodyweight load"
        LoadRole.LEGACY_UNSPECIFIED -> "Load"
    }
    return when {
        value == null && requirement == MeasureRequirement.REQUIRED -> FoundationError.Validation("$label is required")
        value != null && !value.isFinite() -> FoundationError.Validation("$label must be finite")
        value != null && value < 0.0 -> FoundationError.Validation("$label cannot be negative")
        else -> null
    }
}
