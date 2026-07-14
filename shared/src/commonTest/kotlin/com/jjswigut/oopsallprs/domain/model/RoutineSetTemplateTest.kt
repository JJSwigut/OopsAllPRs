package com.jjswigut.oopsallprs.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoutineSetTemplateTest {
    @Test
    fun distanceTargetSurvivesConstructionAndCopy() {
        val original = template(targetDistanceMeters = 42.5)

        val copied = original.copy(position = OrderedPosition(1))

        assertEquals(42.5, original.targetDistanceMeters)
        assertEquals(42.5, copied.targetDistanceMeters)
        assertEquals(OrderedPosition(1), copied.position)
    }

    @Test
    fun rpeAndRirTargetsSurviveConstructionAndCopy() {
        val rpe = template(effortTarget = EffortTarget.Rpe(rpeTenths = 85))

        val rir = rpe.copy(effortTarget = EffortTarget.Rir(rir = 2))

        assertEquals(EffortTarget.Rpe(85), rpe.effortTarget)
        assertEquals(EffortTarget.Rir(2), rir.effortTarget)
        assertEquals(42.5, rir.targetDistanceMeters)
    }

    @Test
    fun invalidDistanceTargetsAreRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.1).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                template(targetDistanceMeters = invalid)
            }
        }
    }

    @Test
    fun invalidEffortTargetsCannotEnterRoutineSnapshots() {
        assertFailsWith<IllegalArgumentException> { template(effortTarget = EffortTarget.Rpe(0)) }
        assertFailsWith<IllegalArgumentException> { template(effortTarget = EffortTarget.Rpe(101)) }
        assertFailsWith<IllegalArgumentException> { template(effortTarget = EffortTarget.Rir(-1)) }
        assertFailsWith<IllegalArgumentException> { template(effortTarget = EffortTarget.Rir(11)) }
    }

    @Test
    fun legacyConstructionDefaultsNewTargetsToNull() {
        val legacy = RoutineSetTemplate(
            id = FoundationId("routine-set"),
            routineExerciseId = FoundationId("routine-exercise"),
            position = OrderedPosition(0),
            targetWeight = WeightKg(20.0),
            targetReps = 8,
            setKind = SetKind.WEIGHTED
        )

        assertEquals(null, legacy.targetDistanceMeters)
        assertEquals(null, legacy.effortTarget)
    }

    private fun template(
        targetDistanceMeters: Double? = 42.5,
        effortTarget: EffortTarget? = null
    ): RoutineSetTemplate =
        RoutineSetTemplate(
            id = FoundationId("routine-set"),
            routineExerciseId = FoundationId("routine-exercise"),
            position = OrderedPosition(0),
            targetWeight = WeightKg(20.0),
            targetReps = null,
            setKind = SetKind.WEIGHTED,
            targetDistanceMeters = targetDistanceMeters,
            effortTarget = effortTarget
        )
}
