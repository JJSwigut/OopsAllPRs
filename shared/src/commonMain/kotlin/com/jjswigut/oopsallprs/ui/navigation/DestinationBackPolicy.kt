package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.ui.history.HistoryState
import com.jjswigut.oopsallprs.ui.progress.ProgressState

internal enum class DestinationBackAction {
    CLOSE_PROGRESS_EVIDENCE,
    CLOSE_PROGRESS_READING,
    CLOSE_PROGRESS_EXERCISE,
    CLOSE_PROGRESS_RECENT_TRAINING_RECORDS,
    CLOSE_HISTORY_DETAIL
}

// Tab selections are retained, but only the visible destination may consume Back.
internal fun resolveDestinationBackAction(
    destination: TopLevelDestination,
    progress: ProgressState,
    history: HistoryState
): DestinationBackAction? = when (destination) {
    TopLevelDestination.PROGRESS -> when {
        progress.selectedEvidence != null -> DestinationBackAction.CLOSE_PROGRESS_EVIDENCE
        progress.selectedReading != null -> DestinationBackAction.CLOSE_PROGRESS_READING
        progress.selectedExercise != null -> DestinationBackAction.CLOSE_PROGRESS_EXERCISE
        progress.isRecentTrainingRecordsOpen -> DestinationBackAction.CLOSE_PROGRESS_RECENT_TRAINING_RECORDS
        else -> null
    }
    TopLevelDestination.HISTORY -> if (history.selectedSummary != null) {
        DestinationBackAction.CLOSE_HISTORY_DETAIL
    } else {
        null
    }
    TopLevelDestination.TRAIN,
    TopLevelDestination.PROFILE -> null
}
