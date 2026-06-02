package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ProgressState(
    val personalRecords: List<PersonalRecord> = emptyList(),
    val progressPoints: List<ProgressPoint> = emptyList(),
    val recentRows: List<ProgressPrRow> = emptyList(),
    val latestPr: ProgressPrRow? = null,
    val exerciseGroups: List<ProgressExerciseGroup> = emptyList(),
    val selectedExerciseId: FoundationId? = null,
    val selectedExercise: ProgressExerciseGroup? = null,
    val selectedChartMetric: ProgressMetric? = null,
    val selectedEvidence: ProgressEvidence? = null,
    val weightUnit: WeightUnit = WeightUnit.POUNDS,
    val emptyMessage: String = "Finish workouts to build PRs here."
)

class ProgressStateHolder(
    private val progress: ProgressRepository,
    private val workouts: WorkoutRepository,
    private val preferences: PreferencesRepository? = null
) {
    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state

    suspend fun refresh() {
        _state.value = buildState(
            selectedExerciseId = _state.value.selectedExerciseId,
            selectedChartMetric = _state.value.selectedChartMetric,
            selectedEvidenceRecordId = _state.value.selectedEvidence?.recordId
        )
    }

    fun selectExercise(exerciseId: FoundationId) {
        val selected = _state.value.exerciseGroups.firstOrNull { it.exerciseCatalogId == exerciseId }
        _state.value = _state.value.copy(
            selectedExerciseId = exerciseId,
            selectedExercise = selected,
            selectedChartMetric = selected?.chart?.selectedMetric,
            selectedEvidence = null
        )
    }

    fun selectChartMetric(metric: ProgressMetric) {
        val selected = _state.value.selectedExercise ?: return
        if (metric !in selected.chart.availableMetrics) return
        val chart = buildProgressChartState(
            points = _state.value.progressPoints,
            records = _state.value.personalRecords,
            exerciseCatalogId = selected.exerciseCatalogId,
            selectedMetric = metric,
            weightUnit = _state.value.weightUnit
        )
        _state.value = _state.value.copy(
            selectedExercise = selected.copy(chart = chart),
            selectedChartMetric = chart.selectedMetric
        )
    }

    fun clearExerciseSelection() {
        _state.value = _state.value.copy(
            selectedExerciseId = null,
            selectedExercise = null,
            selectedChartMetric = null,
            selectedEvidence = null
        )
    }

    suspend fun openEvidence(recordId: FoundationId) {
        _state.value = buildState(
            selectedExerciseId = _state.value.selectedExerciseId,
            selectedChartMetric = _state.value.selectedChartMetric,
            selectedEvidenceRecordId = recordId
        )
    }

    fun clearEvidence() {
        _state.value = _state.value.copy(selectedEvidence = null)
    }

    private suspend fun buildState(
        selectedExerciseId: FoundationId?,
        selectedChartMetric: ProgressMetric?,
        selectedEvidenceRecordId: FoundationId?
    ): ProgressState {
        val records = progress.personalRecords()
        val points = progress.progressPoints()
        val completedWorkouts = workouts.completedWorkouts()
        val unit = preferences?.weightUnit() ?: WeightUnit.POUNDS
        val recentRows = buildRecentPrRows(records, completedWorkouts, unit)
        val groups = buildExerciseGroups(records, points, completedWorkouts, unit)
        val selectedExercise = selectedExerciseId?.let { id ->
            groups.firstOrNull { it.exerciseCatalogId == id }?.let { group ->
                val chart = buildProgressChartState(
                    points = points,
                    records = records,
                    exerciseCatalogId = id,
                    selectedMetric = selectedChartMetric,
                    weightUnit = unit
                )
                group.copy(chart = chart)
            }
        }
        val selectedEvidence = selectedEvidenceRecordId?.let { recordId ->
            records.firstOrNull { it.id == recordId }?.let { record ->
                buildProgressEvidence(
                    record = record,
                    records = records,
                    workouts = completedWorkouts,
                    weightUnit = unit
                )
            }
        }
        return ProgressState(
            personalRecords = records,
            progressPoints = points,
            recentRows = recentRows,
            latestPr = recentRows.firstOrNull(),
            exerciseGroups = groups,
            selectedExerciseId = selectedExerciseId,
            selectedExercise = selectedExercise,
            selectedChartMetric = selectedExercise?.chart?.selectedMetric,
            selectedEvidence = selectedEvidence,
            weightUnit = unit
        )
    }
}
