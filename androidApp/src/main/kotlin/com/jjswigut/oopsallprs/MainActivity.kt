package com.jjswigut.oopsallprs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jjswigut.oopsallprs.android.BuildConfig
import com.jjswigut.oopsallprs.platform.FileExportHandoff
import com.jjswigut.oopsallprs.platform.BackupDocumentHandoff
import com.jjswigut.oopsallprs.platform.FullAccessBillingHandoff
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestNotificationScheduler
import com.jjswigut.oopsallprs.platform.ACTION_OPEN_ACTIVE_WORKOUT
import com.jjswigut.oopsallprs.ui.navigation.ActiveWorkoutOpenRequest

class MainActivity : ComponentActivity() {
    private val activeWorkoutOpenRequest = ActiveWorkoutOpenRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleActiveWorkoutIntent(intent)
        val databaseDriverFactory = PlatformDatabaseDriverFactory(this)
        val fileExportHandoff = FileExportHandoff(this)
        val backupDocumentHandoff = BackupDocumentHandoff(this)
        val fullAccessBillingHandoff = FullAccessBillingHandoff(this)
        val restNotificationScheduler = RestNotificationScheduler(this)

        setContent {
            App(
                databaseDriverFactory = databaseDriverFactory,
                fileExportHandoff = fileExportHandoff,
                backupDocumentHandoff = backupDocumentHandoff,
                fullAccessBilling = fullAccessBillingHandoff,
                restAlertScheduler = restNotificationScheduler,
                activeWorkoutOpenRequest = activeWorkoutOpenRequest,
                developerToolsEnabled = BuildConfig.DEBUG
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleActiveWorkoutIntent(intent)
    }

    private fun handleActiveWorkoutIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_ACTIVE_WORKOUT) {
            activeWorkoutOpenRequest.request()
        }
    }
}
