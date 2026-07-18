package com.jjswigut.oopsallprs.platform

import android.Manifest
import android.app.AlarmManager
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.UnfetchedProduct
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.FullAccessBillingProductIds
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.resume

actual class PlatformDatabaseDriverFactory actual constructor(private val context: Any?) {
    actual fun createDriver(): SqlDriver {
        val androidContext = context as? Context
            ?: error("Android database driver requires an android.content.Context")
        repairRoutineGroupRoundsMigration(androidContext)
        repairBackupSyncStateMigration(androidContext)
        repairFullAccessStateMigration(androidContext)
        return AndroidSqliteDriver(WorkoutDatabase.Schema, androidContext, "oops_all_prs.db")
    }

    private fun repairRoutineGroupRoundsMigration(context: Context) {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (!databaseFile.exists()) return

        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { database ->
            if (database.userVersion() != GROUP_ROUNDS_SCHEMA_VERSION - 1) return

            val routineColumnPresent = database.hasColumn("routine_exercises", "group_rounds")
            val activeColumnPresent = database.hasColumn("active_exercises", "group_rounds")
            if (!routineColumnPresent) {
                database.execSQL("ALTER TABLE routine_exercises ADD COLUMN group_rounds INTEGER")
            }
            if (!activeColumnPresent) {
                database.execSQL("ALTER TABLE active_exercises ADD COLUMN group_rounds INTEGER")
            }

            if (
                database.hasColumn("routine_exercises", "group_rounds") &&
                database.hasColumn("active_exercises", "group_rounds")
            ) {
                database.execSQL("PRAGMA user_version = $GROUP_ROUNDS_SCHEMA_VERSION")
            }
        }
    }

    private fun repairBackupSyncStateMigration(context: Context) {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (!databaseFile.exists()) return

        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { database ->
            if (database.userVersion() < BACKUP_SYNC_SCHEMA_VERSION) return
            if (database.hasTable("sync_state")) return

            database.execSQL(
                """
                CREATE TABLE sync_state (
                    singleton_id INTEGER NOT NULL PRIMARY KEY CHECK (singleton_id = 1),
                    linked_backup_display_name TEXT,
                    provider_reference TEXT,
                    provider_reference_kind TEXT,
                    last_backup_revision TEXT,
                    last_backup_timestamp INTEGER,
                    last_local_revision TEXT,
                    last_local_timestamp INTEGER,
                    last_outcome TEXT NOT NULL,
                    last_error TEXT,
                    last_conflict_summary TEXT,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL("PRAGMA user_version = $BACKUP_SYNC_SCHEMA_VERSION")
        }
    }

    private fun repairFullAccessStateMigration(context: Context) {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (!databaseFile.exists()) return

        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { database ->
            if (database.userVersion() < FULL_ACCESS_SCHEMA_VERSION) return
            if (database.hasTable("full_access_state")) return

            database.execSQL(
                """
                CREATE TABLE full_access_state (
                    singleton_id INTEGER NOT NULL PRIMARY KEY CHECK (singleton_id = 1),
                    completed_free_workouts INTEGER NOT NULL,
                    lifetime_active INTEGER NOT NULL,
                    store_status TEXT NOT NULL,
                    last_error TEXT,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL("PRAGMA user_version = $FULL_ACCESS_SCHEMA_VERSION")
        }
    }

    private fun SQLiteDatabase.userVersion(): Int {
        val cursor = rawQuery("PRAGMA user_version", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun SQLiteDatabase.hasColumn(table: String, column: String): Boolean {
        val cursor = rawQuery("PRAGMA table_info($table)", null)
        return cursor.use {
            val nameColumnIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (it.getString(nameColumnIndex) == column) return@use true
            }
            false
        }
    }

    private fun SQLiteDatabase.hasTable(table: String): Boolean {
        val cursor = rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        )
        return cursor.use { it.moveToFirst() }
    }

    private companion object {
        const val DATABASE_NAME = "oops_all_prs.db"
        const val GROUP_ROUNDS_SCHEMA_VERSION = 7
        const val BACKUP_SYNC_SCHEMA_VERSION = 8
        const val FULL_ACCESS_SCHEMA_VERSION = 9
    }
}

actual class LocalSettingsStore actual constructor(private val context: Any?) {
    private val preferences by lazy {
        val androidContext = context as? Context
            ?: error("Android settings store requires an android.content.Context")
        androidContext.getSharedPreferences("oops_all_prs_settings", Context.MODE_PRIVATE)
    }

    actual fun getString(key: String): String? = preferences.getString(key, null)

    actual fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}

actual class RestNotificationScheduler actual constructor(private val context: Any?) : RestAlertScheduler {
    private val permissionLauncher: ActivityResultLauncher<String>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context as? ComponentActivity)?.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val pending = pendingActiveRest
                pendingActiveRest = null
                if (granted && pending != null && pending.restEndsAt.toEpochMilliseconds() > System.currentTimeMillis()) {
                    showOngoingNotification(pending)
                }
            }
        } else {
            null
        }
    private var pendingActiveRest: ActiveRestNotification? = null

    actual override fun schedule(
        restEndsAt: Instant,
        soundEnabled: Boolean,
        persistentSurfaceEnabled: Boolean
    ): RestAlertScheduleResult {
        val androidContext = context as? Context ?: return RestAlertScheduleResult.UNSUPPORTED
        cancelScheduledAlarm(androidContext)
        notificationManager(androidContext).cancel(ACTIVE_REST_NOTIFICATION_ID)
        val intent = restTimerIntent(androidContext).putExtra(RestTimerReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
        val pendingIntent = PendingIntent.getBroadcast(
            androidContext,
            REST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = androidContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restEndsAt.toEpochMilliseconds(), pendingIntent)

        val activeRest = ActiveRestNotification(restEndsAt)
        pendingActiveRest = activeRest.takeIf { persistentSurfaceEnabled }
        if (!canPostNotifications(androidContext)) {
            requestNotificationPermission()
            return RestAlertScheduleResult.PERMISSION_DENIED
        }
        pendingActiveRest = null
        return if (!persistentSurfaceEnabled || showOngoingNotification(activeRest)) {
            RestAlertScheduleResult.SCHEDULED
        } else {
            RestAlertScheduleResult.PERMISSION_DENIED
        }
    }

    actual override fun cancel() {
        val androidContext = context as? Context ?: return
        pendingActiveRest = null
        cancelScheduledAlarm(androidContext)
        notificationManager(androidContext).cancel(ACTIVE_REST_NOTIFICATION_ID)
    }

    private fun cancelScheduledAlarm(androidContext: Context) {
        val pendingIntent = PendingIntent.getBroadcast(
            androidContext,
            REST_REQUEST_CODE,
            restTimerIntent(androidContext),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = androidContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun showOngoingNotification(activeRest: ActiveRestNotification): Boolean {
        val androidContext = context as? Context ?: return false
        val manager = notificationManager(androidContext)
        manager.ensureActiveRestChannel()
        val openApp = androidContext.packageManager.getLaunchIntentForPackage(androidContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = openApp?.let { intent ->
            PendingIntent.getActivity(
                androidContext,
                REST_OPEN_APP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification = android.app.Notification.Builder(androidContext, CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Rest timer")
            .setContentText("Rest in progress")
            .setCategory(android.app.Notification.CATEGORY_STOPWATCH)
            .setWhen(activeRest.restEndsAt.toEpochMilliseconds())
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
        return try {
            manager.notify(ACTIVE_REST_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching { permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun restTimerIntent(context: Context): Intent =
        Intent(context, RestTimerReceiver::class.java).setAction(RestTimerReceiver.ACTION_REST_DONE)

    private data class ActiveRestNotification(val restEndsAt: Instant)
}

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REST_DONE) return
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ACTIVE_REST_NOTIFICATION_ID)
        val channelId = if (soundEnabled) CHANNEL_SOUND else CHANNEL_SILENT
        notificationManager.ensureRestChannel(channelId, soundEnabled)
        val notification = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Rest complete")
            .setContentText("Time for the next set.")
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(REST_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission may be denied; shared state still recovers on return.
        }
    }

    private fun NotificationManager.ensureRestChannel(channelId: String, soundEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getNotificationChannel(channelId) != null) return
        val channel = NotificationChannel(
            channelId,
            "Rest timer",
            if (soundEnabled) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        )
        if (soundEnabled) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            channel.setSound(sound, attributes)
        } else {
            channel.setSound(null, null)
        }
        createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_REST_DONE = "com.jjswigut.oopsallprs.REST_DONE"
        const val EXTRA_SOUND_ENABLED = "sound_enabled"
    }
}

private fun notificationManager(context: Context): NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

private fun NotificationManager.ensureActiveRestChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getNotificationChannel(CHANNEL_ACTIVE) != null) return
    createNotificationChannel(
        NotificationChannel(CHANNEL_ACTIVE, "Active rest timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the current rest countdown while a timer is active."
            setSound(null, null)
            enableVibration(false)
        }
    )
}

actual class FileExportHandoff actual constructor(private val context: Any?) {
    actual fun share(fileName: String, content: String) {
        val androidContext = context as? Context
            ?: error("Android export handoff requires an android.content.Context")
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, fileName)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, fileName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        androidContext.startActivity(chooser)
    }
}

actual class BackupDocumentHandoff actual constructor(private val context: Any?) : BackupDocumentAdapter {
    private val androidContext: Context? = context as? Context
    private var pendingCreate: PendingCreate? = null
    private var pendingOpen: CompletableDeferred<FoundationResult<BackupDocument>>? = null
    private val createLauncher: ActivityResultLauncher<String>? =
        (context as? ComponentActivity)?.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val pending = pendingCreate
            pendingCreate = null
            if (pending == null) return@registerForActivityResult
            if (uri == null) {
                pending.deferred.complete(foundationFailure(FoundationError.Platform("Backup file creation canceled")))
                return@registerForActivityResult
            }
            persistUri(uri)
            val linked = linkedFile(uri)
            when (val write = writeBackupContent(linked, pending.content)) {
                is FoundationResult.Failure -> pending.deferred.complete(write)
                is FoundationResult.Success -> pending.deferred.complete(foundationSuccess(write.value))
            }
        }
    private val openLauncher: ActivityResultLauncher<Array<String>>? =
        (context as? ComponentActivity)?.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val pending = pendingOpen
            pendingOpen = null
            if (pending == null) return@registerForActivityResult
            if (uri == null) {
                pending.complete(foundationFailure(FoundationError.Platform("Backup file selection canceled")))
                return@registerForActivityResult
            }
            persistUri(uri)
            val linked = linkedFile(uri)
            when (val read = readBackupContent(linked)) {
                is FoundationResult.Failure -> pending.complete(read)
                is FoundationResult.Success -> pending.complete(foundationSuccess(BackupDocument(linked, read.value)))
            }
        }

    actual override suspend fun createBackupDocument(
        suggestedName: String,
        content: String
    ): FoundationResult<BackupLinkedFile> {
        val launcher = createLauncher
            ?: return foundationFailure(FoundationError.Platform("Backup document picker unavailable"))
        val deferred = CompletableDeferred<FoundationResult<BackupLinkedFile>>()
        pendingCreate = PendingCreate(content, deferred)
        launcher.launch(suggestedName)
        return deferred.await()
    }

    actual override suspend fun openBackupDocument(): FoundationResult<BackupDocument> {
        val launcher = openLauncher
            ?: return foundationFailure(FoundationError.Platform("Backup document picker unavailable"))
        val deferred = CompletableDeferred<FoundationResult<BackupDocument>>()
        pendingOpen = deferred
        launcher.launch(arrayOf("application/json", "text/*", "*/*"))
        return deferred.await()
    }

    actual override suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String> =
        readBackupContent(linkedFile)

    private fun readBackupContent(linkedFile: BackupLinkedFile): FoundationResult<String> {
        val context = androidContext
            ?: return foundationFailure(FoundationError.Platform("Android backup adapter requires Context"))
        val uri = linkedFile.toUri()
            ?: return foundationFailure(FoundationError.Platform("Linked backup reference is invalid"))
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return foundationFailure(FoundationError.Platform("Linked backup file could not be read"))
            foundationSuccess(content)
        } catch (throwable: Throwable) {
            foundationFailure(FoundationError.Platform(throwable.message ?: "Linked backup file could not be read"))
        }
    }

    actual override suspend fun writeBackup(
        linkedFile: BackupLinkedFile,
        content: String
    ): FoundationResult<BackupLinkedFile> =
        writeBackupContent(linkedFile, content)

    private fun writeBackupContent(
        linkedFile: BackupLinkedFile,
        content: String
    ): FoundationResult<BackupLinkedFile> {
        val context = androidContext
            ?: return foundationFailure(FoundationError.Platform("Android backup adapter requires Context"))
        val uri = linkedFile.toUri()
            ?: return foundationFailure(FoundationError.Platform("Linked backup reference is invalid"))
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
                ?: return foundationFailure(FoundationError.Platform("Linked backup file could not be written"))
            foundationSuccess(linkedFile)
        } catch (throwable: Throwable) {
            foundationFailure(FoundationError.Platform(throwable.message ?: "Linked backup file could not be written"))
        }
    }

    private fun persistUri(uri: Uri) {
        val context = androidContext ?: return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun linkedFile(uri: Uri): BackupLinkedFile =
        BackupLinkedFile(
            displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Oops All PRs backup",
            providerReference = uri.toString(),
            providerReferenceKind = "android-uri"
        )

    private fun BackupLinkedFile.toUri(): Uri? =
        runCatching { Uri.parse(providerReference) }.getOrNull()

    private data class PendingCreate(
        val content: String,
        val deferred: CompletableDeferred<FoundationResult<BackupLinkedFile>>
    )
}

actual class FullAccessBillingHandoff actual constructor(private val context: Any?) :
    FullAccessBillingAdapter,
    PurchasesUpdatedListener {

    private val androidContext = context as? Context
    private val activity = context as? Activity
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingPurchase: CompletableDeferred<FoundationResult<FullAccessEntitlementSnapshot>>? = null
    private var pendingConnection: CompletableDeferred<FoundationResult<BillingClient>>? = null
    private val billingClient: BillingClient? = androidContext?.let { ctx ->
        BillingClient.newBuilder(ctx)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    actual override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> {
        return when (val result = queryProductDetails(listOf(lifetimeProductQuery()))) {
            is FoundationResult.Failure -> foundationSuccess(listOf(fallbackLifetimeOffer()))
            is FoundationResult.Success -> {
                val productDetails = when (val product = result.value.lifetimeProductDetails()) {
                    is FoundationResult.Failure -> return product
                    is FoundationResult.Success -> product.value
                }
                val offer = when (val storeOffer = productDetails.toLifetimeStoreOffer()) {
                    is FoundationResult.Failure -> return storeOffer
                    is FoundationResult.Success -> storeOffer.value
                }
                foundationSuccess(listOf(offer))
            }
        }
    }

    actual override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot> =
        entitlementSnapshot()

    actual override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot> =
        entitlementSnapshot()

    actual override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot> {
        val client = billingClient ?: return unavailable()
        val launchActivity = activity ?: return foundationFailure(
            FoundationError.Platform("Google Play purchases require an Android Activity context.")
        )
        pendingPurchase?.let {
            return foundationFailure(FoundationError.Platform("A purchase is already in progress."))
        }
        val productDetailsQuery = when (val result = queryProductDetails(listOf(lifetimeProductQuery()))) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        val productDetails = when (val product = productDetailsQuery.lifetimeProductDetails()) {
            is FoundationResult.Failure -> return product
            is FoundationResult.Success -> product.value
        }
        val selectedOffer = productDetails.selectLifetimePurchaseOption()
            ?: return lifetimePurchaseOptionUnavailable()
        val selectedOfferToken = selectedOffer.token
            ?: return lifetimePurchaseOptionUnavailable()
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(selectedOfferToken)
            .build()
        val purchaseResult = CompletableDeferred<FoundationResult<FullAccessEntitlementSnapshot>>()
        pendingPurchase = purchaseResult
        val billingResult = runCatching {
            client.launchBillingFlow(
                launchActivity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build()
            )
        }.getOrElse { throwable ->
            pendingPurchase = null
            return billingFailure(throwable, "Could not launch Google Play purchase flow.")
        }
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return billingFailure(billingResult, "Could not launch Google Play purchase flow.")
        }
        return purchaseResult.await()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val purchaseResult = pendingPurchase ?: return
        pendingPurchase = null
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    purchaseResult.complete(processPurchaseUpdates(purchases.orEmpty()))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseResult.complete(
                    foundationFailure(FoundationError.Platform("Purchase cancelled."))
                )
            }
            else -> {
                purchaseResult.complete(
                    billingFailure(billingResult, "Google Play purchase failed.")
                )
            }
        }
    }

    private suspend fun processPurchaseUpdates(purchases: List<Purchase>): FoundationResult<FullAccessEntitlementSnapshot> {
        val relevant = purchases.filter { purchase ->
            purchase.products.any { it == LIFETIME_UNLOCK_PRODUCT_ID }
        }
        val completed = relevant.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (completed.isEmpty()) {
            return if (relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                foundationFailure(FoundationError.Platform("Purchase is pending. Full Access unlocks after Google Play confirms payment."))
            } else {
                foundationFailure(FoundationError.Platform("Google Play did not return a completed Full Access purchase."))
            }
        }
        completed.forEach { purchase ->
            when (val acknowledged = acknowledgeIfNeeded(purchase)) {
                is FoundationResult.Failure -> return acknowledged
                is FoundationResult.Success -> Unit
            }
        }
        return lifetimeUnlockedSnapshot()
    }

    private suspend fun entitlementSnapshot(): FoundationResult<FullAccessEntitlementSnapshot> {
        val inAppPurchases = when (val result = queryPurchases(BillingClient.ProductType.INAPP)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        val lifetimePurchases = inAppPurchases.filter { purchase ->
            purchase.products.any { it == LIFETIME_UNLOCK_PRODUCT_ID }
        }
        lifetimePurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                when (val acknowledged = acknowledgeIfNeeded(purchase)) {
                    is FoundationResult.Failure -> return acknowledged
                    is FoundationResult.Success -> Unit
                }
            }
        return foundationSuccess(
            FullAccessEntitlementSnapshot(
                lifetimeUnlocked = lifetimePurchases.anyActiveProduct(LIFETIME_UNLOCK_PRODUCT_ID),
                storeStatus = FullAccessStoreStatus.AVAILABLE,
                message = if (lifetimePurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    "Purchase is pending. Full Access unlocks after Google Play confirms payment."
                } else {
                    null
                }
            )
        )
    }

    private suspend fun queryProductDetails(
        products: List<QueryProductDetailsParams.Product>
    ): FoundationResult<ProductDetailsQuery> {
        val client = when (val connected = connectedClient()) {
            is FoundationResult.Failure -> return connected
            is FoundationResult.Success -> connected.value
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        return suspendCancellableCoroutine { continuation ->
            runCatching {
                client.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                    if (!continuation.isActive) return@queryProductDetailsAsync
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(
                            foundationSuccess(
                                ProductDetailsQuery(
                                    productDetails = productDetailsResult.productDetailsList,
                                    unfetchedProducts = productDetailsResult.unfetchedProductList
                                )
                            )
                        )
                    } else {
                        continuation.resume(billingFailure(billingResult, "Could not load Google Play products."))
                    }
                }
            }.onFailure { throwable ->
                if (continuation.isActive) {
                    continuation.resume(billingFailure(throwable, "Could not load Google Play products."))
                }
            }
        }
    }

    private suspend fun queryPurchases(productType: String): FoundationResult<List<Purchase>> {
        val client = when (val connected = connectedClient()) {
            is FoundationResult.Failure -> return connected
            is FoundationResult.Success -> connected.value
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()
        return suspendCancellableCoroutine { continuation ->
            runCatching {
                client.queryPurchasesAsync(params) { billingResult, purchases ->
                    if (!continuation.isActive) return@queryPurchasesAsync
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(foundationSuccess(purchases))
                    } else {
                        continuation.resume(billingFailure(billingResult, "Could not restore Google Play purchases."))
                    }
                }
            }.onFailure { throwable ->
                if (continuation.isActive) {
                    continuation.resume(billingFailure(throwable, "Could not restore Google Play purchases."))
                }
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase): FoundationResult<Unit> {
        if (purchase.isAcknowledged || purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return foundationSuccess(Unit)
        }
        val client = when (val connected = connectedClient()) {
            is FoundationResult.Failure -> return connected
            is FoundationResult.Success -> connected.value
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        return suspendCancellableCoroutine { continuation ->
            runCatching {
                client.acknowledgePurchase(params) { billingResult ->
                    if (!continuation.isActive) return@acknowledgePurchase
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(foundationSuccess(Unit))
                    } else {
                        continuation.resume(billingFailure(billingResult, "Could not acknowledge Google Play purchase."))
                    }
                }
            }.onFailure { throwable ->
                if (continuation.isActive) {
                    continuation.resume(billingFailure(throwable, "Could not acknowledge Google Play purchase."))
                }
            }
        }
    }

    private suspend fun connectedClient(): FoundationResult<BillingClient> {
        val client = billingClient ?: return unavailable()
        if (client.isReady) return foundationSuccess(client)
        pendingConnection?.let { return it.await() }

        val connection = CompletableDeferred<FoundationResult<BillingClient>>()
        pendingConnection = connection
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (pendingConnection == connection) {
                        pendingConnection = null
                    }
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        connection.complete(foundationSuccess(client))
                    } else {
                        connection.complete(billingFailure(billingResult, "Could not connect to Google Play Billing."))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (pendingConnection == connection) {
                        pendingConnection = null
                    }
                    connection.complete(
                        foundationFailure(FoundationError.Platform("Google Play Billing disconnected. Try again."))
                    )
                }
            })
        }.onFailure { throwable ->
            if (pendingConnection == connection) {
                pendingConnection = null
            }
            connection.complete(billingFailure(throwable, "Could not connect to Google Play Billing."))
        }
        return connection.await()
    }

    private fun List<Purchase>.anyActiveProduct(productId: String): Boolean =
        any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.products.any { it == productId }
        }

    private fun lifetimeUnlockedSnapshot(): FoundationResult<FullAccessEntitlementSnapshot> =
        foundationSuccess(
            FullAccessEntitlementSnapshot(
                lifetimeUnlocked = true,
                storeStatus = FullAccessStoreStatus.AVAILABLE,
                message = null
            )
        )

    private fun ProductDetails.toLifetimeStoreOffer(): FoundationResult<FullAccessStoreOffer> {
        val selectedOffer = selectLifetimePurchaseOption()
            ?: return lifetimePurchaseOptionUnavailable()
        return foundationSuccess(
            FullAccessStoreOffer(
                title = "Lifetime",
                priceLabel = selectedOffer.priceLabel ?: LIFETIME_FALLBACK_PRICE_LABEL,
                termsLabel = LIFETIME_TERMS_LABEL
            )
        )
    }

    private fun fallbackLifetimeOffer(): FullAccessStoreOffer =
        FullAccessStoreOffer(
            title = "Lifetime",
            priceLabel = LIFETIME_FALLBACK_PRICE_LABEL,
            termsLabel = LIFETIME_TERMS_LABEL
        )

    private fun ProductDetails.selectLifetimePurchaseOption(): AndroidLifetimePurchaseOption? =
        AndroidLifetimePurchaseOptionSelector.select(lifetimePurchaseOptions())

    private fun ProductDetails.lifetimePurchaseOptions(): List<AndroidLifetimePurchaseOption> {
        val currentOptions = oneTimePurchaseOfferDetailsList.orEmpty()
        val source = currentOptions.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(oneTimePurchaseOfferDetails)
        return source.map { details ->
            AndroidLifetimePurchaseOption(
                formattedPrice = details.formattedPrice,
                offerToken = details.offerToken,
                purchaseOptionId = details.purchaseOptionId,
                offerId = details.offerId,
                hasRentalDetails = details.rentalDetails != null,
                hasPreorderDetails = details.preorderDetails != null
            )
        }
    }

    private fun ProductDetailsQuery.lifetimeProductDetails(): FoundationResult<ProductDetails> {
        productDetails.firstOrNull { it.productId == LIFETIME_UNLOCK_PRODUCT_ID }?.let { product ->
            return foundationSuccess(product)
        }

        val unfetched = unfetchedProducts.firstOrNull { unfetchedProduct ->
            unfetchedProduct.productId == LIFETIME_UNLOCK_PRODUCT_ID
        }
        val message = when (unfetched?.statusCode) {
            UnfetchedProduct.StatusCode.NO_ELIGIBLE_OFFER ->
                "Google Play has no eligible purchase option for $LIFETIME_UNLOCK_PRODUCT_ID. Confirm the buy purchase option is active and available to this tester and region."
            UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND ->
                "Google Play product was not found: $LIFETIME_UNLOCK_PRODUCT_ID. Confirm the one-time product is active for com.jjswigut.oopsallprs.android."
            UnfetchedProduct.StatusCode.INVALID_PRODUCT_ID_FORMAT ->
                "Google Play rejected the product ID format: $LIFETIME_UNLOCK_PRODUCT_ID."
            null ->
                "Google Play product was not returned: $LIFETIME_UNLOCK_PRODUCT_ID. Confirm the one-time product and buy purchase option are active."
            else ->
                "Google Play could not load $LIFETIME_UNLOCK_PRODUCT_ID. Product status code: ${unfetched.statusCode}."
        }
        return foundationFailure(FoundationError.Platform(message))
    }

    private fun <T> lifetimePurchaseOptionUnavailable(): FoundationResult<T> =
        foundationFailure(
            FoundationError.Platform(
                "Google Play returned $LIFETIME_UNLOCK_PRODUCT_ID without one eligible base buy purchase option and offer token. Confirm purchase option ID ${AndroidLifetimePurchaseOptionSelector.EXPECTED_BUY_PURCHASE_OPTION_ID} is active and available."
            )
        )

    private fun lifetimeProductQuery(): QueryProductDetailsParams.Product =
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId(LIFETIME_UNLOCK_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

    private fun <T> unavailable(): FoundationResult<T> =
        foundationFailure(FoundationError.Platform("Google Play Billing is unavailable on this device."))

    private fun <T> billingFailure(result: BillingResult, fallback: String): FoundationResult<T> {
        val debug = result.debugMessage.takeIf { it.isNotBlank() }
        val message = if (debug == null) fallback else "$fallback ${debug}"
        return foundationFailure(FoundationError.Platform(message))
    }

    private fun <T> billingFailure(throwable: Throwable, fallback: String): FoundationResult<T> {
        val detail = throwable.message?.takeIf { it.isNotBlank() }
        val message = if (detail == null) fallback else "$fallback $detail"
        return foundationFailure(FoundationError.Platform(message))
    }

    private companion object {
        const val LIFETIME_UNLOCK_PRODUCT_ID = FullAccessBillingProductIds.LIFETIME
        const val LIFETIME_FALLBACK_PRICE_LABEL = "${'$'}14.99"
        const val LIFETIME_TERMS_LABEL = "One-time Google Play purchase."
    }

    private data class ProductDetailsQuery(
        val productDetails: List<ProductDetails>,
        val unfetchedProducts: List<UnfetchedProduct>
    )
}

actual class HapticFeedback actual constructor(context: Any?) {
    actual fun setLogged() = Unit
    actual fun warning() = Unit
}

actual class PlatformClock actual constructor() {
    actual fun now(): Instant = Clock.System.now()
}

private const val REST_REQUEST_CODE = 9217
private const val REST_NOTIFICATION_ID = 9218
private const val ACTIVE_REST_NOTIFICATION_ID = 9219
private const val REST_OPEN_APP_REQUEST_CODE = 9220
private const val CHANNEL_SOUND = "rest_timer_sound"
private const val CHANNEL_SILENT = "rest_timer_silent"
private const val CHANNEL_ACTIVE = "active_rest_timer"
