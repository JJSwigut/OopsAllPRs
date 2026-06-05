package com.jjswigut.oopsallprs.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual class PlatformDatabaseDriverFactory actual constructor(private val context: Any?) {
    actual fun createDriver(): SqlDriver {
        val androidContext = context as? Context
            ?: error("Android database driver requires an android.content.Context")
        return AndroidSqliteDriver(WorkoutDatabase.Schema, androidContext, "oops_all_prs.db")
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
    actual override fun schedule(restEndsAt: Instant, soundEnabled: Boolean) {
        val androidContext = context as? Context ?: return
        val intent = restTimerIntent(androidContext).putExtra(RestTimerReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
        val pendingIntent = PendingIntent.getBroadcast(
            androidContext,
            REST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = androidContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restEndsAt.toEpochMilliseconds(), pendingIntent)
    }

    actual override fun cancel() {
        val androidContext = context as? Context ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            androidContext,
            REST_REQUEST_CODE,
            restTimerIntent(androidContext),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = androidContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun restTimerIntent(context: Context): Intent =
        Intent(context, RestTimerReceiver::class.java).setAction(RestTimerReceiver.ACTION_REST_DONE)
}

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REST_DONE) return
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

actual class HapticFeedback actual constructor(context: Any?) {
    actual fun setLogged() = Unit
    actual fun warning() = Unit
}

actual class PlatformClock actual constructor() {
    actual fun now(): Instant = Clock.System.now()
}

private const val REST_REQUEST_CODE = 9217
private const val REST_NOTIFICATION_ID = 9218
private const val CHANNEL_SOUND = "rest_timer_sound"
private const val CHANNEL_SILENT = "rest_timer_silent"
