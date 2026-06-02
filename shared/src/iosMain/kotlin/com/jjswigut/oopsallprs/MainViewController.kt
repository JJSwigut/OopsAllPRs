package com.jjswigut.oopsallprs

import androidx.compose.ui.window.ComposeUIViewController
import com.jjswigut.oopsallprs.platform.FileExportHandoff
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestNotificationScheduler
import platform.UIKit.UIViewController

class IosAppViewControllerFactory {
    fun create(developerToolsEnabled: Boolean = false): UIViewController = ComposeUIViewController {
        App(
            databaseDriverFactory = PlatformDatabaseDriverFactory(),
            fileExportHandoff = FileExportHandoff(),
            restNotificationScheduler = RestNotificationScheduler(),
            developerToolsEnabled = developerToolsEnabled,
        )
    }
}
