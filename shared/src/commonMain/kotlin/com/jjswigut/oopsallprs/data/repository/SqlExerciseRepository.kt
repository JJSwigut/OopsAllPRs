package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository

class SqlExerciseRepository(
    private val store: SqlFoundationStore
) : ExerciseRepository by store
