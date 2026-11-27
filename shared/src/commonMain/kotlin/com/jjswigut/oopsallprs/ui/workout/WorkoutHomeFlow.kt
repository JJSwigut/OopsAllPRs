package com.jjswigut.oopsallprs.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.component.FitCard
import com.jjswigut.oopsallprs.ds.component.FitDialog
import com.jjswigut.oopsallprs.ds.component.FitStartButton
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.accessibility.foundationTouchTarget
import com.jjswigut.oopsallprs.ui.common.countLabel
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationTextAction
import com.jjswigut.oopsallprs.ui.profile.ProfileFullAccessStatus
import com.jjswigut.oopsallprs.ui.profile.FullAccessPurchaseOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutHomeFlow(
    state: WorkoutHomeState,
    fullAccessStatus: ProfileFullAccessStatus,
    onStartEmpty: () -> Unit,
    onStartRoutine: (FoundationId) -> Unit,
    onPurchaseLifetimeUnlock: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onRetryStoreOffer: () -> Unit = {},
    onDismissFullAccessPaywall: () -> Unit = {},
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = FitTheme.spacing.xl, bottom = if (hasActiveSession) FitTheme.spacing.xl else 112.dp),
            verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)) {
                FoundationText("Train", style = FitTheme.type.title.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText(
                    when {
                        hasActiveSession -> "A workout is already in progress"
                        state.templates.isEmpty() -> "Start your first workout"
                        else -> "Start fresh or use a saved template"
                    }
                )
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = FitTheme.size.touchMin),
                                    verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                                    ) {
                                        FoundationText(template.name, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                                        FoundationMutedText(
                                            "${template.exerciseCount.countLabel("exercise")} • " +
                                                template.setTargetCount.countLabel("planned set", "planned sets")
                                        )
                                    }
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.xs)
                                    ) {
                                        if (!hasActiveSession) {
                                            FoundationTextAction("Start", onClick = { onStartRoutine(template.templateId) })
                                        }
                                        FoundationTextAction("Edit", onClick = { onEditRoutine(template.templateId) })
                                        FoundationTextAction("Delete", onClick = { onRequestDeleteTemplate(template.templateId) })
                                    }
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
            } else {
                if (!hasActiveSession) {
                    FitButton(
                        text = "Create routine",
                        onClick = onCreateRoutine,
                        modifier = Modifier.fillMaxWidth(),
                        style = FitButtonStyle.Secondary
                    )
                }
            }
            state.errorMessage?.let { message ->
                FoundationText(
                    text = message,
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
        if (!hasActiveSession) {
            FitStartButton(
                text = if (state.templates.isEmpty()) "Start workout" else "Start empty workout",
                onClick = onStartEmpty,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = FitTheme.spacing.xl)
                    .foundationTouchTarget("Start empty workout")
            )
        }
        if (state.isFullAccessPaywallVisible) {
            FullAccessRequiredDialog(
                access = fullAccessStatus,
                onPurchaseLifetimeUnlock = onPurchaseLifetimeUnlock,
                onRestorePurchases = onRestorePurchases,
                onRetryStoreOffer = onRetryStoreOffer,
                onDismiss = onDismissFullAccessPaywall
            )
        }
    }
}

@Composable
private fun FullAccessRequiredDialog(
    access: ProfileFullAccessStatus,
    onPurchaseLifetimeUnlock: () -> Unit,
    onRestorePurchases: () -> Unit,
    onRetryStoreOffer: () -> Unit,
    onDismiss: () -> Unit
) {
    FitDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState())
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
            FoundationText("You've used your free workouts.", style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
            FoundationMutedText("Unlock unlimited workout logging forever.")
            FullAccessPurchaseOptions(access, onPurchaseLifetimeUnlock, onRestorePurchases, onRetryStoreOffer)
            FitButton(
                text = "Not now",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Secondary
            )
        }
    }
}
