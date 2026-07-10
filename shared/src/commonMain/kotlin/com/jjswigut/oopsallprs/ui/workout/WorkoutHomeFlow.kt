package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitListRow
import com.jjswigut.oopsallprs.ds.component.FitStartButton
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.accessibility.foundationTouchTarget
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import com.jjswigut.oopsallprs.ui.profile.ProfileFullAccessStatus

@Composable
fun WorkoutHomeFlow(
    state: WorkoutHomeState,
    fullAccessStatus: ProfileFullAccessStatus,
    onStartEmpty: () -> Unit,
    onStartRoutine: (FoundationId) -> Unit,
    onPurchaseLifetimeUnlock: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onCreateRoutine: () -> Unit = {},
    onEditRoutine: (FoundationId) -> Unit = {},
    onRequestDeleteTemplate: (FoundationId) -> Unit = {},
    onCancelDeleteTemplate: () -> Unit = {},
    onConfirmDeleteTemplate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val hasActiveSession = state.activeSession != null
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = if (state.activeSession == null && state.templates.isEmpty()) FitTheme.spacing.xxxl else FitTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            if (state.activeSession == null && state.templates.isEmpty()) {
                FitCard(glow = FitTheme.glow.none) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                        ) {
                            FoundationText("Today", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText("No active workout")
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                        ) {
                            FoundationText("0", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
                            FoundationMutedText("routines")
                        }
                    }
                }
            }
            if (state.templates.isNotEmpty()) {
                FitCard(glow = FitTheme.glow.none) {
                    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FoundationText("Templates", modifier = Modifier.weight(1f), style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                            FoundationTextAction("New", onClick = onCreateRoutine)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                        ) {
                            items(state.templates, key = { it.templateId.value }) { template ->
                                FitListRow(
                                    onClick = if (hasActiveSession) null else {
                                        { onStartRoutine(template.templateId) }
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                                    ) {
                                        FoundationText(template.name, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                                        FoundationMutedText("${template.exerciseCount} exercises • ${template.setTargetCount} planned sets")
                                    }
                                    FoundationTextAction("Edit", onClick = { onEditRoutine(template.templateId) })
                                    FoundationTextAction("Delete", onClick = { onRequestDeleteTemplate(template.templateId) })
                                }
                            }
                        }
                        state.pendingDeleteTemplate?.let { template ->
                            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
                                FoundationText("Delete template?", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                                FoundationMutedText(template.name)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FitButton(
                                        text = "Cancel",
                                        onClick = onCancelDeleteTemplate,
                                        modifier = Modifier.weight(1f),
                                        style = FitButtonStyle.Secondary
                                    )
                                    FitButton(
                                        text = "Delete",
                                        onClick = onConfirmDeleteTemplate,
                                        modifier = Modifier.weight(1f),
                                        style = FitButtonStyle.Primary
                                    )
                                }
                            }
                        }
                    }
                }
                if (!hasActiveSession) {
                    FitStartButton(
                        text = "Start workout",
                        onClick = onStartEmpty,
                        modifier = Modifier.foundationTouchTarget("Start empty workout")
                    )
                }
            } else {
                if (!hasActiveSession) {
                    FitStartButton(
                        text = "Start workout",
                        onClick = onStartEmpty,
                        modifier = Modifier.foundationTouchTarget("Start empty workout")
                    )
                    FitButton(
                        text = "Create routine",
                        onClick = onCreateRoutine,
                        modifier = Modifier.fillMaxWidth(),
                        style = FitButtonStyle.Secondary
                    )
                }
            }
            if (state.isFullAccessPaywallVisible) {
                FullAccessRequiredCard(
                    access = fullAccessStatus,
                    onPurchaseLifetimeUnlock = onPurchaseLifetimeUnlock,
                    onRestorePurchases = onRestorePurchases
                )
            }
            state.errorMessage?.let { message ->
                FoundationText(
                    text = message,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
    }
}

@Composable
private fun FullAccessRequiredCard(
    access: ProfileFullAccessStatus,
    onPurchaseLifetimeUnlock: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    FitCard(glow = FitTheme.glow.none) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            FoundationText("You've used your free workouts.", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
            FoundationMutedText("Unlock unlimited workout logging forever.")
            FoundationMutedText(access.termsLabel)
            FoundationMutedText("Price ${access.offerLabel}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
            ) {
                FitButton(
                    text = if (access.isStoreBusy) "Working" else "Unlock forever",
                    onClick = onPurchaseLifetimeUnlock,
                    modifier = Modifier.weight(1f),
                    enabled = !access.isStoreBusy,
                    style = FitButtonStyle.Primary
                )
                FitButton(
                    text = "Restore purchase",
                    onClick = onRestorePurchases,
                    modifier = Modifier.weight(1f),
                    enabled = !access.isStoreBusy,
                    style = FitButtonStyle.Secondary
                )
            }
            access.error?.let { error ->
                FoundationText(
                    text = error,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
    }
}
