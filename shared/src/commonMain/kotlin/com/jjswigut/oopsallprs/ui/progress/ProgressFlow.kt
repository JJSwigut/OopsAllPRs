package com.jjswigut.oopsallprs.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitLineChart
import com.jjswigut.oopsallprs.ds.component.FitLineChartPoint
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction

@Composable
fun ProgressFlow(
    state: ProgressState,
    onSelectExercise: (FoundationId) -> Unit = {},
    onChartMetricSelected: (ProgressEvidenceMetric) -> Unit = {},
    onBackFromExercise: () -> Unit = {},
    onOpenEvidence: (FoundationId) -> Unit = {},
    onCloseEvidence: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val routeKey = state.selectedEvidence?.recordId?.value
        ?: state.selectedExercise?.exerciseCatalogId?.value
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

        state.selectedExercise?.let { exercise ->
            ExerciseDetail(
                group = exercise,
                onChartMetricSelected = onChartMetricSelected,
                onBack = onBackFromExercise,
                onOpenEvidence = onOpenEvidence
            )
            return@Column
        }

        LatestPrHeroCard(
            row = state.latestPr,
            emptyMessage = state.emptyMessage,
            modifier = Modifier.fillMaxWidth(),
            onOpenEvidence = onOpenEvidence
        )
        ExerciseGroupsCard(
            groups = state.exerciseGroups,
            onSelectExercise = onSelectExercise
        )
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
        SectionLabel("Latest PR")
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
                        style = FitTheme.type.displayL.copy(color = FitTheme.colors.accent)
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
    onChartMetricSelected: (ProgressEvidenceMetric) -> Unit,
    onBack: () -> Unit,
    onOpenEvidence: (FoundationId) -> Unit
) {
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
                visibleTrendRows.forEach { trend ->
                    FitListRow(
                        onClick = trend.sourceRecordId?.let { recordId ->
                            { onOpenEvidence(recordId) }
                        }
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                        ) {
                            FoundationText(trend.metricLabel, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText(trend.valueLabel)
                        }
                        trend.sourceRecordId?.let {
                            FoundationText("View", style = FitTheme.type.label.copy(color = FitTheme.colors.accent))
                        }
                    }
                }
            }
        }
    }
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
                    FoundationMutedText("${summary.durationLabel} • ${summary.exerciseCount} exercises • ${summary.setCount} sets")
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
