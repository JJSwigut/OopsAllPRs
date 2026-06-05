package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.datetime.Instant

data class ActiveWorkoutView(
    val workoutId: FoundationId,
    val startedAt: Instant,
    val elapsedMillis: Long = 0L,
    val exerciseBlocks: List<ExerciseBlockState>,
    val primaryAction: ActiveWorkoutPrimaryAction,
    val focus: ActiveWorkoutFocus? = null,
    val activeRest: ActiveRestTimerView? = null,
    val errorMessage: String? = null
)

enum class ActiveWorkoutPrimaryAction {
    ADD_EXERCISE,
    LOG_SET,
    RETRY_FAILED_SET,
    RESUME_DRAFT
}

data class ExerciseBlockState(
    val exerciseInstanceId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val displayName: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode,
    val groupLabel: String? = null,
    val groupRounds: Int? = null,
    val loadCalculatorKind: LoadCalculatorKind?,
    val position: OrderedPosition,
    val rest: RestConfiguration,
    val loggedRows: List<LoggedSetRow>,
    val draft: SetRowDraft,
    val prFeedback: ActivePrFeedback? = null,
    val inlineError: String? = null
)

data class SetRowDraft(
    val draftId: FoundationId,
    val exerciseInstanceId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val reps: Int?,
    val weight: WeightKg?,
    val durationMs: Long? = null,
    val timerStartedAt: Instant? = null,
    val previewDurationMs: Long? = null,
    val isPending: Boolean = false,
    val inlineError: String? = null
) {
    val isTimerRunning: Boolean = timerStartedAt != null

    fun effectiveDurationMs(): Long? = previewDurationMs ?: durationMs
}

data class LoggedSetRow(
    val setId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val reps: Int?,
    val weight: WeightKg?,
    val durationMs: Long? = null,
    val loggedAt: Instant,
    val editedAt: Instant? = null,
    val prFeedback: ActivePrFeedback? = null
)

data class LoggedSetEditDraft(
    val setId: FoundationId,
    val exerciseName: String,
    val rowDraft: SetRowDraft
)

data class ActiveWorkoutFocus(
    val exerciseInstanceId: FoundationId,
    val draftId: FoundationId,
    val updatedAt: Instant
)

data class ActiveRestTimerView(
    val originSetId: FoundationId?,
    val startedAt: Instant?,
    val endsAt: Instant,
    val remainingMillis: Long
) {
    val isComplete: Boolean = remainingMillis <= 0L
}

fun ActiveWorkout.toView(
    drafts: Map<FoundationId, SetRowDraft>,
    focus: ActiveWorkoutFocus?,
    prFeedbackBySetId: Map<FoundationId, ActivePrFeedback> = emptyMap(),
    activeSession: ActiveSessionState? = null,
    now: Instant? = null,
    errorMessage: String? = null
): ActiveWorkoutView {
    val blocks = exercises.sortedBy { it.position.value }.map { exercise ->
        exercise.toBlock(drafts[exercise.id]?.withPreview(now), prFeedbackBySetId)
    }
    val resolvedFocus = focus?.let { candidate ->
        blocks.firstOrNull { it.exerciseInstanceId == candidate.exerciseInstanceId }?.let {
            ActiveWorkoutFocus(it.exerciseInstanceId, it.draft.draftId, candidate.updatedAt)
        }
    } ?: blocks.firstOrNull()?.let {
        ActiveWorkoutFocus(it.exerciseInstanceId, it.draft.draftId, updatedAt)
    }
    val focusedBlock = blocks.firstOrNull { it.exerciseInstanceId == resolvedFocus?.exerciseInstanceId }
    return ActiveWorkoutView(
        workoutId = id,
        startedAt = startedAt,
        elapsedMillis = now?.toEpochMilliseconds()?.minus(startedAt.toEpochMilliseconds())?.coerceAtLeast(0L) ?: 0L,
        exerciseBlocks = blocks,
        primaryAction = when {
            blocks.isEmpty() -> ActiveWorkoutPrimaryAction.ADD_EXERCISE
            focusedBlock?.draft?.inlineError != null -> ActiveWorkoutPrimaryAction.RETRY_FAILED_SET
            focusedBlock?.draft?.isPending == true -> ActiveWorkoutPrimaryAction.LOG_SET
            focusedBlock != null -> ActiveWorkoutPrimaryAction.LOG_SET
            else -> ActiveWorkoutPrimaryAction.RESUME_DRAFT
        },
        focus = resolvedFocus,
        activeRest = activeSession?.toRestTimerView(now),
        errorMessage = errorMessage
    )
}

private fun ActiveExercise.toBlock(
    existingDraft: SetRowDraft?,
    prFeedbackBySetId: Map<FoundationId, ActivePrFeedback>
): ExerciseBlockState {
    val loggedRows = sets
        .filter { it.isLogged }
        .sortedBy { it.position.value }
        .mapNotNull { set ->
            set.loggedAt?.let { loggedAt ->
                LoggedSetRow(set.id, set.position, set.setKind, set.reps, set.weight, set.durationMs, loggedAt, set.editedAt, prFeedbackBySetId[set.id])
            }
        }
    val draft = existingDraft ?: defaultDraft(loggedRows)
    return ExerciseBlockState(
        exerciseInstanceId = id,
        exerciseCatalogId = reference.exerciseCatalogId,
        displayName = reference.displayNameSnapshot,
        isBodyweight = reference.isBodyweight,
        loggingMode = reference.loggingMode,
        groupLabel = groupContext?.label,
        groupRounds = groupContext?.rounds,
        loadCalculatorKind = loadCalculatorKind(reference.equipmentSnapshot, draft.setKind),
        position = position,
        rest = rest,
        loggedRows = loggedRows,
        draft = draft,
        prFeedback = loggedRows.lastOrNull { it.prFeedback != null }?.prFeedback,
        inlineError = draft.inlineError
    )
}

private fun ActiveSessionState.toRestTimerView(now: Instant?): ActiveRestTimerView? {
    val endsAt = restEndsAt ?: return null
    val remaining = now?.let { restRemainingMillis(it) }
        ?: (endsAt.toEpochMilliseconds() - (restStartedAt ?: endsAt).toEpochMilliseconds()).coerceAtLeast(0L)
    return ActiveRestTimerView(
        originSetId = restOriginSetId,
        startedAt = restStartedAt,
        endsAt = endsAt,
        remainingMillis = remaining
    )
}

private fun ActiveExercise.defaultDraft(loggedRows: List<LoggedSetRow>): SetRowDraft {
    val planned = sets.firstOrNull { !it.isLogged }
    val last = loggedRows.lastOrNull()
    val kind = planned?.setKind ?: when (reference.loggingMode) {
        ExerciseLoggingMode.BODYWEIGHT -> SetKind.BODYWEIGHT
        ExerciseLoggingMode.TIMED -> SetKind.TIMED
        ExerciseLoggingMode.WEIGHTED -> SetKind.WEIGHTED
    }
    return SetRowDraft(
        draftId = FoundationId("draft-${id.value}-${sets.size}"),
        exerciseInstanceId = id,
        position = planned?.position ?: OrderedPosition(loggedRows.size),
        setKind = kind,
        reps = (planned?.reps ?: last?.reps ?: 5).takeIf { kind != SetKind.TIMED },
        weight = when (kind) {
            SetKind.BODYWEIGHT -> planned?.weight ?: last?.weight?.takeIf { it.value > 0.0 }
            SetKind.WEIGHTED -> planned?.weight ?: last?.weight ?: WeightKg(0.0)
            SetKind.TIMED -> null
        },
        durationMs = (planned?.durationMs ?: last?.durationMs).takeIf { kind == SetKind.TIMED }
    )
}

private fun SetRowDraft.withPreview(now: Instant?): SetRowDraft {
    val started = timerStartedAt ?: return this
    val elapsed = now?.toEpochMilliseconds()?.minus(started.toEpochMilliseconds())?.coerceAtLeast(0L) ?: 0L
    return copy(previewDurationMs = (durationMs ?: 0L) + elapsed)
}
