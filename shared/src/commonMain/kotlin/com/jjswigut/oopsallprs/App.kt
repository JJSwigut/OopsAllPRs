package com.jjswigut.oopsallprs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.jjswigut.oopsallprs.platform.FileExportHandoff
import com.jjswigut.oopsallprs.platform.BackupDocumentHandoff
import com.jjswigut.oopsallprs.platform.PlatformDatabaseDriverFactory
import com.jjswigut.oopsallprs.platform.RestNotificationScheduler
import com.jjswigut.oopsallprs.ui.designsystem.OopsAllPrsFoundationTheme
import com.jjswigut.oopsallprs.ui.navigation.AppShell

@Composable
fun App(
    databaseDriverFactory: PlatformDatabaseDriverFactory,
    fileExportHandoff: FileExportHandoff? = null,
    backupDocumentHandoff: BackupDocumentHandoff? = null,
    restNotificationScheduler: RestNotificationScheduler? = null,
    developerToolsEnabled: Boolean = false
) {
    val appState = remember(databaseDriverFactory, fileExportHandoff, backupDocumentHandoff, restNotificationScheduler, developerToolsEnabled) {
        AppState.create(databaseDriverFactory, fileExportHandoff, backupDocumentHandoff, restNotificationScheduler, developerToolsEnabled)
    }
    val shellState by appState.navigation.state.collectAsState()

    OopsAllPrsFoundationTheme(
        palette = shellState.paletteMode.resolve(),
        reduceMotion = shellState.reduceMotion,
        hapticsEnabled = shellState.hapticsEnabled
    ) {
        AppShell(appState)
    }
}
