package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressDisplayModelsTest {
    @Test
    fun weightedLabelUsesDisplayUnitWithoutMutatingCanonicalKilograms() {
        val record = progressRecord(
            recordKind = PersonalRecordKind.WEIGHT_FOR_REPS,
            reps = 5,
            weight = WeightKg(100.0),
            value = 100.0
        )

        val row = record.toProgressPrRow("Bench Press", WeightUnit.POUNDS)

        assertEquals("220.5 lb x 5", row.valueLabel)
        assertEquals("5 reps", row.detailLabel)
        assertEquals("1970-01-01", row.achievedDateLabel)
        assertEquals(WeightKg(100.0), record.weight)
    }

    @Test
    fun bodyweightRecordsRenderRepsOnly() {
        val record = progressRecord(
            id = FoundationId("pr-bodyweight"),
            exerciseCatalogId = FoundationId("exercise-pullup"),
            recordKind = PersonalRecordKind.BODYWEIGHT_REPS,
            reps = 12,
            weight = null,
            value = 12.0,
            sourceSetId = FoundationId("set-bodyweight")
        )

        val row = record.toProgressPrRow("Pull-Up", WeightUnit.POUNDS)

        assertEquals("12 reps", row.valueLabel)
        assertEquals("Bodyweight", row.detailLabel)
    }

    @Test
    fun e1rmAndVolumeLabelsUseCanonicalValueForDisplayOnly() {
        val e1rm = progressRecord(
            id = FoundationId("pr-e1rm"),
            recordKind = PersonalRecordKind.ESTIMATED_ONE_REP_MAX,
            reps = 5,
            weight = WeightKg(100.0),
            value = 116.6666
        )
        val volume = progressRecord(
            id = FoundationId("pr-volume"),
            recordKind = PersonalRecordKind.VOLUME,
            reps = 5,
            weight = WeightKg(100.0),
            value = 500.0
        )

        assertEquals("116.7 kg", e1rm.toProgressPrRow("Bench Press", WeightUnit.KILOGRAMS).valueLabel)
        assertEquals("500 kg volume", volume.toProgressPrRow("Bench Press", WeightUnit.KILOGRAMS).valueLabel)
        assertEquals(116.6666, e1rm.value)
        assertEquals(500.0, volume.value)
    }
}
