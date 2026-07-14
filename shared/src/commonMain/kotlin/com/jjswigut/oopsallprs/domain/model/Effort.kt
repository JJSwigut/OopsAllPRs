package com.jjswigut.oopsallprs.domain.model

enum class EffortKind(code: String) : WireCoded {
    RPE("rpe"),
    RIR("rir"),
    FAILURE_OUTCOME("failure_outcome");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "effort kind")

        fun fromWireCode(value: String): EffortKind? = byWireCode[value]
    }
}

enum class FailureOutcome(code: String) : WireCoded {
    REACHED("reached"),
    NOT_REACHED("not_reached");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "failure outcome")

        fun fromWireCode(value: String): FailureOutcome? = byWireCode[value]
    }
}

class ObservedEffortSpec(kinds: List<EffortKind>) {
    val kinds: List<EffortKind> = kinds.toList()

    init {
        LoggingContractValidation.requireValidObservedEffort(this.kinds)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is ObservedEffortSpec && kinds == other.kinds

    override fun hashCode(): Int = kinds.hashCode()

    override fun toString(): String = "ObservedEffortSpec(kinds=$kinds)"
}

/** RPE is stored in tenths from 1.0 to 10.0; RIR is stored as a whole number from 0 to 10. */
data class Effort(
    val rpeTenths: Int? = null,
    val rir: Int? = null,
    val failureOutcome: FailureOutcome? = null
) {
    init {
        LoggingContractValidation.requireValidEffort(rpeTenths, rir, failureOutcome)
    }

    val kinds: Set<EffortKind>
        get() = buildSet {
            if (rpeTenths != null) add(EffortKind.RPE)
            if (rir != null) add(EffortKind.RIR)
            if (failureOutcome != null) add(EffortKind.FAILURE_OUTCOME)
        }
}

enum class EffortTargetKind(code: String) : WireCoded {
    RPE("rpe"),
    RIR("rir"),
    TO_FAILURE("to_failure");

    override val wireCode: WireCode = WireCode(code)

    companion object {
        private val byWireCode = LoggingContractValidation.indexByWireCode(entries, "effort target kind")

        fun fromWireCode(value: String): EffortTargetKind? = byWireCode[value]
    }
}

sealed interface EffortTarget {
    val kind: EffortTargetKind

    data class Rpe(val rpeTenths: Int) : EffortTarget {
        init {
            LoggingContractValidation.requireRpeTenths(rpeTenths)
        }

        override val kind: EffortTargetKind = EffortTargetKind.RPE
    }

    data class Rir(val rir: Int) : EffortTarget {
        init {
            LoggingContractValidation.requireRir(rir)
        }

        override val kind: EffortTargetKind = EffortTargetKind.RIR
    }

    data object ToFailure : EffortTarget {
        override val kind: EffortTargetKind = EffortTargetKind.TO_FAILURE
    }
}
