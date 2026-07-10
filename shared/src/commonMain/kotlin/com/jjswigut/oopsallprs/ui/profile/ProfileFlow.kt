package com.jjswigut.oopsallprs.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.dev.DeveloperSeedOutcome
import com.jjswigut.oopsallprs.dev.DeveloperSeedScenario
import com.jjswigut.oopsallprs.dev.DeveloperSeedState
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.formatWeightStep
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitDialog
import com.jjswigut.oopsallprs.ds.component.FitRoller
import com.jjswigut.oopsallprs.ds.component.FitSegmentedControl
import com.jjswigut.oopsallprs.ds.component.FitToggle
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.common.RestDurationRoller
import com.jjswigut.oopsallprs.ui.common.formatRestDurationSeconds
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.navigation.PaletteMode

@Composable
fun ProfileFlow(
    state: ProfileState,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    onWeightStepClick: () -> Unit,
    onWeightStepDraftChange: (Double) -> Unit,
    onWeightStepSave: () -> Unit,
    onWeightStepCancel: () -> Unit,
    onDefaultRestClick: () -> Unit,
    onDefaultRestDraftChange: (Int) -> Unit,
    onDefaultRestSave: () -> Unit,
    onDefaultRestCancel: () -> Unit,
    onRestSoundChanged: (Boolean) -> Unit,
    onPaletteModeSelected: (PaletteMode) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onPurchaseLifetimeUnlock: () -> Unit,
    onRestorePurchases: () -> Unit,
    onExportRequested: (ExportType) -> Unit,
    onStartBackupSetup: () -> Unit,
    onBackupSetupNext: () -> Unit,
    onBackupSetupBack: () -> Unit,
    onBackupSetupDismiss: () -> Unit,
    onLinkBackupFile: () -> Unit,
    onBackupNow: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreFromFile: () -> Unit,
    onKeepLocalBackup: () -> Unit,
    onRestoreBackupConflict: () -> Unit,
    onCancelBackupConflict: () -> Unit,
    onManageExercises: () -> Unit,
    developerSeedState: DeveloperSeedState? = null,
    onDeveloperSeedSelected: (DeveloperSeedScenario) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        FullAccessCard(
            state = state,
            onPurchaseLifetimeUnlock = onPurchaseLifetimeUnlock,
            onRestorePurchases = onRestorePurchases
        )
        UnitsCard(
            state = state,
            onWeightUnitSelected = onWeightUnitSelected,
            onWeightStepClick = onWeightStepClick
        )
        RestPreferencesCard(state, onDefaultRestClick, onRestSoundChanged)
        ExercisesCard(onManageExercises)
        BackupCard(
            state = state,
            onStartBackupSetup = onStartBackupSetup,
            onLinkBackupFile = onLinkBackupFile,
            onBackupNow = onBackupNow,
            onSyncNow = onSyncNow,
            onRestoreFromFile = onRestoreFromFile,
            onKeepLocalBackup = onKeepLocalBackup,
            onRestoreBackupConflict = onRestoreBackupConflict,
            onCancelBackupConflict = onCancelBackupConflict
        )
        ExportCard(state, onExportRequested)
        developerSeedState?.let { seedState ->
            DeveloperSeedCard(seedState, onDeveloperSeedSelected)
        }
        InteractionCard(
            state = state,
            onPaletteModeSelected = onPaletteModeSelected,
            onHapticsChanged = onHapticsChanged,
            onReduceMotionChanged = onReduceMotionChanged
        )
    }

    if (state.isWeightStepPickerVisible) {
        WeightStepDialog(
            state = state,
            onDraftChange = onWeightStepDraftChange,
            onSave = onWeightStepSave,
            onCancel = onWeightStepCancel
        )
    }
    if (state.isDefaultRestPickerVisible) {
        DefaultRestDialog(
            state = state,
            onDraftChange = onDefaultRestDraftChange,
            onSave = onDefaultRestSave,
            onCancel = onDefaultRestCancel
        )
    }
    state.backupSetupStep?.let { step ->
        BackupSetupDialog(
            step = step,
            isBusy = state.isBackupBusy,
            onNext = onBackupSetupNext,
            onBack = onBackupSetupBack,
            onDismiss = onBackupSetupDismiss,
            onChooseLocation = onLinkBackupFile
        )
    }
}

@Composable
private fun FullAccessCard(
    state: ProfileState,
    onPurchaseLifetimeUnlock: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val access = state.fullAccessStatus
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Unlock")
            StatusRow("Status", access.statusLabel)
            FoundationMutedText(access.detailLabel)
            when {
                access.hasFullAccess -> FoundationMutedText("Unlimited logging is available forever.")
                access.isFreeLimitReached -> {
                    FoundationText(
                        text = "You've used your free workouts.",
                        style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
                    )
                    FoundationMutedText("Unlock unlimited workout logging forever.")
                    FoundationMutedText(access.termsLabel)
                    StatusRow("Price", access.offerLabel)
                    FitButton(
                        text = if (access.isStoreBusy) "Working" else "Unlock forever",
                        onClick = onPurchaseLifetimeUnlock,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !access.isStoreBusy,
                        style = FitButtonStyle.Primary
                    )
                    FitButton(
                        text = if (access.isStoreBusy) "Working" else "Restore purchase",
                        onClick = onRestorePurchases,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !access.isStoreBusy,
                        style = FitButtonStyle.Secondary
                    )
                }
                else -> FoundationMutedText("Keep logging. Unlock appears when the free workout limit is reached.")
            }
            if (!access.hasFullAccess && !access.isFreeLimitReached) {
                FitButton(
                    text = if (access.isStoreBusy) "Working" else "Restore purchase",
                    onClick = onRestorePurchases,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !access.isStoreBusy,
                    style = FitButtonStyle.Secondary
                )
            }
            FoundationMutedText("Purchases restore through the app store used to buy them.")
            access.error?.let { message ->
                FoundationText(
                    text = message,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
    }
}

@Composable
private fun BackupCard(
    state: ProfileState,
    onStartBackupSetup: () -> Unit,
    onLinkBackupFile: () -> Unit,
    onBackupNow: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreFromFile: () -> Unit,
    onKeepLocalBackup: () -> Unit,
    onRestoreBackupConflict: () -> Unit,
    onCancelBackupConflict: () -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Backup")
            if (!state.backupStatus.isLinked) {
                StatusRow("This phone", "Saved locally")
                StatusRow("Backup", "Not set up")
                FoundationMutedText("Keep a copy of your workouts in a file you choose, like Drive or local storage.")
                FitButton(
                    text = if (state.isBackupBusy) "Working" else "Set up backup",
                    onClick = onStartBackupSetup,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBackupBusy,
                    style = FitButtonStyle.Primary
                )
                FoundationMutedText("No account is required. Oops All PRs only writes to the file you pick.")
            } else {
                StatusRow("Backup file", state.backupStatus.linkedLocation)
                StatusRow("Last check", state.backupStatus.lastSyncLabel)
                StatusRow("Status", state.backupStatus.lastOutcomeLabel)
                FoundationMutedText(state.backupStatus.privacyLabel)
                FitButton(
                    text = if (state.isBackupBusy) "Working" else "Change backup file",
                    onClick = onLinkBackupFile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBackupBusy,
                    style = FitButtonStyle.Secondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
                ) {
                    FitButton(
                        text = "Backup now",
                        onClick = onBackupNow,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBackupBusy && state.backupStatus.canBackup,
                        style = FitButtonStyle.Secondary
                    )
                    FitButton(
                        text = "Sync now",
                        onClick = onSyncNow,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBackupBusy && state.backupStatus.canSync,
                        style = FitButtonStyle.Secondary
                    )
                }
            }
            RestoreAction(onRestoreFromFile, state.isBackupBusy)
            if (state.backupStatus.hasConflict) {
                state.backupStatus.conflictSummary?.let { FoundationMutedText(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
                ) {
                    FitButton(
                        text = "Keep local",
                        onClick = onKeepLocalBackup,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBackupBusy,
                        style = FitButtonStyle.Secondary
                    )
                    FitButton(
                        text = "Restore backup",
                        onClick = onRestoreBackupConflict,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBackupBusy,
                        style = FitButtonStyle.Secondary
                    )
                }
                FitButton(
                    text = "Cancel conflict",
                    onClick = onCancelBackupConflict,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBackupBusy,
                    style = FitButtonStyle.Secondary
                )
            }
            state.lastRestoreMessage?.let { FoundationMutedText(it) }
            state.restoreWarning?.let {
                FoundationText(
                    text = it,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
            state.safetyBackupMessage?.let { FoundationMutedText(it) }
            state.backupError?.let { message ->
                FoundationText(
                    text = message,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
    }
}

@Composable
private fun BackupSetupDialog(
    step: BackupSetupStep,
    isBusy: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onChooseLocation: () -> Unit
) {
    val content = when (step) {
        BackupSetupStep.INTRO -> BackupSetupContent(
            eyebrow = "Step 1 of 3",
            title = "Back up your training",
            body = "Oops All PRs keeps your data on this phone. A backup adds a second copy so a lost or replaced phone is less risky.",
            bullets = listOf(
                "Workouts, routines, exercises, PRs, preferences, and active workout state are included.",
                "The app stays account-free and local-first."
            ),
            primary = "Next"
        )
        BackupSetupStep.LOCATION -> BackupSetupContent(
            eyebrow = "Step 2 of 3",
            title = "Pick where the file lives",
            body = "Android will ask where to save the backup file. You can choose cloud storage, local files, or another document provider.",
            bullets = listOf(
                "Use a cloud folder if you want the file available on another device.",
                "Anyone with access to the file can read it."
            ),
            primary = "Next"
        )
        BackupSetupStep.READY -> BackupSetupContent(
            eyebrow = "Step 3 of 3",
            title = "Create your backup file",
            body = "After you choose a location, Oops All PRs writes the first backup and remembers that file for future backups.",
            bullets = listOf(
                "You can run Backup now any time from Profile.",
                "Restore from file remains available if you need to recover data."
            ),
            primary = "Choose location"
        )
    }

    FitDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            SectionLabel(content.eyebrow)
            FoundationText(
                text = content.title,
                style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText(content.body)
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                content.bullets.forEach { bullet ->
                    FoundationMutedText("- $bullet")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                FitButton(
                    text = if (step == BackupSetupStep.INTRO) "Close" else "Back",
                    onClick = if (step == BackupSetupStep.INTRO) onDismiss else onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    style = FitButtonStyle.Secondary
                )
                FitButton(
                    text = if (isBusy) "Working" else content.primary,
                    onClick = if (step == BackupSetupStep.READY) onChooseLocation else onNext,
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

private data class BackupSetupContent(
    val eyebrow: String,
    val title: String,
    val body: String,
    val bullets: List<String>,
    val primary: String
)

@Composable
private fun RestoreAction(
    onRestoreFromFile: () -> Unit,
    isBackupBusy: Boolean
) {
    FitButton(
        text = "Restore from file",
        onClick = onRestoreFromFile,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isBackupBusy,
        style = FitButtonStyle.Secondary
    )
}

@Composable
private fun DeveloperSeedCard(
    state: DeveloperSeedState,
    onDeveloperSeedSelected: (DeveloperSeedScenario) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Developer")
            state.scenarios.forEach { row ->
                val isLoading = state.loadingScenario == row.scenario
                FitButton(
                    text = if (isLoading) "Loading" else row.label,
                    onClick = { onDeveloperSeedSelected(row.scenario) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.loadingScenario == null,
                    style = FitButtonStyle.Secondary
                )
                FoundationMutedText(row.description)
            }
            state.lastResult?.let { result ->
                FoundationText(
                    text = "${result.outcome.displayLabel()}: ${result.message}",
                    style = FitTheme.type.caption.copy(
                        color = when (result.outcome) {
                            DeveloperSeedOutcome.FAILED -> FitTheme.colors.danger
                            DeveloperSeedOutcome.LOADED,
                            DeveloperSeedOutcome.SKIPPED -> FitTheme.colors.onSurface
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun RestPreferencesCard(
    state: ProfileState,
    onDefaultRestClick: () -> Unit,
    onRestSoundChanged: (Boolean) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Rest")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FoundationText("Default rest")
                    FoundationMutedText(formatRestDurationSeconds(state.defaultRestSeconds))
                }
                FitButton(
                    text = "Change",
                    onClick = onDefaultRestClick,
                    style = FitButtonStyle.Secondary
                )
            }
            ToggleRow(
                label = "Rest sound",
                value = if (state.restSoundEnabled) "On" else "Off",
                checked = state.restSoundEnabled,
                onCheckedChange = onRestSoundChanged
            )
            FoundationMutedText("New exercises use ${formatRestDurationSeconds(state.defaultRestSeconds)} rest.")
        }
    }
}

@Composable
private fun DefaultRestDialog(
    state: ProfileState,
    onDraftChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    FitDialog(onDismissRequest = onCancel) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            SectionLabel("Default rest")
            FoundationMutedText(formatRestDurationSeconds(state.draftDefaultRestSeconds))
            RestDurationRoller(
                seconds = state.draftDefaultRestSeconds,
                onSecondsChange = onDraftChange
            )
            state.defaultRestError?.let { error ->
                FoundationText(
                    text = error,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                FitButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Save",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

@Composable
private fun UnitsCard(
    state: ProfileState,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    onWeightStepClick: () -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Units")
            FitSegmentedControl(
                options = listOf("Pounds", "Kilograms"),
                selectedIndex = if (state.weightUnit == WeightUnit.POUNDS) 0 else 1,
                onSelect = { index ->
                    onWeightUnitSelected(if (index == 0) WeightUnit.POUNDS else WeightUnit.KILOGRAMS)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FoundationText("Weight step")
                    FoundationMutedText("${formatWeightStep(state.weightStep)} ${state.weightUnit.abbreviation()}")
                }
                FitButton(
                    text = "Change",
                    onClick = onWeightStepClick,
                    style = FitButtonStyle.Secondary
                )
            }
            FoundationMutedText("Exports use ${state.weightUnit.displayName()}.")
        }
    }
}

@Composable
private fun WeightStepDialog(
    state: ProfileState,
    onDraftChange: (Double) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val unitLabel = state.weightUnit.abbreviation()
    FitDialog(onDismissRequest = onCancel) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            SectionLabel("Weight step")
            FoundationMutedText("${formatWeightStep(state.draftWeightStep)} $unitLabel")
            FitRoller(
                value = state.draftWeightStep.toFloat(),
                onValueChange = { onDraftChange(it.toDouble()) },
                min = WeightStepPreference.MIN_STEP.toFloat(),
                max = state.weightUnit.maxWeightStep().toFloat(),
                step = 0.25f,
                unit = unitLabel,
                format = { formatWeightStep(it.toDouble()) }
            )
            state.weightStepError?.let { error ->
                FoundationText(
                    text = error,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                FitButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Save",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

@Composable
private fun ExportCard(
    state: ProfileState,
    onExportRequested: (ExportType) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Export")
            ExportButton("Workouts", state.isExporting) { onExportRequested(ExportType.WORKOUTS) }
            ExportButton("Routines", state.isExporting) { onExportRequested(ExportType.ROUTINES) }
            ExportButton("Exercises", state.isExporting) { onExportRequested(ExportType.EXERCISES) }
            ExportButton("PRs", state.isExporting) { onExportRequested(ExportType.PERSONAL_RECORDS) }
            state.lastExport?.let { result ->
                FoundationMutedText(
                    "${result.type.displayName()} export: ${result.rowCount} rows, ${result.weightUnit.abbreviation()}"
                )
                FoundationMutedText(result.fileName)
            }
            state.exportError?.let { message ->
                FoundationText(
                    text = message,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
    }
}

@Composable
private fun ExercisesCard(onManageExercises: () -> Unit) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Exercises")
            FoundationMutedText("Add, edit, or archive the exercises in your library.")
            FitButton(
                text = "Manage exercises",
                onClick = onManageExercises,
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Primary
            )
        }
    }
}

@Composable
private fun InteractionCard(
    state: ProfileState,
    onPaletteModeSelected: (PaletteMode) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            SectionLabel("Preferences")
            FitSegmentedControl(
                options = listOf("Dark", "Light"),
                selectedIndex = if (state.paletteMode == PaletteMode.LIGHT) 1 else 0,
                onSelect = { index -> onPaletteModeSelected(if (index == 1) PaletteMode.LIGHT else PaletteMode.DARK) },
                modifier = Modifier.fillMaxWidth()
            )
            ToggleRow(
                label = "Haptics",
                value = if (state.hapticsEnabled) "On" else "Off",
                checked = state.hapticsEnabled,
                onCheckedChange = onHapticsChanged
            )
            ToggleRow(
                label = "Reduce motion",
                value = if (state.reduceMotion) "On" else "Off",
                checked = state.reduceMotion,
                onCheckedChange = onReduceMotionChanged
            )
        }
    }
}

@Composable
private fun ExportButton(
    label: String,
    disabled: Boolean,
    onClick: () -> Unit
) {
    FitButton(
        text = if (disabled) "Exporting" else label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !disabled,
        style = FitButtonStyle.Secondary
    )
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        FoundationMutedText(label, modifier = Modifier.weight(1f))
        FoundationText(
            text = value,
            modifier = Modifier.weight(2f),
            style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FoundationText(
                text = label,
                style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText(value)
        }
        FitToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = label
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    FoundationText(
        text = text,
        style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
    )
}

private fun WeightUnit.displayName(): String =
    when (this) {
        WeightUnit.POUNDS -> "pounds"
        WeightUnit.KILOGRAMS -> "kilograms"
    }

private fun WeightUnit.abbreviation(): String =
    when (this) {
        WeightUnit.POUNDS -> "lb"
        WeightUnit.KILOGRAMS -> "kg"
    }

private fun WeightUnit.maxWeightStep(): Double =
    when (this) {
        WeightUnit.POUNDS -> WeightStepPreference.MAX_POUNDS_STEP
        WeightUnit.KILOGRAMS -> WeightStepPreference.MAX_KILOGRAMS_STEP
    }

private fun ExportType.displayName(): String =
    when (this) {
        ExportType.WORKOUTS -> "Workout"
        ExportType.ROUTINES -> "Routine"
        ExportType.EXERCISES -> "Exercise"
        ExportType.PERSONAL_RECORDS -> "PR"
    }

private fun DeveloperSeedOutcome.displayLabel(): String =
    when (this) {
        DeveloperSeedOutcome.LOADED -> "Loaded"
        DeveloperSeedOutcome.SKIPPED -> "Skipped"
        DeveloperSeedOutcome.FAILED -> "Failed"
    }
