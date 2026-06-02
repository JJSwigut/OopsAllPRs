package com.jjswigut.oopsallprs.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction

@Composable
fun ExerciseManagementFlow(
    state: ExerciseManagementState,
    onQueryChange: (String) -> Unit,
    onBeginCreate: () -> Unit,
    onBeginEdit: (ManagedExerciseRow) -> Unit,
    onRequestArchive: (ManagedExerciseRow) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onDraftLoggingModeChange: (ExerciseLoggingMode) -> Unit,
    onSaveDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    onConfirmArchive: () -> Unit,
    onCancelArchive: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            item(key = "header") {
                FitCard(glow = FitTheme.glow.none) {
                    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                        FoundationText("Exercises", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
                        FoundationMutedText("User-created exercises can be renamed or archived. Seeded exercises stay read-only.")
                        FitTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            placeholder = "Search custom exercises",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            state.draft?.let { draft ->
                item(key = "draft") {
                    ExerciseDraftCard(
                        draft = draft,
                        onNameChange = onDraftNameChange,
                        onLoggingModeChange = onDraftLoggingModeChange,
                        onSave = onSaveDraft,
                        onCancel = onCancelDraft
                    )
                }
            }
            state.pendingArchive?.let { row ->
                item(key = "archive") {
                    ArchiveCard(row, onConfirmArchive, onCancelArchive)
                }
            }
            if (state.rows.isEmpty()) {
                item(key = "empty") {
                    FitCard(glow = FitTheme.glow.none) {
                        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                            FoundationText("No custom exercises")
                            FoundationMutedText("Create one when the seeded catalog does not cover your movement.")
                        }
                    }
                }
            } else {
                items(state.rows, key = { it.exerciseCatalogId.value }) { row ->
                    ManagedExerciseRowView(
                        row = row,
                        onEdit = { onBeginEdit(row) },
                        onArchive = { onRequestArchive(row) }
                    )
                }
            }
        }
        state.errorMessage?.let { message ->
            FoundationText(text = message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FitButton(text = "Close", onClick = onClose, modifier = Modifier.weight(1f), style = FitButtonStyle.Secondary)
            FitButton(text = "New exercise", onClick = onBeginCreate, modifier = Modifier.weight(1f), style = FitButtonStyle.Primary)
        }
    }
}

@Composable
private fun ManagedExerciseRowView(
    row: ManagedExerciseRow,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    FitListRow {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
            FoundationText(row.displayName, style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface))
            FoundationMutedText(
                exerciseMetadataLine(row.subtitle, row.loggingMode, separator = " • ")
            )
        }
        FoundationTextAction("Edit", onEdit)
        FoundationTextAction("Archive", onArchive)
    }
}

@Composable
private fun ExerciseDraftCard(
    draft: ExerciseEditDraft,
    onNameChange: (String) -> Unit,
    onLoggingModeChange: (ExerciseLoggingMode) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            FoundationText(
                text = if (draft.isEditing) "Edit exercise" else "Create exercise",
                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
            )
            FitTextField(
                value = draft.name,
                onValueChange = onNameChange,
                placeholder = "Exercise name",
                selectAllOnFocus = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                FitButton(
                    text = "Weighted",
                    onClick = { onLoggingModeChange(ExerciseLoggingMode.WEIGHTED) },
                    modifier = Modifier.weight(1f),
                    style = if (draft.loggingMode == ExerciseLoggingMode.WEIGHTED) FitButtonStyle.Primary else FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Bodyweight",
                    onClick = { onLoggingModeChange(ExerciseLoggingMode.BODYWEIGHT) },
                    modifier = Modifier.weight(1f),
                    style = if (draft.loggingMode == ExerciseLoggingMode.BODYWEIGHT) FitButtonStyle.Primary else FitButtonStyle.Secondary
                )
                FitButton(
                    text = "Timed",
                    onClick = { onLoggingModeChange(ExerciseLoggingMode.TIMED) },
                    modifier = Modifier.weight(1f),
                    style = if (draft.loggingMode == ExerciseLoggingMode.TIMED) FitButtonStyle.Primary else FitButtonStyle.Secondary
                )
            }
            draft.errorMessage?.let { message ->
                FoundationText(text = message, style = FitTheme.type.caption.copy(color = FitTheme.colors.danger))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                FitButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f), style = FitButtonStyle.Secondary)
                FitButton(
                    text = if (draft.isSaving) "Saving" else "Save",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = draft.canSave,
                    style = FitButtonStyle.Primary
                )
            }
        }
    }
}

@Composable
private fun ArchiveCard(
    row: ManagedExerciseRow,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            FoundationText("Archive exercise?", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
            FoundationMutedText(row.displayName)
            Row(horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                FitButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f), style = FitButtonStyle.Secondary)
                FitButton(text = "Archive", onClick = onConfirm, modifier = Modifier.weight(1f), style = FitButtonStyle.Primary)
            }
        }
    }
}
