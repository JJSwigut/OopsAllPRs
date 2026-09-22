package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.EvidenceConfidence
import com.jjswigut.oopsallprs.domain.model.EvidenceCoverage
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ProgressionSummary
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressSummaryLabelTest {
    private val building = ProgressionSummary(
        reading = EvidenceReading.CAPABILITY,
        state = ProgressTrendState.BUILDING_TREND,
        changePercent = null,
        confidence = EvidenceConfidence.LOW,
        coverage = EvidenceCoverage(6, 35, 2),
        comparisonWindow = null,
        evidence = emptyList(),
        explanation = "Configurations differ"
    )

    @Test
    fun sufficientButIncomparableHistoryDoesNotAskForMoreHistory() {
        assertEquals("Need comparable workouts", building.displayStateLabel())
    }

    @Test
    fun sparseHistoryAndEstablishedTrendsKeepTheirOwnLabels() {
        assertEquals("Building a trend", building.copy(coverage = EvidenceCoverage(3, 14, 1)).displayStateLabel())
        assertEquals("Holding steady", building.copy(state = ProgressTrendState.HOLDING_STEADY).displayStateLabel())
    }
}
