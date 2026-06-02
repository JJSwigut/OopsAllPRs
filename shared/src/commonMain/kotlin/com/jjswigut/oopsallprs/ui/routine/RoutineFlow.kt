package com.jjswigut.oopsallprs.ui.routine

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ui.accessibility.foundationTouchTarget
import com.jjswigut.oopsallprs.ui.designsystem.FoundationActionButton
import com.jjswigut.oopsallprs.ui.designsystem.FoundationText

@Composable
fun RoutineFlow(
    state: RoutineState,
    onLaunchRoutine: () -> Unit,
    onSaveCompleted: () -> Unit
) {
    Column {
        FoundationText("Routines: ${state.routines.size}")
        FoundationActionButton("Launch", onLaunchRoutine, Modifier.foundationTouchTarget("Launch routine"))
        FoundationActionButton("Save", onSaveCompleted, Modifier.foundationTouchTarget("Save workout as routine"), primary = false)
    }
}
