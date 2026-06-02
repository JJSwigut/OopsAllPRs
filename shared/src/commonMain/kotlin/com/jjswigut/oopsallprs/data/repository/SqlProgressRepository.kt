package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.repository.ProgressRepository

class SqlProgressRepository(
    private val store: SqlFoundationStore
) : ProgressRepository by store
