package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.EvidenceLadderSnapshot
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ProgressionSummary
import com.jjswigut.oopsallprs.domain.model.RecentTrainingReview
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.usecase.EvidenceLadderUseCase
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.domain.usecase.RecentTrainingReviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

data class ProgressState(
    val personalRecords: List<PersonalRecord> = emptyList(),
    val progressPoints: List<ProgressPoint> = emptyList(),
    val recentRows: List<ProgressPrRow> = emptyList(),
    val latestPr: ProgressPrRow? = null,
    val isRecentTrainingRecordsOpen: Boolean = false,
    val exerciseGroups: List<ProgressExerciseGroup> = emptyList(),
    val selectedExerciseId: FoundationId? = null,
    val selectedExercise: ProgressExerciseGroup? = null,
    val selectedChartMetric: ProgressEvidenceMetric? = null,
    val selectedEvidence: ProgressEvidence? = null,
    val evidenceLadder: EvidenceLadderSnapshot? = null,
    val overallReadings: List<ProgressionSummary> = emptyList(),
    val recentTrainingReview: RecentTrainingReview? = null,
    val selectedReading: ProgressionSummary? = null,
    val weightUnit: WeightUnit = WeightUnit.POUNDS,
    val emptyMessage: String = "Finish workouts to build PRs here."
)

class ProgressStateHolder(
    private val progress: ProgressRepository,
    private val workouts: WorkoutRepository,
    private val preferences: PreferencesRepository? = null,
    private val personalRecordDerivation: PersonalRecordDerivationUseCase? = null,
    private val evidenceLadderUseCase: EvidenceLadderUseCase = EvidenceLadderUseCase(),
    private val loggingConfigurations: LoggingConfigurationRepository? = null,
    private val recentTrainingReviewUseCase: RecentTrainingReviewUseCase = RecentTrainingReviewUseCase(),
    private val clock: () -> kotlinx.datetime.Instant = { Clock.System.now() },
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state

    suspend fun refresh() {
        _state.value = buildState(
            selectedExerciseId = _state.value.selectedExerciseId,
            selectedChartMetric = _state.value.selectedChartMetric,
            selectedEvidenceRecordId = _state.value.selectedEvidence?.recordId,
            selectedReading = _state.value.selectedReading?.reading,
            isRecentTrainingRecordsOpen = _state.value.isRecentTrainingRecordsOpen
        )
    }

    fun selectExercise(exerciseId: FoundationId) {
        val selected = _state.value.exerciseGroups.firstOrNull { it.exerciseCatalogId == exerciseId }
        _state.value = _state.value.copy(
            selectedExerciseId = exerciseId,
            selectedExercise = selected,
            selectedChartMetric = selected?.chart?.selectedMetric,
            selectedEvidence = null,
            selectedReading = null,
            isRecentTrainingRecordsOpen = false
        )
    }

    fun selectChartMetric(metric: ProgressEvidenceMetric) {
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
            selectedEvidenceRecordId = recordId,
            selectedReading = _state.value.selectedReading?.reading,
            isRecentTrainingRecordsOpen = _state.value.isRecentTrainingRecordsOpen
        )
    }

    fun clearEvidence() {
        _state.value = _state.value.copy(selectedEvidence = null)
    }

    fun selectReading(reading: EvidenceReading) {
        _state.value = _state.value.copy(
            selectedReading = _state.value.overallReadings.firstOrNull { it.reading == reading },
            selectedEvidence = null,
            selectedExerciseId = null,
            selectedExercise = null,
            isRecentTrainingRecordsOpen = false
        )
    }

    fun clearReading() {
        _state.value = _state.value.copy(selectedReading = null)
    }

    fun openRecentTrainingRecords() {
        if (_state.value.recentTrainingReview?.personalRecords.isNullOrEmpty()) return
        _state.value = _state.value.copy(
            isRecentTrainingRecordsOpen = true,
            selectedEvidence = null,
            selectedReading = null,
            selectedExerciseId = null,
            selectedExercise = null,
            selectedChartMetric = null
        )
    }

    fun clearRecentTrainingRecords() {
        _state.value = _state.value.copy(isRecentTrainingRecordsOpen = false)
    }

    private suspend fun buildState(
        selectedExerciseId: FoundationId?,
        selectedChartMetric: ProgressEvidenceMetric?,
        selectedEvidenceRecordId: FoundationId?,
        selectedReading: EvidenceReading?,
        isRecentTrainingRecordsOpen: Boolean
    ): ProgressState {
        val completedWorkouts = workouts.completedWorkouts()
        personalRecordDerivation?.rebuildFrom(completedWorkouts)
        val records = progress.personalRecords()
        val points = progress.progressPoints()
        val unit = preferences?.weightUnit() ?: WeightUnit.POUNDS
        val ladder = evidenceLadderUseCase.project(
            completedWorkouts,
            records,
            points,
            loggingConfigurations = loggingConfigurations?.loggingConfigurations() ?: LegacyLoggingConfigurations.all
        )
        val recentTrainingReview = recentTrainingReviewUseCase.project(
            workouts = completedWorkouts,
            personalRecords = records,
            progressReadings = ladder.overallReadings,
            now = clock(),
            timeZone = timeZone()
        )
        val recentRows = buildRecentPrRows(records, completedWorkouts, unit)
        val groups = buildExerciseGroups(
            records,
            points,
            completedWorkouts,
            unit,
            ladder.exerciseProgressions.associate { it.exerciseCatalogId to it.capability }
        )
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
            isRecentTrainingRecordsOpen = isRecentTrainingRecordsOpen && recentTrainingReview.personalRecords.isNotEmpty(),
            exerciseGroups = groups,
            selectedExerciseId = selectedExerciseId,
            selectedExercise = selectedExercise,
            selectedChartMetric = selectedExercise?.chart?.selectedMetric,
            selectedEvidence = selectedEvidence,
            evidenceLadder = ladder,
            overallReadings = ladder.overallReadings,
            recentTrainingReview = recentTrainingReview,
            selectedReading = selectedReading?.let { reading -> ladder.overallReadings.firstOrNull { it.reading == reading } },
            weightUnit = unit
        )
    }
}
