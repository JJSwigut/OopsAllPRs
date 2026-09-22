package com.jjswigut.oopsallprs.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.domain.model.FullAccessOfferState
import com.jjswigut.oopsallprs.ds.component.FitButton
import com.jjswigut.oopsallprs.ds.component.FitButtonStyle
import com.jjswigut.oopsallprs.ds.theme.FitTheme
import com.jjswigut.oopsallprs.ui.designsystem.FoundationMutedText
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText

internal const val FULL_ACCESS_BENEFITS_SUMMARY =
    "Unlimited workout logging, backup, restore, and sync."

@Composable
internal fun FullAccessPurchaseOptions(
    access: ProfileFullAccessStatus,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FitTheme.spacing.sm)) {
        FoundationText(
            FULL_ACCESS_BENEFITS_SUMMARY,
            style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface)
        )
        when (access.offerState) {
            FullAccessOfferState.Loading -> {
                FoundationMutedText("Checking the store price...")
                FitButton(
                    text = if (access.isStoreBusy) "Working..." else "Checking store price...",
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    style = FitButtonStyle.Secondary
                )
            }
            is FullAccessOfferState.Available -> {
                FoundationText(access.offerLabel, style = FitTheme.type.label.copy(color = FitTheme.colors.onSurface))
                FoundationMutedText(access.termsLabel)
                FitButton(
                    text = "Unlock for ${access.offerLabel}",
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = access.canPurchase,
                    style = FitButtonStyle.Primary
                )
            }
            is FullAccessOfferState.Unavailable -> {
                FoundationMutedText(purchaseOfferMessage(access.offerState))
                FitButton(
                    text = "Retry store connection",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !access.isStoreBusy,
                    style = FitButtonStyle.Primary
                )
            }
        }
        FitButton(
            text = "Restore purchase",
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            enabled = !access.isStoreBusy,
            style = FitButtonStyle.Secondary
        )
        FoundationMutedText("No app account required.")
        FoundationMutedText("Restore using the same store account you purchased with.")
        if (access.offerState !is FullAccessOfferState.Unavailable) {
            access.storeMessage?.let { FoundationMutedText(purchaseOperationMessage(it)) }
            access.error?.let {
                FoundationText(
                    purchaseOperationMessage(it),
                    style = FitTheme.type.caption.copy(color = FitTheme.colors.danger)
                )
            }
        }
    }
}

internal fun purchaseOfferMessage(offerState: FullAccessOfferState): String =
    when (offerState) {
        FullAccessOfferState.Loading -> "Checking the store price..."
        is FullAccessOfferState.Available -> offerState.offer.termsLabel
        is FullAccessOfferState.Unavailable -> "The store isn't available right now. Try again in a moment."
    }

internal fun purchaseOperationMessage(error: String): String =
    when {
        error.contains("cancel", ignoreCase = true) -> "The purchase was canceled."
        error.contains("restore", ignoreCase = true) ->
            "We couldn't restore a purchase. Check your store account and try again."
        else -> "The store couldn't complete that request. Try again in a moment."
    }
