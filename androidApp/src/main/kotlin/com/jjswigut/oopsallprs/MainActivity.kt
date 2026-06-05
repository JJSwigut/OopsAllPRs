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
        setContent {
            App(
                databaseDriverFactory = PlatformDatabaseDriverFactory(this),
                fileExportHandoff = FileExportHandoff(this),
                backupDocumentHandoff = BackupDocumentHandoff(this),
                restNotificationScheduler = RestNotificationScheduler(this),
                developerToolsEnabled = BuildConfig.DEBUG
            )
        }
    }
}
