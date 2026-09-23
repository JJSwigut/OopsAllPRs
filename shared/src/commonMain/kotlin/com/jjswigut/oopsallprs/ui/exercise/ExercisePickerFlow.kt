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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitTextField
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationActionButton
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText

@Composable
fun ExercisePickerFlow(
    state: ExercisePickerState,
    onQueryChange: (String) -> Unit,
    onSelect: (ExercisePickerResultRow) -> Unit,
    onShowCreate: () -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomLoggingModeChange: (ExerciseLoggingMode) -> Unit,
    onCreateExercise: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        if (state.isCreatingCustom) {
            CustomExerciseForm(
                draft = state.customDraft,
                isSaving = state.isSaving,
                onNameChange = onCustomNameChange,
                onLoggingModeChange = onCustomLoggingModeChange,
                onCreate = onCreateExercise,
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                FoundationText(
                    text = "Add exercise",
                    modifier = Modifier.semantics { heading() },
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
                ExerciseResultList(
                    state = state,
                    onShowCreate = onShowCreate,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        PickerBottomControls(
            state = state,
            onQueryChange = onQueryChange,
            onShowCreate = onShowCreate,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PickerBottomControls(
    state: ExercisePickerState,
    onQueryChange: (String) -> Unit,
    onShowCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
    ) {
        if (!state.isCreatingCustom) {
            FitTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search exercises"
            )
        }
        state.errorMessage?.let { message ->
            FoundationText(
                text = message,
                style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FitButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                style = FitButtonStyle.Secondary
            )
            if (!state.isCreatingCustom) {
                FitButton(
                    text = "Custom",
                    onClick = onShowCreate,
                    modifier = Modifier.weight(1f),
                    style = FitButtonStyle.Secondary
                )
            }
        }
    }
}

@Composable
private fun ExerciseResultList(
    state: ExercisePickerState,
    onShowCreate: () -> Unit,
    onSelect: (ExercisePickerResultRow) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
    ) {
        if (state.query.isBlank() && state.recentResults.isNotEmpty()) {
            item(key = "recent-label") {
                FoundationMutedText("Recently used")
            }
            items(
                items = state.recentResults,
                key = { row -> "recent-${row.exerciseCatalogId.value}" }
            ) { row ->
                ExerciseResultRow(
                    row = row,
                    isSaving = state.isSaving,
                    onSelect = { onSelect(row) }
                )
            }
            item(key = "all-label") {
                FoundationMutedText("All exercises")
            }
        }
        if (state.results.isEmpty()) {
            item(key = "empty") {
                FitCard(modifier = Modifier.fillMaxWidth(), glow = FitTheme.glow.none) {
                    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                        FoundationText("No local matches")
                        FoundationMutedText("Create it locally and keep logging.")
                        FoundationActionButton(
                            label = "Create exercise",
                            onClick = onShowCreate,
                            primary = false
                        )
                    }
                }
            }
        } else {
            items(
                items = state.results,
                key = { row -> "all-${row.exerciseCatalogId.value}" }
            ) { row ->
                ExerciseResultRow(
                    row = row,
                    isSaving = state.isSaving,
                    onSelect = { onSelect(row) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseResultRow(
    row: ExercisePickerResultRow,
    isSaving: Boolean,
    onSelect: () -> Unit
) {
    FitListRow(onClick = onSelect) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            FoundationText(
                text = row.displayName,
                style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText(text = exerciseMetadataLine(row.subtitle, row.loggingMode, separator = " · "))
        }
        FitButton(
            text = if (isSaving) "Adding..." else "Add",
            onClick = onSelect,
            style = FitButtonStyle.Primary
        )
    }
}

@Composable
private fun CustomExerciseForm(
    draft: CustomExerciseDraft,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onLoggingModeChange: (ExerciseLoggingMode) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
    ) {
        FitCard(modifier = Modifier.fillMaxWidth(), glow = FitTheme.glow.none) {
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
                FoundationText(
                    text = "Custom exercise",
                    style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface)
                )
                FitTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    placeholder = "Exercise name",
                    selectAllOnFocus = true
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
                FoundationActionButton(
                    label = if (isSaving) "Creating..." else "Create and add",
                    onClick = onCreate,
                    primary = true
                )
            }
        }
    }
}
