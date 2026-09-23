package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.export.ExportService
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT
import com.jjswigut.oopsallprs.domain.model.FullAccessGate
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessOfferState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.ExportRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.navigation.PaletteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException

const val DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT: Int = FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT

data class ProfileExportResult(
    val type: ExportType,
    val fileName: String,
    val rowCount: Int,
    val weightUnit: WeightUnit
)

data class ProfileBackupStatus(
    val linkedLocation: String = "Not linked",
    val lastSyncLabel: String = "Never",
    val lastOutcomeLabel: String = "No backup linked",
    val conflictSummary: String? = null,
    val privacyLabel: String = "Plain JSON backup is readable by anyone with access to the file.",
    val canBackup: Boolean = false,
    val canSync: Boolean = false,
    val hasConflict: Boolean = false,
    val isLinked: Boolean = false,
    val warning: String? = null
)

data class ProfileFullAccessStatus(
    val access: FullAccessState = FullAccessState(),
    val statusLabel: String = "Free",
    val detailLabel: String = "0 of 10 free workouts used",
    val offerState: FullAccessOfferState = FullAccessOfferState.Loading,
    val completedFreeWorkouts: Int = 0,
    val freeWorkoutLimit: Int = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT,
    val isFreeLimitReached: Boolean = false,
    val hasFullAccess: Boolean = false,
    val isStoreBusy: Boolean = false,
    val error: String? = null,
    val storeMessage: String? = null
) {
    val canPurchase: Boolean
        get() = !hasFullAccess && !isStoreBusy && offerState is FullAccessOfferState.Available
    val offerLabel: String
        get() = (offerState as? FullAccessOfferState.Available)?.offer?.priceLabel.orEmpty()
    val termsLabel: String
        get() = (offerState as? FullAccessOfferState.Available)?.offer?.termsLabel.orEmpty()
}

enum class BackupSetupStep {
    INTRO,
    LOCATION,
    READY
}

data class ProfileState(
    val weightUnit: WeightUnit = WeightUnit.POUNDS,
    val weightStep: Double = WeightStepPreference.DEFAULT_POUNDS_STEP,
    val isWeightStepPickerVisible: Boolean = false,
    val draftWeightStep: Double = WeightStepPreference.DEFAULT_POUNDS_STEP,
    val weightStepError: String? = null,
    val defaultRestSeconds: Int = RestConfiguration.DEFAULT_SECONDS,
    val isDefaultRestPickerVisible: Boolean = false,
    val draftDefaultRestSeconds: Int = RestConfiguration.DEFAULT_SECONDS,
    val defaultRestError: String? = null,
    val restSoundEnabled: Boolean = true,
    val startWorkoutTimerWithFirstSet: Boolean = true,
    val restTimerSurfaceEnabled: Boolean = false,
    val paletteMode: PaletteMode = PaletteMode.DARK,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val isHydrated: Boolean = false,
    val isExporting: Boolean = false,
    val lastExport: ProfileExportResult? = null,
    val exportError: String? = null,
    val backupStatus: ProfileBackupStatus = ProfileBackupStatus(),
    val isBackupBusy: Boolean = false,
    val backupError: String? = null,
    val lastRestoreMessage: String? = null,
    val restoreWarning: String? = null,
    val safetyBackupMessage: String? = null,
    val backupSetupStep: BackupSetupStep? = null,
    val fullAccessStatus: ProfileFullAccessStatus = ProfileFullAccessStatus(),
    val isUnlockDialogVisible: Boolean = false
)

class ProfileStateHolder(
    private val preferences: PreferencesRepository? = null,
    private val exports: ExportRepository? = null,
    private val exportHandoff: ((ExportFile) -> Unit)? = null,
    private val backupSync: BackupSyncCoordinator? = null,
    private val fullAccess: FullAccessUseCases? = null,
    private val onRestTimerSurfacePreferenceChanged: (suspend () -> Unit)? = null
) {
    private val freeWorkoutLimit = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT
    private val _state = MutableStateFlow(ProfileState())
    private val storeMutex = Mutex()
    val state: StateFlow<ProfileState> = _state

    suspend fun hydrate() {
        val unit = preferences?.weightUnit() ?: _state.value.weightUnit
        val step = preferences?.weightStep(unit) ?: _state.value.weightStep
        val restSeconds = preferences?.defaultRestSeconds() ?: _state.value.defaultRestSeconds
        val soundEnabled = preferences?.restSoundEnabled() ?: _state.value.restSoundEnabled
        val startTimerWithFirstSet = preferences?.startWorkoutTimerWithFirstSet()
            ?: _state.value.startWorkoutTimerWithFirstSet
        val surfaceEnabled = preferences?.restTimerSurfaceEnabled() ?: _state.value.restTimerSurfaceEnabled
        val previousAccessStatus = _state.value.fullAccessStatus
        val accessStatus = loadFullAccessStatus()
        val syncState = backupSync?.loadState()
        // Do not replace a store-operation result that arrived during local hydration.
        val latestAccessStatus = if (_state.value.fullAccessStatus === previousAccessStatus) accessStatus
            else _state.value.fullAccessStatus
        _state.value = _state.value.copy(
            weightUnit = unit,
            weightStep = step,
            draftWeightStep = step,
            weightStepError = null,
            defaultRestSeconds = restSeconds,
            isDefaultRestPickerVisible = false,
            draftDefaultRestSeconds = restSeconds,
            defaultRestError = null,
            restSoundEnabled = soundEnabled,
            startWorkoutTimerWithFirstSet = startTimerWithFirstSet,
            restTimerSurfaceEnabled = surfaceEnabled,
            isHydrated = true,
            exportError = null,
            backupStatus = syncState?.toProfileStatus() ?: ProfileBackupStatus(),
            fullAccessStatus = latestAccessStatus,
            isUnlockDialogVisible = !latestAccessStatus.hasFullAccess && _state.value.isUnlockDialogVisible,
            backupError = syncState?.lastError ?: if (latestAccessStatus.hasFullAccess) {
                _state.value.backupError.withoutGateError()
            } else _state.value.backupError
        )
    }

    suspend fun refreshFullAccess() {
        val latestAccessStatus = loadFullAccessStatus()
        val unlocked = latestAccessStatus.hasFullAccess
        val accessChanged = unlocked != _state.value.fullAccessStatus.hasFullAccess
        _state.value = _state.value.copy(
            fullAccessStatus = latestAccessStatus.copy(
                storeMessage = if (accessChanged) null else latestAccessStatus.storeMessage
            ),
            isUnlockDialogVisible = !unlocked && _state.value.isUnlockDialogVisible,
            exportError = if (unlocked) _state.value.exportError.withoutGateError() else _state.value.exportError,
            backupError = if (unlocked) {
                _state.value.backupError.withoutGateError() ?: _state.value.backupStatus.warning
            } else _state.value.backupError
        )
    }

    fun openDefaultRestPicker() {
        _state.value = _state.value.copy(
            isDefaultRestPickerVisible = true,
            draftDefaultRestSeconds = _state.value.defaultRestSeconds,
            defaultRestError = null
        )
    }

    fun cancelDefaultRestPicker() {
        _state.value = _state.value.copy(
            isDefaultRestPickerVisible = false,
            draftDefaultRestSeconds = _state.value.defaultRestSeconds,
            defaultRestError = null
        )
    }

    fun setDraftDefaultRestSeconds(seconds: Int) {
        _state.value = _state.value.copy(
            draftDefaultRestSeconds = seconds,
            defaultRestError = null
        )
    }

    suspend fun saveDefaultRest(): FoundationResult<Int> {
        val current = _state.value
        val result = preferences?.setDefaultRestSeconds(current.draftDefaultRestSeconds)
            ?: foundationSuccess(current.draftDefaultRestSeconds)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = current.copy(defaultRestError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = current.copy(
                    defaultRestSeconds = result.value,
                    isDefaultRestPickerVisible = false,
                    draftDefaultRestSeconds = result.value,
                    defaultRestError = null,
                    exportError = null
                )
                result
            }
        }
    }

    suspend fun setRestSoundEnabled(enabled: Boolean): FoundationResult<Boolean> {
        val result = preferences?.setRestSoundEnabled(enabled) ?: foundationSuccess(enabled)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(exportError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(restSoundEnabled = result.value, exportError = null)
                result
            }
        }
    }

    suspend fun setStartWorkoutTimerWithFirstSet(enabled: Boolean): FoundationResult<Boolean> {
        val result = preferences?.setStartWorkoutTimerWithFirstSet(enabled) ?: foundationSuccess(enabled)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(exportError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(
                    startWorkoutTimerWithFirstSet = result.value,
                    exportError = null
                )
                result
            }
        }
    }

    suspend fun setRestTimerSurfaceEnabled(enabled: Boolean): FoundationResult<Boolean> {
        val result = preferences?.setRestTimerSurfaceEnabled(enabled) ?: foundationSuccess(enabled)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(exportError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(
                    restTimerSurfaceEnabled = result.value,
                    exportError = null
                )
                onRestTimerSurfacePreferenceChanged?.invoke()
                result
            }
        }
    }

    suspend fun setWeightUnit(unit: WeightUnit): FoundationResult<WeightUnit> {
        val result = preferences?.setWeightUnit(unit) ?: foundationSuccess(unit)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(exportError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                val step = preferences?.weightStep(result.value) ?: WeightStepPreference.defaultFor(result.value)
                _state.value = _state.value.copy(
                    weightUnit = result.value,
                    weightStep = step,
                    draftWeightStep = step,
                    weightStepError = null,
                    exportError = null
                )
                result
            }
        }
    }

    fun openWeightStepPicker() {
        _state.value = _state.value.copy(
            isWeightStepPickerVisible = true,
            draftWeightStep = _state.value.weightStep,
            weightStepError = null
        )
    }

    fun cancelWeightStepPicker() {
        _state.value = _state.value.copy(
            isWeightStepPickerVisible = false,
            draftWeightStep = _state.value.weightStep,
            weightStepError = null
        )
    }

    fun setDraftWeightStep(step: Double) {
        _state.value = _state.value.copy(
            draftWeightStep = WeightStepPreference.normalize(step),
            weightStepError = null
        )
    }

    suspend fun saveWeightStep(): FoundationResult<Double> {
        val current = _state.value
        val result = preferences?.setWeightStep(current.weightUnit, current.draftWeightStep)
            ?: foundationSuccess(current.draftWeightStep)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = current.copy(weightStepError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = current.copy(
                    weightStep = result.value,
                    draftWeightStep = result.value,
                    isWeightStepPickerVisible = false,
                    weightStepError = null,
                    exportError = null
                )
                result
            }
        }
    }

    suspend fun export(type: ExportType): FoundationResult<ProfileExportResult> {
        val repository = exports
            ?: return exportFailure(FoundationError.Platform("Export unavailable"))
        val unit = _state.value.weightUnit
        _state.value = _state.value.copy(isExporting = true, exportError = null)

        return try {
            when (val result = ExportService(repository).export(type, unit)) {
                is FoundationResult.Failure -> exportFailure(result.error)
                is FoundationResult.Success -> {
                    exportHandoff?.invoke(result.value)
                    val profileResult = result.value.toProfileResult()
                    _state.value = _state.value.copy(
                        isExporting = false,
                        lastExport = profileResult,
                        exportError = null
                    )
                    foundationSuccess(profileResult)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            exportFailure(FoundationError.Platform(throwable.message ?: "Export handoff failed"))
        }
    }

    fun startBackupSetup() {
        if (fullAccess != null && !currentFullAccess().hasFullAccess) {
            val message = FullAccessGate.BACKUP_LINK.blockedMessage()
            _state.value = _state.value.copy(
                backupSetupStep = null,
                backupError = message,
                isUnlockDialogVisible = true,
                fullAccessStatus = _state.value.fullAccessStatus.copy(error = message)
            )
            return
        }
        _state.value = _state.value.copy(
            backupSetupStep = BackupSetupStep.INTRO,
            backupError = null
        )
    }

    fun advanceBackupSetup() {
        _state.value = _state.value.copy(
            backupSetupStep = when (_state.value.backupSetupStep) {
                BackupSetupStep.INTRO -> BackupSetupStep.LOCATION
                BackupSetupStep.LOCATION -> BackupSetupStep.READY
                BackupSetupStep.READY -> BackupSetupStep.READY
                null -> BackupSetupStep.INTRO
            }
        )
    }

    fun backUpBackupSetup() {
        _state.value = _state.value.copy(
            backupSetupStep = when (_state.value.backupSetupStep) {
                BackupSetupStep.READY -> BackupSetupStep.LOCATION
                BackupSetupStep.LOCATION -> BackupSetupStep.INTRO
                BackupSetupStep.INTRO,
                null -> null
            }
        )
    }

    fun dismissBackupSetup() {
        _state.value = _state.value.copy(backupSetupStep = null)
    }

    fun dismissUnlockDialog() {
        _state.value = _state.value.copy(isUnlockDialogVisible = false)
    }

    suspend fun linkBackupFile(): FoundationResult<BackupSyncState> =
        requireFullAccess(FullAccessGate.BACKUP_LINK)?.let { backupFailure(it) }
            ?: backupOperation(closeSetup = true) { it.linkNewBackup() }

    suspend fun backupNow(): FoundationResult<BackupSyncState> =
        requireFullAccess(FullAccessGate.BACKUP_NOW)?.let { backupFailure(it) }
            ?: backupOperation { it.backupNow() }

    suspend fun syncNow(): FoundationResult<BackupSyncState> =
        requireFullAccess(FullAccessGate.SYNC_NOW)?.let { backupFailure(it) }
            ?: backupOperation { it.syncNow() }

    suspend fun checkLinkedBackup(): FoundationResult<BackupSyncState> {
        val coordinator = backupSync
            ?: return backupFailure(FoundationError.Platform("Backup unavailable"))
        val access = fullAccess
        if (access != null) {
            val accessState = storeMutex.withLock {
                access.loadState().also { loaded ->
                    _state.value = _state.value.copy(fullAccessStatus = accessStatus(
                        loaded, _state.value.fullAccessStatus.offerState, error = _state.value.fullAccessStatus.error
                    ).copy(storeMessage = _state.value.fullAccessStatus.storeMessage))
                }
            }
            if (!accessState.hasFullAccess) {
                val syncState = coordinator.loadState()
                _state.value = _state.value.copy(
                    isBackupBusy = false,
                    backupStatus = syncState.toProfileStatus(),
                    backupError = syncState.lastError
                )
                return foundationSuccess(syncState)
            }
        }
        return try {
            when (val result = coordinator.syncLinkedBackupIfAvailable()) {
                is FoundationResult.Failure -> {
                    _state.value = _state.value.copy(
                        isBackupBusy = false,
                        backupStatus = coordinator.loadState().toProfileStatus(),
                        backupError = result.error.message
                    )
                    result
                }
                is FoundationResult.Success -> {
                    _state.value = _state.value.copy(
                        isBackupBusy = false,
                        backupStatus = result.value.toProfileStatus(),
                        backupError = result.value.lastError
                    )
                    result
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            backupFailure(FoundationError.Platform(throwable.message ?: "Backup check failed"))
        }
    }

    suspend fun restoreFromFile(): FoundationResult<BackupRestoreResult> {
        requireFullAccess(FullAccessGate.RESTORE_BACKUP)?.let { return backupRestoreFailure(it) }
        val coordinator = backupSync
            ?: return backupRestoreFailure(FoundationError.Platform("Backup unavailable"))
        _state.value = _state.value.copy(
            isBackupBusy = true,
            backupError = null,
            lastRestoreMessage = null,
            restoreWarning = null,
            safetyBackupMessage = null
        )
        return try {
            when (val result = coordinator.restoreFromFile()) {
                is FoundationResult.Failure -> backupRestoreFailure(result.error)
                is FoundationResult.Success -> {
                    val syncState = try {
                        coordinator.loadState()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        BackupSyncState(
                            lastOutcome = BackupSyncOutcome.RESTORED,
                            lastError = "Workout data was restored, but backup state could not be read. Review your backup connection before syncing.",
                            updatedAt = Clock.System.now()
                        )
                    }
                    val warning = result.value.syncWarning ?: syncState.lastError
                    _state.value = _state.value.copy(
                        isBackupBusy = false,
                        backupStatus = syncState.toProfileStatus(),
                        backupError = warning,
                        lastRestoreMessage = "Restored ${result.value.restoredSummary.displayCounts()}",
                        restoreWarning = if (result.value.activeWorkoutReplaced) {
                            if (result.value.restoredSummary.hasActiveWorkout) {
                                "Active workout was replaced by the backup."
                            } else {
                                "Active workout was removed. It is preserved in your safety backup."
                            }
                        } else {
                            null
                        },
                        safetyBackupMessage = "Safety backup created before restore."
                    )
                    foundationSuccess(result.value.copy(syncWarning = warning))
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            backupRestoreFailure(FoundationError.Platform(throwable.message ?: "Restore failed"))
        }
    }

    suspend fun keepLocalBackup(): FoundationResult<BackupSyncState> =
        requireFullAccess(FullAccessGate.BACKUP_NOW)?.let { backupFailure(it) }
            ?: backupOperation { it.resolveConflict(BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP) }

    suspend fun restoreBackupConflict(): FoundationResult<BackupSyncState> =
        requireFullAccess(FullAccessGate.RESTORE_BACKUP)?.let { backupFailure(it) }
            ?: backupOperation { it.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY) }

    suspend fun cancelBackupConflict(): FoundationResult<BackupSyncState> =
        backupOperation { it.resolveConflict(BackupConflictDecision.CANCEL) }

    suspend fun refreshStoreOffer(): FullAccessOfferState = storeMutex.withLock {
        refreshStoreOfferLocked()
    }

    private suspend fun refreshStoreOfferLocked(): FullAccessOfferState {
        _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(
            offerState = FullAccessOfferState.Loading, isStoreBusy = true, error = null
        ))
        try {
            val offer = fullAccess?.loadLifetimeOffer()
                ?: FullAccessOfferState.Unavailable("Store purchases are unavailable on this build.")
            _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(offerState = offer))
            return offer
        } catch (cancellation: CancellationException) {
            _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(
                offerState = FullAccessOfferState.Unavailable("Loading the store offer was canceled. Try again.")
            ))
            throw cancellation
        } finally {
            _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(isStoreBusy = false))
        }
    }

    suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessState> = storeMutex.withLock {
        if (_state.value.fullAccessStatus.hasFullAccess) {
            foundationSuccess(_state.value.fullAccessStatus.access)
        } else if (_state.value.fullAccessStatus.offerState !is FullAccessOfferState.Available) {
            fullAccessFailure(FoundationError.Platform("Load the store offer before purchasing. Try again."))
        } else {
            storeOperationLocked(restore = false)
        }
    }

    suspend fun restorePurchases(): FoundationResult<FullAccessState> = storeMutex.withLock {
        storeOperationLocked(restore = true)
    }

    private suspend fun storeOperationLocked(restore: Boolean): FoundationResult<FullAccessState> {
        val access = fullAccess
            ?: return fullAccessFailure(FoundationError.Platform("Store purchases unavailable"))
        _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(
            isStoreBusy = true, error = null, storeMessage = null
        ))
        return try {
            val result = if (restore) access.restorePurchases() else access.purchaseLifetimeUnlock()
            when (result) {
                is FoundationResult.Failure -> fullAccessFailure(result.error)
                is FoundationResult.Success -> {
                    val unlocked = result.value.hasFullAccess
                    val message = result.value.lastError ?: when {
                        restore && unlocked -> "Lifetime purchase restored."
                        restore -> "No lifetime purchase was found for this store account."
                        !unlocked -> "Purchase did not unlock Full Access. Try again or restore your purchase."
                        else -> null
                    }
                    val status = accessStatus(result.value, _state.value.fullAccessStatus.offerState, error = null)
                        .copy(isStoreBusy = true, error = null, storeMessage = message)
                    _state.value = _state.value.copy(
                        fullAccessStatus = status,
                        isUnlockDialogVisible = !unlocked && _state.value.isUnlockDialogVisible,
                        exportError = if (unlocked) _state.value.exportError.withoutGateError() else _state.value.exportError,
                        backupError = if (unlocked) {
                            _state.value.backupError.withoutGateError() ?: _state.value.backupStatus.warning
                        } else _state.value.backupError
                    )
                    result
                }
            }
        } catch (cancellation: CancellationException) {
            _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(
                error = if (restore) "Restore purchase was canceled. Try again." else "Purchase was canceled. Try again."
            ))
            throw cancellation
        } catch (error: Exception) {
            fullAccessFailure(FoundationError.Platform(error.message ?: "Store operation failed. Try again."))
        } finally {
            _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(isStoreBusy = false))
        }
    }

    fun setPaletteMode(mode: PaletteMode) {
        _state.value = _state.value.copy(paletteMode = mode)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(hapticsEnabled = enabled)
    }

    fun setReduceMotion(enabled: Boolean) {
        _state.value = _state.value.copy(reduceMotion = enabled)
    }

    private suspend fun requireFullAccess(gate: FullAccessGate): FoundationError? = storeMutex.withLock {
        val access = fullAccess ?: return@withLock null
        val result = access.checkGate(gate)
        if (result.allowed) {
            _state.value = _state.value.copy(fullAccessStatus = accessStatus(
                result.state, _state.value.fullAccessStatus.offerState, error = null
            ))
            null
        } else {
            val message = gate.blockedMessage()
            _state.value = _state.value.copy(
                isUnlockDialogVisible = true,
                backupSetupStep = null,
                fullAccessStatus = accessStatus(result.state, FullAccessOfferState.Loading, error = message)
            )
            refreshStoreOfferLocked()
            _state.value = _state.value.copy(fullAccessStatus = _state.value.fullAccessStatus.copy(error = message))
            FoundationError.Validation(message)
        }
    }

    private suspend fun loadFullAccessStatus(): ProfileFullAccessStatus {
        val access = fullAccess ?: return ProfileFullAccessStatus(
            hasFullAccess = true,
            statusLabel = "Unlocked",
            detailLabel = "Unlimited workout logging is unlocked."
        )
        return accessStatus(access.loadState(), _state.value.fullAccessStatus.offerState, error = null)
            .copy(isStoreBusy = _state.value.fullAccessStatus.isStoreBusy, storeMessage = _state.value.fullAccessStatus.storeMessage)
    }

    private fun currentFullAccess(): FullAccessState =
        _state.value.fullAccessStatus.access

    private fun String?.withoutGateError(): String? =
        takeUnless { message -> FullAccessGate.entries.any { it.blockedMessage() == message } }

    private fun fullAccessFailure(error: FoundationError): FoundationResult<FullAccessState> {
        _state.value = _state.value.copy(
            fullAccessStatus = _state.value.fullAccessStatus.copy(isStoreBusy = false, error = error.message, storeMessage = null)
        )
        return foundationFailure(error)
    }

    private fun exportFailure(error: FoundationError): FoundationResult<ProfileExportResult> {
        _state.value = _state.value.copy(
            isExporting = false,
            exportError = error.message
        )
        return foundationFailure(error)
    }

    private suspend fun backupOperation(
        closeSetup: Boolean = false,
        block: suspend (BackupSyncCoordinator) -> FoundationResult<BackupSyncState>
    ): FoundationResult<BackupSyncState> {
        val coordinator = backupSync
            ?: return backupFailure(FoundationError.Platform("Backup unavailable"))
        _state.value = _state.value.copy(
            isBackupBusy = true,
            backupError = null,
            lastRestoreMessage = null,
            restoreWarning = null,
            safetyBackupMessage = null,
            backupSetupStep = if (closeSetup) null else _state.value.backupSetupStep
        )
        return try {
            when (val result = block(coordinator)) {
                is FoundationResult.Failure -> {
                    _state.value = _state.value.copy(backupStatus = coordinator.loadState().toProfileStatus())
                    backupFailure(result.error)
                }
                is FoundationResult.Success -> {
                    _state.value = _state.value.copy(
                        isBackupBusy = false,
                        backupStatus = result.value.toProfileStatus(),
                        backupError = result.value.lastError
                    )
                    result
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            backupFailure(FoundationError.Platform(throwable.message ?: "Backup operation failed"))
        }
    }

    private fun backupFailure(error: FoundationError): FoundationResult<BackupSyncState> {
        _state.value = _state.value.copy(isBackupBusy = false, backupError = error.message)
        return foundationFailure(error)
    }

    private fun backupRestoreFailure(error: FoundationError): FoundationResult<BackupRestoreResult> {
        _state.value = _state.value.copy(isBackupBusy = false, backupError = error.message)
        return foundationFailure(error)
    }

    private fun ExportFile.toProfileResult(): ProfileExportResult =
        ProfileExportResult(
            type = snapshot.exportType,
            fileName = fileName,
            rowCount = snapshot.rowCount,
            weightUnit = snapshot.weightUnit
        )

    private fun BackupSyncState.toProfileStatus(): ProfileBackupStatus {
        val linked = linkedFile
        return ProfileBackupStatus(
            linkedLocation = linked?.displayName ?: "Not linked",
            lastSyncLabel = lastBackupTimestamp?.toString() ?: "Never",
            lastOutcomeLabel = lastOutcome.displayLabel(),
            conflictSummary = lastConflictSummary,
            canBackup = linked != null && !isTerminalUnavailable(),
            canSync = linked != null && !isTerminalUnavailable(),
            hasConflict = lastOutcome == BackupSyncOutcome.CONFLICT || lastOutcome == BackupSyncOutcome.BACKUP_CHANGED,
            isLinked = linked != null,
            warning = lastError
        )
    }

    private fun BackupSyncState.isTerminalUnavailable(): Boolean =
        lastOutcome == BackupSyncOutcome.UNAVAILABLE

    private fun BackupSyncOutcome.displayLabel(): String =
        when (this) {
            BackupSyncOutcome.UNLINKED -> "No backup linked"
            BackupSyncOutcome.LINKED -> "Backup linked"
            BackupSyncOutcome.CLEAN -> "Up to date"
            BackupSyncOutcome.LOCAL_WRITTEN -> "Backup updated"
            BackupSyncOutcome.BACKUP_CHANGED -> "Backup changed"
            BackupSyncOutcome.CONFLICT -> "Conflict needs review"
            BackupSyncOutcome.RESTORED -> "Backup restored"
            BackupSyncOutcome.UNAVAILABLE -> "Linked file unavailable"
            BackupSyncOutcome.FAILED -> "Backup error"
        }

    private fun accessStatus(
        access: FullAccessState,
        offerState: FullAccessOfferState,
        error: String?
    ): ProfileFullAccessStatus {
        val completed = access.normalizedCompletedFreeWorkouts
        val displayedCompleted = completed.coerceAtMost(freeWorkoutLimit)
        return ProfileFullAccessStatus(
            access = access,
            statusLabel = access.statusLabel(freeWorkoutLimit),
            detailLabel = access.detailLabel(freeWorkoutLimit),
            offerState = offerState,
            completedFreeWorkouts = displayedCompleted,
            freeWorkoutLimit = freeWorkoutLimit,
            isFreeLimitReached = !access.hasFullAccess && completed >= freeWorkoutLimit,
            hasFullAccess = access.hasFullAccess,
            isStoreBusy = false,
            error = error ?: access.lastError
        )
    }
}

private fun FullAccessState.statusLabel(freeWorkoutLimit: Int): String =
    when {
        hasFullAccess -> "Unlocked"
        normalizedCompletedFreeWorkouts >= freeWorkoutLimit -> "Unlock required"
        else -> "Free"
    }

private fun FullAccessState.detailLabel(freeWorkoutLimit: Int): String {
    val completed = normalizedCompletedFreeWorkouts
    return when {
        hasFullAccess -> "Unlimited workout logging is unlocked."
        completed >= freeWorkoutLimit -> "You've used your free workouts."
        else -> "${completed.coerceAtMost(freeWorkoutLimit)} of $freeWorkoutLimit free workouts used"
    }
}

private fun FullAccessGate.blockedMessage(): String =
    when (this) {
        FullAccessGate.WORKOUT_START -> "You've used your free workouts."
        FullAccessGate.EXPORT -> "Export is available to everyone."
        FullAccessGate.IMPORT -> "Unlock forever to import workout data."
        FullAccessGate.BACKUP_LINK -> "Unlock forever to set up backup."
        FullAccessGate.BACKUP_NOW -> "Unlock forever to back up your data."
        FullAccessGate.SYNC_NOW -> "Unlock forever to sync your backup."
        FullAccessGate.RESTORE_BACKUP -> "Unlock forever to restore from backup."
    }
