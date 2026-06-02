package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository

class SqlRoutineRepository(
    private val store: SqlFoundationStore
) : RoutineRepository by store, WorkoutRepository by store
