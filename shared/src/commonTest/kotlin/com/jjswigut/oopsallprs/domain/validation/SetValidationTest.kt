package com.jjswigut.oopsallprs.domain.validation

import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.instant
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SetValidationTest {
    @Test
    fun bodyweightRepsOnlySetIsValid() {
        val now = instant(1_000)
        val set = ExerciseSet(FoundationId("set"), FoundationId("exercise"), OrderedPosition(0), SetKind.BODYWEIGHT, null, 10, now, now, now)
        assertNull(set.validateForLogging())
    }

    @Test
    fun weightedSetRequiresWeight() {
        val now = instant(1_000)
        val set = ExerciseSet(FoundationId("set"), FoundationId("exercise"), OrderedPosition(0), SetKind.WEIGHTED, null, 10, now, now, now)
        assertNotNull(set.validateForLogging())
    }

    @Test
    fun weightedSetWithFractionalLoadIsValid() {
        val now = instant(1_000)
        val set = ExerciseSet(FoundationId("set"), FoundationId("exercise"), OrderedPosition(0), SetKind.WEIGHTED, WeightKg(22.5), 8, now, now, now)
        assertNull(set.validateForLogging())
    }

    @Test
    fun timedSetRequiresPositiveDuration() {
        val now = instant(1_000)
        val invalid = ExerciseSet(FoundationId("set"), FoundationId("exercise"), OrderedPosition(0), SetKind.TIMED, null, null, now, now, now)
        val valid = invalid.copy(durationMs = 60_000L)

        assertNotNull(invalid.validateForLogging())
        assertNull(valid.validateForLogging())
    }
}
