package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
    onEditSet: (FoundationId) -> Unit = {},
    onDeleteSet: (FoundationId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    FitCard(
        modifier = modifier.pressable(onClick = onFocus),
        glow = FitTheme.glow.none
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                ) {
                    FoundationText(
                        text = block.displayName,
                        style = FitTheme.type.body.copy(color = FitTheme.colors.onSurface)
                    )
                    FoundationMutedText(
                        listOfNotNull(
                            block.groupSummary(),
                            when (block.loggingMode) {
                            ExerciseLoggingMode.TIMED -> "Timed"
                            ExerciseLoggingMode.BODYWEIGHT -> "Bodyweight"
                            ExerciseLoggingMode.WEIGHTED -> "Weighted"
                            }
                        ).joinToString(" • ")
                    )
                }
                FoundationMutedText("${block.loggedRows.size} sets")
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
}

private fun ExerciseBlockState.groupSummary(): String? {
    val label = groupLabel ?: return null
    val rounds = groupRounds ?: return label
    return "$label x$rounds"
}
