package com.jjswigut.oopsallprs.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jjswigut.oopsallprs.ds.component.FitRoller

const val REST_DURATION_STEP_SECONDS = 5
private const val REST_DURATION_MIN_SECONDS = REST_DURATION_STEP_SECONDS
private const val REST_DURATION_MAX_SECONDS = 600

@Composable
fun RestDurationRoller(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedSeconds = normalizeRestDurationSeconds(seconds)
    FitRoller(
        value = normalizedSeconds.toFloat(),
        onValueChange = { onSecondsChange(it.toInt()) },
        min = REST_DURATION_MIN_SECONDS.toFloat(),
        max = maxOf(REST_DURATION_MAX_SECONDS, normalizedSeconds).toFloat(),
        step = REST_DURATION_STEP_SECONDS.toFloat(),
        modifier = modifier,
        unit = "sec",
        visibleCount = 3,
        format = { formatRestDurationSeconds(it.toInt()) }
    )
}

fun formatRestDurationSeconds(seconds: Int): String {
    val totalSeconds = seconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val remainder = totalSeconds % 60
    return if (minutes == 0) {
        "${remainder}s"
    } else if (remainder == 0) {
        "${minutes}m"
    } else {
        "$minutes:${remainder.toString().padStart(2, '0')}"
    }
}

private fun normalizeRestDurationSeconds(seconds: Int): Int {
    val rounded = ((seconds + REST_DURATION_STEP_SECONDS / 2) / REST_DURATION_STEP_SECONDS) * REST_DURATION_STEP_SECONDS
    return rounded.coerceAtLeast(REST_DURATION_MIN_SECONDS)
}
