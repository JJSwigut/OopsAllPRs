package com.jjswigut.oopsallprs

import androidx.compose.ui.window.ComposeUIViewController
import com.jjswigut.oopsallprs.platform.FileExportHandoff
import com.jjswigut.oopsallprs.platform.BackupDocumentHandoff
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestNotificationScheduler
import platform.UIKit.UIViewController

class IosAppViewControllerFactory {
    fun create(context: Any? = null, developerToolsEnabled: Boolean = false): UIViewController = ComposeUIViewController {
        App(
            databaseDriverFactory = PlatformDatabaseDriverFactory(),
            fileExportHandoff = FileExportHandoff(),
            backupDocumentHandoff = BackupDocumentHandoff(context),
            restNotificationScheduler = RestNotificationScheduler(),
            developerToolsEnabled = developerToolsEnabled,
        )
    }
}
