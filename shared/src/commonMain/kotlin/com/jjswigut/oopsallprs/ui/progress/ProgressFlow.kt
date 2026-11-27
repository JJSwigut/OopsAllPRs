package com.jjswigut.oopsallprs.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick as semanticsOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.ProgressionSummary
import com.jjswigut.oopsallprs.domain.model.RecentTrainingReview
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitLineChart
import com.jjswigut.oopsallprs.ds.component.FitLineChartPoint
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.haptic.HapticType
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import com.jjswigut.oopsallprs.ui.common.countLabel

@Composable
fun ProgressFlow(
    state: ProgressState,
    onSelectExercise: (FoundationId) -> Unit = {},
    onChartMetricSelected: (ProgressEvidenceMetric) -> Unit = {},
    onBackFromExercise: () -> Unit = {},
    onOpenEvidence: (FoundationId) -> Unit = {},
    onCloseEvidence: () -> Unit = {},
    onSelectReading: (EvidenceReading) -> Unit = {},
    onCloseReading: () -> Unit = {},
    onOpenWorkout: (FoundationId) -> Unit = {},
    onOpenTrain: () -> Unit = {},
    onOpenRecentTrainingRecords: () -> Unit = {},
    onCloseRecentTrainingRecords: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val routeKey = state.selectedEvidence?.recordId?.value
        ?: state.selectedReading?.reading?.name
        ?: state.selectedExercise?.exerciseCatalogId?.value
        ?: if (state.isRecentTrainingRecordsOpen) "recent-training-records" else null
        ?: "overview"
    LaunchedEffect(routeKey) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        state.selectedEvidence?.let { evidence ->
            EvidenceDetail(evidence = evidence, onBack = onCloseEvidence)
            return@Column
        }

        state.selectedReading?.let { reading ->
            ReadingDetail(reading, state.weightUnit, onCloseReading, onOpenWorkout)
            return@Column
        }

        state.selectedExercise?.let { exercise ->
            ExerciseDetail(
                group = exercise,
                weightUnit = state.weightUnit,
                onChartMetricSelected = onChartMetricSelected,
                onBack = onBackFromExercise,
                onOpenEvidence = onOpenEvidence,
                onOpenWorkout = onOpenWorkout
            )
            return@Column
        }

        if (state.isRecentTrainingRecordsOpen) {
            state.recentTrainingReview?.let { review ->
                RecentTrainingRecordsDetail(
                    review = review,
                    weightUnit = state.weightUnit,
                    onBack = onCloseRecentTrainingRecords,
                    onOpenEvidence = onOpenEvidence
                )
            }
            return@Column
        }

        FoundationText(
            "Progress",
            modifier = Modifier.semantics { heading() },
            style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
        )
        LatestPrHeroCard(
            row = state.latestPr,
            emptyMessage = state.emptyMessage,
            modifier = Modifier.fillMaxWidth(),
            onOpenEvidence = onOpenEvidence
        )
        RecentTrainingCard(
            review = state.recentTrainingReview,
            onOpenTrain = onOpenTrain,
            onOpenRecords = onOpenRecentTrainingRecords
        )
        OverallEvidenceCard(state.overallReadings, onSelectReading)
        ExerciseGroupsCard(
            groups = state.exerciseGroups,
            onSelectExercise = onSelectExercise
        )
    }
}

@Composable
private fun RecentTrainingCard(
    review: RecentTrainingReview?,
    onOpenTrain: () -> Unit,
    onOpenRecords: () -> Unit
) {
    FitCard(modifier = Modifier.fillMaxWidth(), glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Recent training")
            if (review == null || review.completedWorkoutCount == 0) {
                FoundationText("No completed workouts in the last 7 days.", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText("Your history and progress are still here.")
                FitButton(
                    text = "Plan next workout",
                    onClick = onOpenTrain,
                    modifier = Modifier.fillMaxWidth(),
                    style = FitButtonStyle.Primary
                )
            } else {
                FoundationText(review.window.label, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText(review.factsLabel())
                if (review.personalRecords.isNotEmpty()) {
                    FoundationText(
                        "${review.personalRecords.size} new ${if (review.personalRecords.size == 1) "record" else "records"}",
                        style = FitTheme.type.label.copy(color = FitTheme.colors.accent)
                    )
                    FoundationTextAction("View records", onOpenRecords)
                }
                FoundationTextAction("Train", onOpenTrain)
            }
        }
    }
}

@Composable
private fun RecentTrainingRecordsDetail(
    review: RecentTrainingReview,
    weightUnit: WeightUnit,
    onBack: () -> Unit,
    onOpenEvidence: (FoundationId) -> Unit
) {
    val rows = buildRecentPrRows(review.personalRecords, review.completedWorkouts, weightUnit)
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
        ProgressDetailHeader("Recent records", review.window.label, onBack)
        FoundationMutedText(review.factsLabel())
        FitCard(glow = FitTheme.glow.none) {
            SectionLabel("New records")
            rows.forEach { row ->
                FitListRow(onClick = { onOpenEvidence(row.recordId) }) {
                    Column(modifier = Modifier.weight(1f)) {
                        FoundationText(row.exerciseName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                        FoundationMutedText("${row.kindLabel} • ${row.valueLabel}")
                    }
                    FoundationText("View", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                }
            }
        }
    }
}

@Composable
private fun LatestPrHeroCard(
    row: ProgressPrRow?,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onOpenEvidence: (FoundationId) -> Unit
) {
    FitCard(modifier = modifier, glow = FitTheme.glow.none) {
        SectionLabel("Recent achievement")
        if (row == null) {
            FoundationMutedText(emptyMessage)
        } else {
            FitListRow(onClick = { onOpenEvidence(row.recordId) }) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                ) {
                    FoundationText(
                        row.valueLabel,
                        style = FitTheme.type.title.copy(color = FitTheme.colors.accent)
                    )
                    FoundationText(row.exerciseName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                    FoundationMutedText("${row.kindLabel} • ${row.achievedDateLabel}")
                }
                FoundationText("View", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
            }
        }
    }
}

@Composable
private fun ExerciseGroupsCard(
    groups: List<ProgressExerciseGroup>,
    onSelectExercise: (FoundationId) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        SectionLabel("Exercises")
        if (groups.isEmpty()) {
            FoundationMutedText("Exercises appear here after completed workouts set PRs.")
        } else {
            groups.forEach { group ->
                FitListRow(onClick = { onSelectExercise(group.exerciseCatalogId) }) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                    ) {
                        FoundationText(group.exerciseName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                        FoundationMutedText(
                            group.latestRecord?.let { "Latest ${it.valueLabel} • ${it.achievedDateLabel}" }
                                ?: "${group.trendRows.size} trend points"
                        )
                    }
                    FoundationText("View", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetail(
    group: ProgressExerciseGroup,
    weightUnit: WeightUnit,
    onChartMetricSelected: (ProgressEvidenceMetric) -> Unit,
    onBack: () -> Unit,
    onOpenEvidence: (FoundationId) -> Unit,
    onOpenWorkout: (FoundationId) -> Unit
) {
    var showCapabilityDetails by remember(group.exerciseCatalogId) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
        ProgressDetailHeader(
            title = group.exerciseName,
            subtitle = group.latestRecord?.let { "Latest PR • ${it.achievedDateLabel}" } ?: "No PRs yet",
            onBack = onBack
        )
        LatestPrHeroCard(
            row = group.latestRecord,
            emptyMessage = "Set PRs on this exercise to build a latest-best summary.",
            onOpenEvidence = onOpenEvidence
        )
        group.capability?.let { summary ->
            FitCard(glow = FitTheme.glow.none) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Capability")
                    InfoButton(
                        expanded = showCapabilityDetails,
                        subject = "capability",
                        onClick = { showCapabilityDetails = !showCapabilityDetails }
                    )
                }
                FoundationText(summary.displayStateLabel(), style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
                EvidenceStrip(summary)
                if (showCapabilityDetails) {
                    FoundationMutedText(summary.explanation)
                    FoundationMutedText(summary.coverage.coverageLabel())
                    summary.comparisonWindow?.let { window ->
                        FoundationMutedText(window.comparisonLabel())
                    }
                    summary.evidence.takeLast(3).forEach { source ->
                        FitListRow(onClick = { onOpenWorkout(source.sourceWorkoutId) }) {
                            Column(modifier = Modifier.weight(1f)) {
                                FoundationText(source.displayValueLabel(weightUnit), style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                                FoundationMutedText(source.recordedAt.shortDateLabel())
                            }
                            FoundationText("Workout", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                        }
                    }
                }
            }
        }
        ProgressChartCard(
            chart = group.chart,
            onMetricSelected = onChartMetricSelected
        )
        FitCard(glow = FitTheme.glow.none) {
            SectionLabel("Trend")
            val visibleTrendRows = group.chart.selectedMetric?.let { metric ->
                group.trendRows.filter { it.metric == metric }
            } ?: group.trendRows
            if (visibleTrendRows.isEmpty()) {
                FoundationMutedText("Trend points will appear as workouts are completed.")
            } else {
                visibleTrendRows.asReversed().forEach { trend ->
                    FitListRow(
                        onClick = {
                            trend.sourceRecordId?.let(onOpenEvidence)
                                ?: onOpenWorkout(trend.sourceWorkoutId)
                        }
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                        ) {
                            FoundationText(trend.metricLabel, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText(trend.valueLabel)
                            FoundationMutedText(trend.recordedAt.shortDateLabel())
                        }
                        FoundationText("View", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallEvidenceCard(
    readings: List<ProgressionSummary>,
    onSelectReading: (EvidenceReading) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        SectionLabel("Overall evidence")
        if (readings.isEmpty()) {
            FoundationMutedText("Finish workouts to build transparent progress readings.")
        } else {
            readings.forEach { summary ->
                FitListRow(onClick = { onSelectReading(summary.reading) }) {
                    Column(modifier = Modifier.weight(1f)) {
                        FoundationText(summary.reading.title(), style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                        FoundationText(summary.displayStateLabel(), style = FitTheme.type.body.copy(color = FitTheme.colors.accent))
                        EvidenceStrip(summary)
                    }
                    FoundationText("View", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                }
            }
        }
    }
}

@Composable
private fun EvidenceStrip(
    summary: ProgressionSummary,
    modifier: Modifier = Modifier
) {
    val accent = FitTheme.colors.accent
    val inactive = FitTheme.colors.onSurfaceMuted.copy(alpha = 0.18f)
    val semanticsLabel = if (summary.state == ProgressTrendState.BUILDING_TREND) {
        "${summary.displayStateLabel()}. ${summary.coverage.qualifyingSessions} of ${summary.coverage.minimumSessions} sessions and " +
            "${summary.coverage.spanDays} of ${summary.coverage.minimumSpanDays} days"
    } else {
        summary.state.label
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .semantics { contentDescription = semanticsLabel }
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        if (summary.state == ProgressTrendState.BUILDING_TREND && !summary.coverage.isMature) {
            drawRoundRect(color = inactive, cornerRadius = radius)
            val sessionProgress = summary.coverage.qualifyingSessions.toFloat() / summary.coverage.minimumSessions
            val dayProgress = summary.coverage.spanDays.toFloat() / summary.coverage.minimumSpanDays
            val progress = minOf(sessionProgress, dayProgress).coerceIn(0f, 1f)
            if (progress > 0f) {
                drawRoundRect(
                    color = accent,
                    size = Size(size.width * progress, size.height),
                    cornerRadius = radius
                )
            }
        } else {
            val gap = 4.dp.toPx()
            val segmentWidth = (size.width - gap * 2f) / 3f
            val activeSegment = when (summary.state) {
                ProgressTrendState.REBUILDING -> 0
                ProgressTrendState.HOLDING_STEADY,
                ProgressTrendState.STEADY -> 1
                ProgressTrendState.RECENT_RANGE_HIGHER -> 2
                ProgressTrendState.BUILDING_TREND -> -1
            }
            repeat(3) { index ->
                drawRoundRect(
                    color = if (index == activeSegment) accent else inactive,
                    topLeft = Offset(index * (segmentWidth + gap), 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
private fun InfoButton(
    expanded: Boolean,
    subject: String,
    onClick: () -> Unit
) {
    val label = if (expanded) "Hide $subject details" else "Show $subject details"
    Box(
        modifier = Modifier
            .sizeIn(minWidth = FitTheme.size.touchMin, minHeight = FitTheme.size.touchMin)
            .pressable(haptic = HapticType.Light, onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = label
                semanticsOnClick(label = label) {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        FoundationText(
            text = if (expanded) "×" else "ⓘ",
            style = FitTheme.type.body.copy(color = FitTheme.colors.accent)
        )
    }
}

@Composable
private fun ReadingDetail(
    summary: ProgressionSummary,
    weightUnit: WeightUnit,
    onBack: () -> Unit,
    onOpenWorkout: (FoundationId) -> Unit
) {
    val sourceSessionCount = summary.evidence.map { it.sourceWorkoutId }.distinct().size

    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
        ProgressDetailHeader(summary.reading.title(), summary.displayStateLabel(), onBack)
        FitCard(glow = FitTheme.glow.none) {
            SectionLabel("How it works")
            FoundationMutedText(summary.explanation)
            FoundationMutedText(summary.coverage.coverageLabel())
            summary.changePercent?.let { change ->
                FoundationMutedText("Change ${change.formatChangePercent()}")
            }
            if (summary.exerciseComparisons.isEmpty()) {
                summary.comparisonWindow?.let { window ->
                    FoundationMutedText(window.comparisonLabel())
                }
            }
        }
        if (summary.exerciseComparisons.isNotEmpty()) {
            FitCard(glow = FitTheme.glow.none) {
                SectionLabel("Exercise comparisons")
                summary.exerciseComparisons.forEach { comparison ->
                    FitListRow {
                        Column(modifier = Modifier.weight(1f)) {
                            FoundationText(comparison.exerciseName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText("Change ${comparison.changePercent.formatChangePercent()}")
                            FoundationMutedText(comparison.comparisonWindow.comparisonLabel())
                        }
                    }
                }
            }
        }
        FitCard(glow = FitTheme.glow.none) {
            SectionLabel("Source workouts")
            FoundationMutedText("$sourceSessionCount qualifying sessions")
            if (summary.evidence.isEmpty()) {
                FoundationMutedText("No qualifying source sessions yet.")
            } else {
                summary.evidence.sortedByDescending { it.recordedAt }.forEach { source ->
                    FitListRow(onClick = { onOpenWorkout(source.sourceWorkoutId) }) {
                        Column(modifier = Modifier.weight(1f)) {
                            FoundationText(source.exerciseName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText("${source.displayValueLabel(weightUnit)} • ${source.recordedAt.shortDateLabel()}")
                        }
                        FoundationText("Workout", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                    }
                }
            }
        }
    }
}

private fun EvidenceReading.title(): String = when (this) {
    EvidenceReading.CAPABILITY -> "Capability"
    EvidenceReading.WORK_CAPACITY -> "Work capacity"
    EvidenceReading.CONSISTENCY -> "Consistency"
}

private fun com.jjswigut.oopsallprs.domain.model.EvidenceCoverage.coverageLabel(): String =
    "${qualifyingSessions.countLabel("session")} • ${spanDays.countLabel("day")} • " +
        contributingExercises.countLabel("exercise")

private fun com.jjswigut.oopsallprs.domain.model.ProgressComparisonWindow.comparisonLabel(): String =
    "Compared ${earlierStart.shortDateLabel()}–${earlierEnd.shortDateLabel()} with " +
        "${recentStart.shortDateLabel()}–${recentEnd.shortDateLabel()}"

private fun Double.formatChangePercent(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded > 0.0) "+$rounded%" else "$rounded%"
}

@Composable
private fun ProgressChartCard(
    chart: ProgressChartState,
    onMetricSelected: (ProgressEvidenceMetric) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Chart")
                chart.latestValueLabel?.let { latest ->
                    FoundationText(latest, style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                }
            }
            if (chart.availableMetrics.size > 1 && chart.selectedMetric != null) {
                FitSegmentedControl(
                    options = chart.availableMetrics.map { it.shortLabel() },
                    selectedIndex = chart.availableMetrics.indexOf(chart.selectedMetric),
                    onSelect = { index -> onMetricSelected(chart.availableMetrics[index]) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                FoundationMutedText(chart.selectedMetric?.label() ?: "Progress")
            }
            FitLineChart(
                points = chart.points.map { point ->
                    FitLineChartPoint(
                        value = point.value,
                        valueLabel = point.valueLabel,
                        axisLabel = point.dateLabel
                    )
                },
                emptyLabel = chart.emptyMessage,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EvidenceDetail(
    evidence: ProgressEvidence,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
        ProgressDetailHeader(
            title = evidence.title,
            subtitle = "${evidence.recordKindLabel} • ${evidence.recordValueLabel} • ${evidence.achievedDateLabel}",
            onBack = onBack
        )
        FitCard(glow = FitTheme.glow.none) {
            SectionLabel("Source")
            FoundationMutedText(evidence.message)
            FoundationMutedText("Achieved ${evidence.achievedDateLabel}")
            if (evidence.isAvailable) {
                evidence.workoutSummary?.let { summary ->
                    FoundationText("Completed workout", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                    FoundationMutedText(
                        "${summary.durationLabel} • ${summary.exerciseCount.countLabel("exercise")} • " +
                            summary.setCount.countLabel("set")
                    )
                }
                evidence.sourceExerciseName?.let { name ->
                    FoundationText(name, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                }
                evidence.sourceSetLabel?.let { label ->
                    FoundationMutedText(label)
                }
            } else {
                FoundationMutedText("Workout ${evidence.sourceWorkoutId.value}")
                FoundationMutedText("Set ${evidence.sourceSetId.value}")
            }
        }
    }
}

@Composable
private fun ProgressDetailHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
        FoundationTextAction("Back", onBack)
        FoundationText(title, style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
        FoundationMutedText(subtitle)
    }
}

@Composable
private fun PrRow(
    row: ProgressPrRow,
    onClick: () -> Unit
) {
    FitListRow(onClick = onClick) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            FoundationText(row.exerciseName, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
            FoundationMutedText("${row.kindLabel} • ${row.detailLabel} • ${row.achievedDateLabel}")
        }
        FoundationText(row.valueLabel, style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
    }
}

@Composable
private fun SectionLabel(text: String) {
    FoundationText(
        text = text,
        style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
    )
}
