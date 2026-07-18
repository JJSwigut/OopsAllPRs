package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
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

data class MeasureInputUpdate<T>(
    val rawValue: String,
    val value: T?,
    val errorMessage: String? = null
)

data class ExerciseBlockState(
    val exerciseInstanceId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val displayName: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode,
    val loggingConfiguration: LoggingConfiguration,
    val loggingConfigurationSource: LoggingConfigurationSource,
    val canSaveConfigurationAsDefault: Boolean = false,
    val groupId: FoundationId? = null,
    val groupLabel: String? = null,
    val groupRounds: Int? = null,
    val circuitProgress: CircuitProgressView? = null,
    val loadCalculatorKind: LoadCalculatorKind?,
    val position: OrderedPosition,
    val rest: RestConfiguration,
    val loggedRows: List<LoggedSetRow>,
    val draft: SetRowDraft,
    val prFeedback: ActivePrFeedback? = null,
    val inlineError: String? = null
) {
    val tracksAddedWeight: Boolean
        get() = loggingConfiguration.measures.any {
            it.kind == MeasureKind.LOAD && it.loadRole == LoadRole.ADDED_TO_BODYWEIGHT
        }

    val effortKind: EffortKind?
        get() = loggingConfiguration.observedEffort?.kinds?.firstOrNull()
}

data class CircuitProgressView(
    val round: Int,
    val rounds: Int,
    val isComplete: Boolean
)

internal data class ExerciseBlockGroupState(
    val blocks: List<ExerciseBlockState>
) {
    val groupId: FoundationId? = blocks.firstOrNull()?.groupId
    val groupLabel: String? = blocks.firstOrNull()?.groupLabel
    val groupRounds: Int? = blocks.firstOrNull()?.groupRounds
    val isGrouped: Boolean = groupId != null && blocks.size > 1
    val loggedSetCount: Int = blocks.sumOf { it.loggedRows.size }
}

data class SetRowDraft(
    val draftId: FoundationId,
    val exerciseInstanceId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val captureConfigurationId: LoggingConfigurationId = setKind.toFallbackConfiguration().id,
    val loggingConfiguration: LoggingConfiguration = setKind.toFallbackConfiguration(),
    val reps: Int?,
    val weight: WeightKg?,
    val durationMs: Long? = null,
    val distanceMeters: Double? = null,
    val observedEffort: Effort? = null,
    val weightInput: String? = null,
    val distanceInput: String? = null,
    val effortInput: String? = null,
    val timerStartedAt: Instant? = null,
    val previewDurationMs: Long? = null,
    val isPending: Boolean = false,
    val inputError: String? = null,
    val inlineError: String? = null
) {
    val isTimerRunning: Boolean = timerStartedAt != null

    fun effectiveDurationMs(): Long? = previewDurationMs ?: durationMs
}

data class LoggedSetRow(
    val setId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val captureConfigurationId: LoggingConfigurationId = setKind.toFallbackConfiguration().id,
    val loggingConfiguration: LoggingConfiguration? = setKind.toFallbackConfiguration(),
    val reps: Int?,
    val weight: WeightKg?,
    val durationMs: Long? = null,
    val distanceMeters: Double? = null,
    val observedEffort: Effort? = null,
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
    configurations: Map<LoggingConfigurationId, LoggingConfiguration> = emptyMap(),
    saveDefaultExerciseIds: Set<FoundationId> = emptySet(),
    now: Instant? = null,
    errorMessage: String? = null
): ActiveWorkoutView {
    val rawBlocks = exercises.sortedBy { it.position.value }.map { exercise ->
        exercise.toBlock(
            existingDraft = drafts[exercise.id]?.withPreview(now),
            prFeedbackBySetId = prFeedbackBySetId,
            configurations = configurations,
            canSaveConfigurationAsDefault = exercise.id in saveDefaultExerciseIds
        )
    }
    val completedCircuitIds = rawBlocks
        .filter { it.groupId != null }
        .groupBy { it.groupId }
        .filterValues { grouped ->
            val rounds = grouped.firstNotNullOfOrNull { it.groupRounds } ?: return@filterValues false
            grouped.all { block ->
                (0 until rounds).all { round ->
                    block.loggedRows.any { it.position.value == round }
                }
            }
        }
        .keys
    val blocks = rawBlocks.map { block ->
        val rounds = block.groupRounds
        if (block.groupId == null || rounds == null) {
            block
        } else {
            block.copy(
                circuitProgress = CircuitProgressView(
                    round = (block.draft.position.value + 1).coerceIn(1, rounds),
                    rounds = rounds,
                    isComplete = block.groupId in completedCircuitIds
                )
            )
        }
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

internal fun ActiveWorkoutView.exerciseBlockGroups(): List<ExerciseBlockGroupState> =
    exerciseBlocks.toExerciseBlockGroups()

internal fun List<ExerciseBlockState>.toExerciseBlockGroups(): List<ExerciseBlockGroupState> {
    val groups = mutableListOf<ExerciseBlockGroupState>()
    var pendingGroupId: FoundationId? = null
    var pendingBlocks = mutableListOf<ExerciseBlockState>()

    fun flushPendingGroup() {
        if (pendingBlocks.isNotEmpty()) {
            groups += ExerciseBlockGroupState(pendingBlocks.toList())
        }
        pendingGroupId = null
        pendingBlocks = mutableListOf()
    }

    forEach { block ->
        val blockGroupId = block.groupId
        if (blockGroupId == null) {
            flushPendingGroup()
            groups += ExerciseBlockGroupState(listOf(block))
        } else if (pendingGroupId == blockGroupId) {
            pendingBlocks += block
        } else {
            flushPendingGroup()
            pendingGroupId = blockGroupId
            pendingBlocks += block
        }
    }
    flushPendingGroup()
    return groups
}

private fun ActiveExercise.toBlock(
    existingDraft: SetRowDraft?,
    prFeedbackBySetId: Map<FoundationId, ActivePrFeedback>,
    configurations: Map<LoggingConfigurationId, LoggingConfiguration>,
    canSaveConfigurationAsDefault: Boolean
): ExerciseBlockState {
    val activeConfiguration = resolvedLoggingConfiguration.configuration
    val loggedRows = sets
        .filter { it.isLogged }
        .sortedBy { it.position.value }
        .mapNotNull { set ->
            set.loggedAt?.let { loggedAt ->
                val configuration = configurations[set.captureConfigurationId]
                    ?: activeConfiguration.takeIf { it.id == set.captureConfigurationId }
                LoggedSetRow(
                    setId = set.id,
                    position = set.position,
                    setKind = set.setKind,
                    captureConfigurationId = set.captureConfigurationId,
                    loggingConfiguration = configuration,
                    reps = set.reps,
                    weight = set.weight,
                    durationMs = set.durationMs,
                    distanceMeters = set.distanceMeters,
                    observedEffort = set.observedEffort,
                    loggedAt = loggedAt,
                    editedAt = set.editedAt,
                    prFeedback = prFeedbackBySetId[set.id]
                )
            }
        }
    val draft = existingDraft ?: defaultDraft(loggedRows)
    return ExerciseBlockState(
        exerciseInstanceId = id,
        exerciseCatalogId = reference.exerciseCatalogId,
        displayName = reference.displayNameSnapshot,
        isBodyweight = reference.isBodyweight,
        loggingMode = reference.loggingMode,
        loggingConfiguration = activeConfiguration,
        loggingConfigurationSource = resolvedLoggingConfiguration.source,
        canSaveConfigurationAsDefault = canSaveConfigurationAsDefault,
        groupId = groupContext?.groupId,
        groupLabel = groupContext?.label,
        groupRounds = groupContext?.rounds,
        loadCalculatorKind = loadCalculatorKind(reference.equipmentSnapshot, activeConfiguration),
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
    val configuration = resolvedLoggingConfiguration.configuration
    val enabledMeasures = configuration.measures.map { it.kind }.toSet()
    val kind = configuration.legacySetKind(reference.isBodyweight)
    val loadSpec = configuration.measures.firstOrNull { it.kind == MeasureKind.LOAD }
    return SetRowDraft(
        draftId = FoundationId("draft-${id.value}-${sets.size}"),
        exerciseInstanceId = id,
        position = planned?.position ?: OrderedPosition(loggedRows.size),
        setKind = kind,
        captureConfigurationId = configuration.id,
        loggingConfiguration = configuration,
        reps = (planned?.reps ?: last?.reps ?: 5).takeIf { MeasureKind.REPETITIONS in enabledMeasures },
        weight = when {
            loadSpec == null -> null
            loadSpec.requirement == MeasureRequirement.REQUIRED -> planned?.weight ?: last?.weight ?: WeightKg(0.0)
            else -> (planned?.weight ?: last?.weight)?.takeIf { it.value > 0.0 }
        },
        durationMs = (planned?.durationMs ?: last?.durationMs).takeIf { MeasureKind.DURATION in enabledMeasures },
        distanceMeters = (planned?.distanceMeters ?: last?.distanceMeters).takeIf { MeasureKind.DISTANCE in enabledMeasures },
        observedEffort = null
    )
}

private fun SetRowDraft.withPreview(now: Instant?): SetRowDraft {
    val started = timerStartedAt ?: return this
    val elapsed = now?.toEpochMilliseconds()?.minus(started.toEpochMilliseconds())?.coerceAtLeast(0L) ?: 0L
    return copy(previewDurationMs = (durationMs ?: 0L) + elapsed)
}

internal fun LoggingConfiguration.legacySetKind(isBodyweight: Boolean): SetKind =
    when {
        measures.size == 1 && measures.single().kind == MeasureKind.DURATION -> SetKind.TIMED
        isBodyweight -> SetKind.BODYWEIGHT
        else -> SetKind.WEIGHTED
    }

private fun SetKind.toFallbackConfiguration(): LoggingConfiguration =
    LegacyLoggingConfigurations.from(this)
