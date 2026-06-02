package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository

class SqlWorkoutRepository(
    private val store: SqlFoundationStore
) : WorkoutRepository by store, SessionRepository by store, ActiveWorkoutUxRepository by store
