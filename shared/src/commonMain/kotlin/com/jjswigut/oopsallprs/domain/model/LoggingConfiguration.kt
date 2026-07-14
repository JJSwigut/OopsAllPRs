package com.jjswigut.oopsallprs.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class WireCode(val value: String) {
    init {
        LoggingContractValidation.requireValidWireCode(value)
    }

    override fun toString(): String = value
}

interface WireCoded {
    val wireCode: WireCode
}

@JvmInline
value class LoggingConfigurationId(val value: String) {
    init {
        LoggingContractValidation.requireConfigurationId(value)
    }

    override fun toString(): String = value
}

@JvmInline
value class LoggingSchemaVersion(val value: Int) {
    init {
        LoggingContractValidation.requireSchemaVersion(value)
    }
}

enum class CanonicalMeasureUnit(code: String) : WireCoded {
    COUNT("count"),
    KILOGRAMS("kilograms"),
    MILLISECONDS("milliseconds"),
    METERS("meters");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "canonical measure unit")

        fun fromWireCode(value: String): CanonicalMeasureUnit? = byWireCode[value]
    }
}

enum class MeasureKind(
    code: String,
    val canonicalUnit: CanonicalMeasureUnit
) : WireCoded {
    REPETITIONS("reps", CanonicalMeasureUnit.COUNT),
    LOAD("load", CanonicalMeasureUnit.KILOGRAMS),
    DURATION("duration", CanonicalMeasureUnit.MILLISECONDS),
    DISTANCE("distance", CanonicalMeasureUnit.METERS);

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "measure kind")

        fun fromWireCode(value: String): MeasureKind? = byWireCode[value]
    }
}

/** A measure is disabled by omitting its [MeasureSpec], rather than by another requirement value. */
enum class MeasureRequirement(code: String) : WireCoded {
    REQUIRED("required"),
    OPTIONAL("optional");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "measure requirement")

        fun fromWireCode(value: String): MeasureRequirement? = byWireCode[value]
    }
}

/**
 * [EXTERNAL_RESISTANCE] is the movement's primary external load, such as a barbell or carried weight.
 * [ADDED_TO_BODYWEIGHT] is extra load applied to a movement whose base performance uses bodyweight.
 */
enum class LoadRole(code: String) : WireCoded {
    EXTERNAL_RESISTANCE("external_resistance"),
    ASSISTANCE("assistance"),
    ADDED_TO_BODYWEIGHT("added_to_bodyweight"),
    LEGACY_UNSPECIFIED("legacy_unspecified");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "load role")

        fun fromWireCode(value: String): LoadRole? = byWireCode[value]
    }
}

data class MeasureSpec(
    val kind: MeasureKind,
    val requirement: MeasureRequirement,
    val loadRole: LoadRole? = null
) {
    init {
        LoggingContractValidation.requireValidMeasureSpec(kind, loadRole)
    }
}

class LoggingConfiguration(
    val id: LoggingConfigurationId,
    val schemaVersion: LoggingSchemaVersion,
    measures: List<MeasureSpec>,
    val observedEffort: ObservedEffortSpec? = null
) {
    val measures: List<MeasureSpec> = measures.toList()

    init {
        LoggingContractValidation.requireValidConfiguration(this.measures)
    }

    fun requireCanCapture(effort: Effort?) {
        if (effort == null) return
        val effortSpec = requireNotNull(observedEffort) {
            "Observed effort is disabled for logging configuration $id"
        }
        LoggingContractValidation.requireSupportedEffort(effortSpec.kinds, effort.kinds)
    }

    fun copy(
        id: LoggingConfigurationId = this.id,
        schemaVersion: LoggingSchemaVersion = this.schemaVersion,
        measures: List<MeasureSpec> = this.measures,
        observedEffort: ObservedEffortSpec? = this.observedEffort
    ): LoggingConfiguration = LoggingConfiguration(id, schemaVersion, measures, observedEffort)

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LoggingConfiguration &&
            id == other.id &&
            schemaVersion == other.schemaVersion &&
            measures == other.measures &&
            observedEffort == other.observedEffort

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + schemaVersion.hashCode()
        result = 31 * result + measures.hashCode()
        result = 31 * result + (observedEffort?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "LoggingConfiguration(id=$id, schemaVersion=$schemaVersion, measures=$measures, " +
            "observedEffort=$observedEffort)"
}

enum class LoggingConfigurationSource(code: String) : WireCoded {
    DEFINITION_DEFAULT("definition_default"),
    USER_DEFAULT("user_default"),
    WORKOUT_OVERRIDE("workout_override");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "configuration source")

        fun fromWireCode(value: String): LoggingConfigurationSource? = byWireCode[value]
    }
}

data class ResolvedLoggingConfiguration(
    val configuration: LoggingConfiguration,
    val source: LoggingConfigurationSource
)

object LoggingConfigurationResolver {
    fun resolve(
        definitionDefault: LoggingConfiguration,
        userDefault: LoggingConfiguration? = null,
        workoutOverride: LoggingConfiguration? = null
    ): ResolvedLoggingConfiguration =
        when {
            workoutOverride != null -> ResolvedLoggingConfiguration(
                configuration = workoutOverride,
                source = LoggingConfigurationSource.WORKOUT_OVERRIDE
            )
            userDefault != null -> ResolvedLoggingConfiguration(
                configuration = userDefault,
                source = LoggingConfigurationSource.USER_DEFAULT
            )
            else -> ResolvedLoggingConfiguration(
                configuration = definitionDefault,
                source = LoggingConfigurationSource.DEFINITION_DEFAULT
            )
        }
}

internal object LoggingContractValidation {
    private const val MIN_RPE_TENTHS = 10
    private const val MAX_RPE_TENTHS = 100
    private const val MIN_RIR = 0
    private const val MAX_RIR = 10

    fun requireConfigurationId(value: String) {
        require(value.isNotBlank()) { "Logging configuration ID cannot be blank" }
    }

    fun requireSchemaVersion(value: Int) {
        require(value > 0) { "Logging schema version must be greater than zero" }
    }

    fun requireValidWireCode(value: String) {
        val segments = value.split('_')
        require(
            segments.all { segment ->
                segment.isNotEmpty() &&
                    segment.first() in 'a'..'z' &&
                    segment.all { it in 'a'..'z' || it in '0'..'9' }
            }
        ) {
            "Wire code must contain lowercase alphanumeric segments separated by single underscores"
        }
    }

    fun requireValidMeasureSpec(kind: MeasureKind, loadRole: LoadRole?) {
        if (kind == MeasureKind.LOAD) {
            requireNotNull(loadRole) { "Load measures must declare a load role" }
        } else {
            require(loadRole == null) { "Only load measures may declare a load role" }
        }
    }

    fun requireValidConfiguration(measures: List<MeasureSpec>) {
        require(measures.isNotEmpty()) { "Logging configuration must contain at least one measure" }
        requireNoDuplicates(measures.map(MeasureSpec::kind), "measure kinds")
    }

    fun requireValidObservedEffort(kinds: List<EffortKind>) {
        require(kinds.isNotEmpty()) { "Observed effort specification must contain at least one kind" }
        requireNoDuplicates(kinds, "observed effort kinds")
    }

    fun requireValidEffort(rpeTenths: Int?, rir: Int?, failureOutcome: FailureOutcome?) {
        require(rpeTenths != null || rir != null || failureOutcome != null) {
            "Observed effort must contain at least one value"
        }
        if (rpeTenths != null) requireRpeTenths(rpeTenths)
        if (rir != null) requireRir(rir)
    }

    fun requireRpeTenths(value: Int) {
        require(value in MIN_RPE_TENTHS..MAX_RPE_TENTHS) {
            "RPE tenths must be between $MIN_RPE_TENTHS and $MAX_RPE_TENTHS"
        }
    }

    fun requireRir(value: Int) {
        require(value in MIN_RIR..MAX_RIR) {
            "RIR must be between $MIN_RIR and $MAX_RIR"
        }
    }

    fun requireSupportedEffort(configured: List<EffortKind>, observed: Set<EffortKind>) {
        val unsupported = observed - configured.toSet()
        require(unsupported.isEmpty()) {
            "Observed effort contains disabled kinds: ${unsupported.joinToString()}"
        }
    }

    fun <T : WireCoded> indexByWireCode(values: Iterable<T>, label: String): Map<String, T> {
        val indexed = values.associateBy { it.wireCode.value }
        require(indexed.size == values.count()) { "Duplicate $label wire codes" }
        return indexed
    }

    private fun <T> requireNoDuplicates(values: List<T>, label: String) {
        require(values.distinct().size == values.size) { "Logging configuration contains duplicate $label" }
    }
}
