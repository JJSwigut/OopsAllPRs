package com.jjswigut.oopsallprs.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileCoordinatorReadingWithoutChanges
import platform.Foundation.NSFileCoordinatorWritingForReplacing
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLBookmarkCreationWithSecurityScope
import platform.Foundation.NSURLBookmarkResolutionWithSecurityScope
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class PlatformDatabaseDriverFactory actual constructor(context: Any?) {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(WorkoutDatabase.Schema, "oops_all_prs.db")
}

actual class LocalSettingsStore actual constructor(context: Any?) {
    private val values = mutableMapOf<String, String>()
    actual fun getString(key: String): String? = values[key]
    actual fun putString(key: String, value: String) {
        values[key] = value
    }
}

actual class RestNotificationScheduler actual constructor(context: Any?) : RestAlertScheduler {
    actual override fun schedule(
        restEndsAt: Instant,
        soundEnabled: Boolean,
        persistentSurfaceEnabled: Boolean
    ): RestAlertScheduleResult = RestAlertScheduleResult.UNSUPPORTED
    actual override fun cancel() = Unit
}

actual class FileExportHandoff actual constructor(context: Any?) {
    actual fun share(fileName: String, content: String) = Unit
}

@OptIn(ExperimentalForeignApi::class)
actual class BackupDocumentHandoff actual constructor(context: Any?) : BackupDocumentAdapter {
    private val presenter: UIViewController? = context as? UIViewController
    private var activeDelegate: BackupPickerDelegate? = null

    actual override suspend fun createBackupDocument(
        suggestedName: String,
        content: String
    ): FoundationResult<BackupLinkedFile> {
        val presenter = presenter ?: return unavailable("iOS backup document presenter is unavailable")
        val tempUrl = temporaryUrl(suggestedName)
        val written = NSString.create(string = content).writeToURL(
            url = tempUrl,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        if (!written) return unavailable("Unable to prepare backup file for iOS document picker")
        return pickDocument(
            presenter,
            UIDocumentPickerViewController(tempUrl, UIDocumentPickerMode.UIDocumentPickerModeExportToService)
        )
    }

    actual override suspend fun openBackupDocument(): FoundationResult<BackupDocument> =
        when (val linked = openLinkedFile()) {
            is FoundationResult.Failure -> foundationFailure(linked.error)
            is FoundationResult.Success -> when (val content = readBackup(linked.value)) {
                is FoundationResult.Failure -> foundationFailure(content.error)
                is FoundationResult.Success -> foundationSuccess(BackupDocument(linked.value, content.value))
            }
        }

    actual override suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String> =
        withScopedUrl(linkedFile) { url ->
            var content: String? = null
            NSFileCoordinator().coordinateReadingItemAtURL(
                url = url,
                options = NSFileCoordinatorReadingWithoutChanges,
                error = null
            ) { coordinatedUrl ->
                if (coordinatedUrl == null) return@coordinateReadingItemAtURL
                content = NSString.stringWithContentsOfURL(
                    url = coordinatedUrl,
                    encoding = NSUTF8StringEncoding,
                    error = null
                ) as String?
            }
            content?.let(::foundationSuccess) ?: unavailable("Unable to read linked backup file")
        }

    actual override suspend fun writeBackup(
        linkedFile: BackupLinkedFile,
        content: String
    ): FoundationResult<BackupLinkedFile> =
        withScopedUrl(linkedFile) { url ->
            var written = false
            NSFileCoordinator().coordinateWritingItemAtURL(
                url = url,
                options = NSFileCoordinatorWritingForReplacing,
                error = null
            ) { coordinatedUrl ->
                if (coordinatedUrl == null) return@coordinateWritingItemAtURL
                written = NSString.create(string = content).writeToURL(
                    url = coordinatedUrl,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null
                )
            }
            if (written) foundationSuccess(linkedFile) else unavailable("Unable to write linked backup file")
        }

    private suspend fun openLinkedFile(): FoundationResult<BackupLinkedFile> {
        val presenter = presenter ?: return unavailable("iOS backup document presenter is unavailable")
        return pickDocument(
            presenter = presenter,
            picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.json", "public.text"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeOpen
            )
        )
    }

    private suspend fun pickDocument(
        presenter: UIViewController,
        picker: UIDocumentPickerViewController
    ): FoundationResult<BackupLinkedFile> =
        suspendCancellableCoroutine { continuation ->
            val delegate = BackupPickerDelegate { url ->
                activeDelegate = null
                if (url == null) {
                    continuation.resume(unavailable("Document picker cancelled"))
                } else {
                    continuation.resume(linkedFile(url))
                }
            }
            activeDelegate = delegate
            picker.delegate = delegate
            picker.modalPresentationStyle = 0
            presenter.presentViewController(picker, animated = true, completion = null)
            continuation.invokeOnCancellation {
                activeDelegate = null
                picker.dismissViewControllerAnimated(true, completion = null)
            }
        }

    private fun linkedFile(url: NSURL): FoundationResult<BackupLinkedFile> {
        val bookmark = url.bookmarkDataWithOptions(
            options = NSURLBookmarkCreationWithSecurityScope,
            includingResourceValuesForKeys = null,
            relativeToURL = null,
            error = null
        ) ?: return unavailable("Unable to create security-scoped bookmark")
        return foundationSuccess(
            BackupLinkedFile(
                displayName = url.lastPathComponent ?: "Backup file",
                providerReference = bookmark.base64EncodedStringWithOptions(0u),
                providerReferenceKind = "ios-security-scoped-bookmark"
            )
        )
    }

    private fun <T> withScopedUrl(
        linkedFile: BackupLinkedFile,
        block: (NSURL) -> FoundationResult<T>
    ): FoundationResult<T> {
        val url = resolveUrl(linkedFile) ?: return unavailable("Unable to resolve linked backup bookmark")
        val didAccess = url.startAccessingSecurityScopedResource()
        return try {
            block(url)
        } finally {
            if (didAccess) url.stopAccessingSecurityScopedResource()
        }
    }

    private fun resolveUrl(linkedFile: BackupLinkedFile): NSURL? {
        val bookmark = NSData.create(
            base64EncodedString = linkedFile.providerReference,
            options = NSDataBase64DecodingIgnoreUnknownCharacters
        ) ?: return NSURL.URLWithString(linkedFile.providerReference)
        return NSURL.URLByResolvingBookmarkData(
            bookmarkData = bookmark,
            options = NSURLBookmarkResolutionWithSecurityScope,
            relativeToURL = null,
            bookmarkDataIsStale = null,
            error = null
        )
    }

    private fun temporaryUrl(fileName: String): NSURL =
        NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)

    private fun <T> unavailable(message: String): FoundationResult<T> =
        foundationFailure(FoundationError.Platform(message))

    private class BackupPickerDelegate(
        private val onComplete: (NSURL?) -> Unit
    ) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>
        ) {
            onComplete(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onComplete(null)
        }
    }
}

actual class FullAccessBillingHandoff actual constructor(private val context: Any?) : FullAccessBillingAdapter {
    actual override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> =
        unavailable()

    actual override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot> =
        unavailable()

    actual override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot> =
        unavailable()

    actual override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot> =
        unavailable()

    private fun <T> unavailable(): FoundationResult<T> =
        foundationFailure(FoundationError.Platform(UNAVAILABLE_MESSAGE))

    private companion object {
        const val UNAVAILABLE_MESSAGE = "App Store purchases are not configured for this build."
    }
}

actual class HapticFeedback actual constructor(context: Any?) {
    actual fun setLogged() = Unit
    actual fun warning() = Unit
}

actual class PlatformClock actual constructor() {
    actual fun now(): Instant = Clock.System.now()
}
