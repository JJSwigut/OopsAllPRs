package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.repository.SetLedgerRepository

class SqlSetLedgerRepository(
    private val store: SqlFoundationStore
) : SetLedgerRepository by store
