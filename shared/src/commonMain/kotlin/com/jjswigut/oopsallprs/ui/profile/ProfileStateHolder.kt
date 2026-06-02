package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.export.ExportService
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.ExportRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.ui.navigation.PaletteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.cancellation.CancellationException

data class LocalReadinessStatus(
    val storageLabel: String = "Local database",
    val syncLabel: String = "Cloud sync off",
    val backupLabel: String = "Android Auto Backup eligible",
    val restNotificationLabel: String = "Timers recover if alerts are off",
    val exportLabel: String = "CSV export available",
    val alphaGateLabel: String = "Manual alpha smoke pending"
)

typealias LocalOwnershipStatus = LocalReadinessStatus

data class ProfileExportResult(
    val type: ExportType,
    val fileName: String,
    val rowCount: Int,
    val weightUnit: WeightUnit
)

data class ProfileState(
    val weightUnit: WeightUnit = WeightUnit.POUNDS,
    val weightStep: Double = WeightStepPreference.DEFAULT_POUNDS_STEP,
    val isWeightStepPickerVisible: Boolean = false,
    val draftWeightStep: Double = WeightStepPreference.DEFAULT_POUNDS_STEP,
    val weightStepError: String? = null,
    val defaultRestSeconds: Int = RestConfiguration.DEFAULT_SECONDS,
    val restSoundEnabled: Boolean = true,
    val paletteMode: PaletteMode = PaletteMode.DARK,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val localStatus: LocalReadinessStatus = LocalReadinessStatus(),
    val isHydrated: Boolean = false,
    val isExporting: Boolean = false,
    val lastExport: ProfileExportResult? = null,
    val exportError: String? = null
)

class ProfileStateHolder(
    private val preferences: PreferencesRepository? = null,
    private val exports: ExportRepository? = null,
    private val exportHandoff: ((ExportFile) -> Unit)? = null
) {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state

    suspend fun hydrate() {
        val unit = preferences?.weightUnit() ?: _state.value.weightUnit
        val step = preferences?.weightStep(unit) ?: _state.value.weightStep
        val restSeconds = preferences?.defaultRestSeconds() ?: _state.value.defaultRestSeconds
        val soundEnabled = preferences?.restSoundEnabled() ?: _state.value.restSoundEnabled
        _state.value = _state.value.copy(
            weightUnit = unit,
            weightStep = step,
            draftWeightStep = step,
            weightStepError = null,
            defaultRestSeconds = restSeconds,
            restSoundEnabled = soundEnabled,
            localStatus = LocalReadinessStatus(),
            isHydrated = true,
            exportError = null
        )
    }

    suspend fun setDefaultRestSeconds(seconds: Int): FoundationResult<Int> {
        val result = preferences?.setDefaultRestSeconds(seconds) ?: foundationSuccess(seconds)
        return when (result) {
            is FoundationResult.Failure -> {
                _state.value = _state.value.copy(exportError = result.error.message)
                result
            }
            is FoundationResult.Success -> {
                _state.value = _state.value.copy(defaultRestSeconds = result.value, exportError = null)
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

    fun setPaletteMode(mode: PaletteMode) {
        _state.value = _state.value.copy(paletteMode = mode)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(hapticsEnabled = enabled)
    }

    fun setReduceMotion(enabled: Boolean) {
        _state.value = _state.value.copy(reduceMotion = enabled)
    }

    private fun exportFailure(error: FoundationError): FoundationResult<ProfileExportResult> {
        _state.value = _state.value.copy(
            isExporting = false,
            exportError = error.message
        )
        return foundationFailure(error)
    }

    private fun ExportFile.toProfileResult(): ProfileExportResult =
        ProfileExportResult(
            type = snapshot.exportType,
            fileName = fileName,
            rowCount = snapshot.rowCount,
            weightUnit = snapshot.weightUnit
        )
}
