package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.foundation.pressable
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText

@Composable
fun ExerciseBlock(
    block: ExerciseBlockState,
    isFocused: Boolean,
    onFocus: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    onSettings: (FoundationId) -> Unit = {},
    onEditSet: (FoundationId) -> Unit = {},
    onDeleteSet: (FoundationId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    FitCard(
        modifier = modifier.pressable(onClick = onFocus),
        glow = FitTheme.glow.none
    ) {
        ExerciseBlockContent(
            block = block,
            weightUnit = weightUnit,
            showGroupSummary = true,
            isFocused = isFocused,
            onSettings = onSettings,
            onEditSet = onEditSet,
            onDeleteSet = onDeleteSet
        )
    }
}

@Composable
internal fun ExerciseBlockGroupCard(
    group: ExerciseBlockGroupState,
    focusedExerciseId: FoundationId?,
    onFocus: (FoundationId) -> Unit,
    weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    onSettings: (FoundationId) -> Unit = {},
    onEditSet: (FoundationId) -> Unit = {},
    onDeleteSet: (FoundationId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    FitCard(
        modifier = modifier,
        glow = FitTheme.glow.none
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)) {
            ExerciseGroupHeader(group)
            group.blocks.forEach { block ->
                ExerciseBlockContent(
                    block = block,
                    weightUnit = weightUnit,
                    showGroupSummary = false,
                    isFocused = focusedExerciseId == block.exerciseInstanceId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(onClick = { onFocus(block.exerciseInstanceId) }),
                    onSettings = onSettings,
                    onEditSet = onEditSet,
                    onDeleteSet = onDeleteSet
                )
            }
        }
    }
}

@Composable
private fun ExerciseGroupHeader(group: ExerciseBlockGroupState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
        ) {
            FoundationText(
                text = group.groupSummary().orEmpty(),
                style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
            )
            FoundationMutedText("${group.blocks.size} exercises")
        }
        FoundationMutedText(group.loggedSetCount.setsLabel())
    }
}

@Composable
private fun ExerciseBlockContent(
    block: ExerciseBlockState,
    weightUnit: WeightUnit,
    showGroupSummary: Boolean,
    isFocused: Boolean,
    onSettings: (FoundationId) -> Unit,
    onEditSet: (FoundationId) -> Unit,
    onDeleteSet: (FoundationId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                val titleColor = if (isFocused) FitTheme.colors.accent else FitTheme.colors.onSurface
                FoundationText(
                    text = block.displayName,
                    style = FitTheme.type.body.copy(color = titleColor)
                )
                FoundationMutedText(block.subtitle(showGroupSummary))
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
            ) {
                FoundationMutedText(block.loggedRows.size.setsLabel())
                FoundationText(
                    text = "Settings",
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.accent),
                    modifier = Modifier.pressable(onClick = { onSettings(block.exerciseInstanceId) })
                )
            }
        }
        block.loggedRows.forEach { logged ->
            LoggedSetRowView(
                row = logged,
                weightUnit = weightUnit,
                onEdit = { onEditSet(logged.setId) },
                onDelete = { onDeleteSet(logged.setId) }
            )
        }
    }
}

private fun ExerciseBlockState.groupSummary(): String? {
    val label = groupLabel ?: return null
    val rounds = groupRounds ?: return label
    return "$label x$rounds"
}

private fun ExerciseBlockGroupState.groupSummary(): String? {
    val label = groupLabel ?: return null
    val rounds = groupRounds ?: return label
    return "$label x$rounds"
}

private fun ExerciseBlockState.subtitle(showGroupSummary: Boolean): String =
    listOfNotNull(
        groupSummary().takeIf { showGroupSummary },
        when (loggingMode) {
            ExerciseLoggingMode.TIMED -> "Timed"
            ExerciseLoggingMode.BODYWEIGHT -> "Bodyweight"
            ExerciseLoggingMode.WEIGHTED -> "Weighted"
        }
    ).joinToString(" • ")

private fun Int.setsLabel(): String =
    if (this == 1) "1 set" else "$this sets"
