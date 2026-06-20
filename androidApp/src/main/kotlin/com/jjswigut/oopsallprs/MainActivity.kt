package com.jjswigut.oopsallprs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jjswigut.oopsallprs.android.BuildConfig
import com.jjswigut.oopsallprs.platform.FileExportHandoff
import com.jjswigut.oopsallprs.platform.BackupDocumentHandoff
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestNotificationScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val databaseDriverFactory = PlatformDatabaseDriverFactory(this)
        val fileExportHandoff = FileExportHandoff(this)
        val backupDocumentHandoff = BackupDocumentHandoff(this)
        val restNotificationScheduler = RestNotificationScheduler(this)

        setContent {
            App(
                databaseDriverFactory = databaseDriverFactory,
                fileExportHandoff = fileExportHandoff,
                backupDocumentHandoff = backupDocumentHandoff,
                restNotificationScheduler = restNotificationScheduler,
                developerToolsEnabled = BuildConfig.DEBUG
            )
        }
    }
}
