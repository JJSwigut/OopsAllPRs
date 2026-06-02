package com.jjswigut.oopsallprs.data.exercise

import oopsallprs.shared.generated.resources.Res

internal suspend fun defaultExerciseSeedCsv(): String =
    Res.readBytes("files/exercises.csv").decodeToString()
