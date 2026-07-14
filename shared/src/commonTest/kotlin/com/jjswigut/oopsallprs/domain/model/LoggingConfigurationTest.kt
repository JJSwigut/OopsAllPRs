package com.jjswigut.oopsallprs.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class LoggingConfigurationTest {
    @Test
    fun repsOnlyPushUpDisablesLoadByAbsence() {
        val configuration = configuration(
            id = "push-up",
            measures = listOf(required(MeasureKind.REPETITIONS))
        )

        assertEquals(listOf(MeasureKind.REPETITIONS), configuration.measures.map(MeasureSpec::kind))
        assertNull(configuration.measures.single().loadRole)
    }

    @Test
    fun bulgarianSplitSquatSupportsOptionalAddedWeight() {
        val configuration = configuration(
            id = "bulgarian-split-squat",
            measures = listOf(
                required(MeasureKind.REPETITIONS),
                optionalLoad(LoadRole.ADDED_TO_BODYWEIGHT)
            )
        )

        assertEquals(MeasureRequirement.OPTIONAL, configuration.measures[1].requirement)
        assertEquals(LoadRole.ADDED_TO_BODYWEIGHT, configuration.measures[1].loadRole)
    }

    @Test
    fun assistedPullUpKeepsAssistanceDistinctFromResistance() {
        val configuration = configuration(
            id = "assisted-pull-up",
            measures = listOf(
                required(MeasureKind.REPETITIONS),
                requiredLoad(LoadRole.ASSISTANCE)
            )
        )

        assertEquals(LoadRole.ASSISTANCE, configuration.measures[1].loadRole)
    }

    @Test
    fun weightedPullUpUsesLoadAddedToBodyweight() {
        val configuration = configuration(
            id = "weighted-pull-up",
            measures = listOf(
                required(MeasureKind.REPETITIONS),
                requiredLoad(LoadRole.ADDED_TO_BODYWEIGHT)
            )
        )

        assertEquals(LoadRole.ADDED_TO_BODYWEIGHT, configuration.measures[1].loadRole)
    }

    @Test
    fun plankRequiresDurationInCanonicalMilliseconds() {
        val configuration = configuration(
            id = "plank",
            measures = listOf(required(MeasureKind.DURATION))
        )

        assertEquals(CanonicalMeasureUnit.MILLISECONDS, configuration.measures.single().kind.canonicalUnit)
    }

    @Test
    fun farmerCarryPreservesDistanceLoadDurationOrderAndRequirements() {
        val configuration = configuration(
            id = "farmer-carry",
            measures = listOf(
                required(MeasureKind.DISTANCE),
                requiredLoad(LoadRole.EXTERNAL_RESISTANCE),
                MeasureSpec(MeasureKind.DURATION, MeasureRequirement.OPTIONAL)
            )
        )

        assertEquals(
            listOf(MeasureKind.DISTANCE, MeasureKind.LOAD, MeasureKind.DURATION),
            configuration.measures.map(MeasureSpec::kind)
        )
        assertEquals(
            listOf(MeasureRequirement.REQUIRED, MeasureRequirement.REQUIRED, MeasureRequirement.OPTIONAL),
            configuration.measures.map(MeasureSpec::requirement)
        )
    }

    @Test
    fun twoRirIsOptionalObservedEvidence() {
        val configuration = configuration(
            id = "rir-enabled",
            measures = listOf(required(MeasureKind.REPETITIONS)),
            observedEffort = ObservedEffortSpec(listOf(EffortKind.RIR))
        )
        val effort = Effort(rir = 2)

        configuration.requireCanCapture(null)
        configuration.requireCanCapture(effort)

        assertEquals(2, effort.rir)
        assertEquals(setOf(EffortKind.RIR), effort.kinds)
    }

    @Test
    fun observedFailureDoesNotManufactureRpeOrRir() {
        val configuration = configuration(
            id = "failure-enabled",
            measures = listOf(required(MeasureKind.REPETITIONS)),
            observedEffort = ObservedEffortSpec(listOf(EffortKind.FAILURE_OUTCOME))
        )
        val effort = Effort(failureOutcome = FailureOutcome.REACHED)

        configuration.requireCanCapture(effort)

        assertEquals(FailureOutcome.REACHED, effort.failureOutcome)
        assertNull(effort.rpeTenths)
        assertNull(effort.rir)
    }

    @Test
    fun plannedEffortTargetIsDistinctFromObservedEffort() {
        val target: EffortTarget = EffortTarget.Rir(2)
        val observed = Effort(rir = 2)

        assertEquals(EffortTargetKind.RIR, target.kind)
        assertEquals(2, observed.rir)
    }

    @Test
    fun workoutOverrideWinsResolutionWithoutMutatingDefaults() {
        val definition = configuration("definition", listOf(required(MeasureKind.REPETITIONS)))
        val user = configuration(
            "user",
            listOf(required(MeasureKind.REPETITIONS), optionalLoad(LoadRole.ADDED_TO_BODYWEIGHT))
        )
        val workoutOverride = configuration("workout-override", listOf(required(MeasureKind.DURATION)))

        val resolved = LoggingConfigurationResolver.resolve(definition, user, workoutOverride)

        assertSame(workoutOverride, resolved.configuration)
        assertEquals(LoggingConfigurationSource.WORKOUT_OVERRIDE, resolved.source)
        assertEquals(listOf(MeasureKind.REPETITIONS), definition.measures.map(MeasureSpec::kind))
    }

    @Test
    fun userDefaultWinsWhenWorkoutOverrideIsAbsent() {
        val definition = configuration("definition", listOf(required(MeasureKind.REPETITIONS)))
        val user = configuration("user", listOf(required(MeasureKind.DISTANCE)))

        val resolved = LoggingConfigurationResolver.resolve(definition, user)

        assertSame(user, resolved.configuration)
        assertEquals(LoggingConfigurationSource.USER_DEFAULT, resolved.source)
    }

    @Test
    fun definitionDefaultIsResolutionFallback() {
        val definition = configuration("definition", listOf(required(MeasureKind.REPETITIONS)))

        val resolved = LoggingConfigurationResolver.resolve(definition)

        assertSame(definition, resolved.configuration)
        assertEquals(LoggingConfigurationSource.DEFINITION_DEFAULT, resolved.source)
    }

    @Test
    fun configurationSnapshotsMutableInputList() {
        val measures = mutableListOf(required(MeasureKind.REPETITIONS))
        val configuration = configuration("immutable", measures)

        measures += requiredLoad(LoadRole.EXTERNAL_RESISTANCE)

        assertEquals(listOf(MeasureKind.REPETITIONS), configuration.measures.map(MeasureSpec::kind))
    }

    @Test
    fun wireCodesAreStableAndDecodeWithoutEnumNames() {
        assertEquals("reps", MeasureKind.REPETITIONS.wireCode.value)
        assertEquals(MeasureKind.REPETITIONS, MeasureKind.fromWireCode("reps"))
        assertEquals(
            listOf(
                "external_resistance",
                "assistance",
                "added_to_bodyweight",
                "legacy_unspecified"
            ),
            LoadRole.entries.map { it.wireCode.value }
        )
        assertEquals(LoadRole.ADDED_TO_BODYWEIGHT, LoadRole.fromWireCode("added_to_bodyweight"))
        assertEquals(FailureOutcome.REACHED, FailureOutcome.fromWireCode("reached"))
        assertEquals(
            LoggingConfigurationSource.WORKOUT_OVERRIDE,
            LoggingConfigurationSource.fromWireCode("workout_override")
        )
        assertNull(LoggingConfigurationSource.fromWireCode("workout_snapshot"))
        assertNull(MeasureKind.fromWireCode("REPETITIONS"))
    }

    @Test
    fun wireCodesAreUniqueWithinEachContractNamespace() {
        assertUniqueWireCodes(MeasureKind.entries)
        assertUniqueWireCodes(CanonicalMeasureUnit.entries)
        assertUniqueWireCodes(MeasureRequirement.entries)
        assertUniqueWireCodes(LoadRole.entries)
        assertUniqueWireCodes(EffortKind.entries)
        assertUniqueWireCodes(FailureOutcome.entries)
        assertUniqueWireCodes(EffortTargetKind.entries)
        assertUniqueWireCodes(LoggingConfigurationSource.entries)
    }

    @Test
    fun duplicateMeasuresAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            configuration(
                id = "duplicate",
                measures = listOf(
                    required(MeasureKind.REPETITIONS),
                    MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.OPTIONAL)
                )
            )
        }
    }

    @Test
    fun invalidMeasureAndEffortShapesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            MeasureSpec(MeasureKind.LOAD, MeasureRequirement.REQUIRED)
        }
        assertFailsWith<IllegalArgumentException> {
            MeasureSpec(
                MeasureKind.DURATION,
                MeasureRequirement.REQUIRED,
                LoadRole.EXTERNAL_RESISTANCE
            )
        }
        assertFailsWith<IllegalArgumentException> { ObservedEffortSpec(emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            ObservedEffortSpec(listOf(EffortKind.RIR, EffortKind.RIR))
        }
        assertFailsWith<IllegalArgumentException> { Effort() }
    }

    @Test
    fun invalidSchemaAndEffortRangesAreRejected() {
        assertFailsWith<IllegalArgumentException> { LoggingSchemaVersion(0) }
        assertFailsWith<IllegalArgumentException> { configuration("empty", emptyList()) }
        assertFailsWith<IllegalArgumentException> { Effort(rpeTenths = 9) }
        assertFailsWith<IllegalArgumentException> { Effort(rpeTenths = 101) }
        assertFailsWith<IllegalArgumentException> { Effort(rir = -1) }
        assertFailsWith<IllegalArgumentException> { Effort(rir = 11) }
        assertFailsWith<IllegalArgumentException> { EffortTarget.Rpe(0) }
        assertFailsWith<IllegalArgumentException> { EffortTarget.Rir(11) }
    }

    @Test
    fun invalidWireCodesAreRejected() {
        assertFailsWith<IllegalArgumentException> { WireCode("RPE") }
        assertFailsWith<IllegalArgumentException> { WireCode("added__weight") }
        assertFailsWith<IllegalArgumentException> { WireCode("2_rir") }
    }

    @Test
    fun effortKindsMustBeEnabledByConfiguration() {
        val configuration = configuration(
            id = "rir-only",
            measures = listOf(required(MeasureKind.REPETITIONS)),
            observedEffort = ObservedEffortSpec(listOf(EffortKind.RIR))
        )

        assertFailsWith<IllegalArgumentException> {
            configuration.requireCanCapture(Effort(failureOutcome = FailureOutcome.REACHED))
        }
        assertFailsWith<IllegalArgumentException> {
            configuration(
                id = "effort-disabled",
                measures = listOf(required(MeasureKind.REPETITIONS))
            ).requireCanCapture(Effort(rir = 2))
        }
    }

    private fun configuration(
        id: String,
        measures: List<MeasureSpec>,
        observedEffort: ObservedEffortSpec? = null
    ): LoggingConfiguration = LoggingConfiguration(
        id = LoggingConfigurationId(id),
        schemaVersion = LoggingSchemaVersion(1),
        measures = measures,
        observedEffort = observedEffort
    )

    private fun required(kind: MeasureKind): MeasureSpec =
        MeasureSpec(kind, MeasureRequirement.REQUIRED)

    private fun requiredLoad(role: LoadRole): MeasureSpec =
        MeasureSpec(MeasureKind.LOAD, MeasureRequirement.REQUIRED, role)

    private fun optionalLoad(role: LoadRole): MeasureSpec =
        MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, role)

    private fun assertUniqueWireCodes(values: Iterable<WireCoded>) {
        val codes = values.map { it.wireCode.value }
        assertEquals(codes.size, codes.distinct().size)
    }
}
