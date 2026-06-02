package com.jjswigut.oopsallprs.data.db

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.datetime.Instant

fun FoundationId.toSql(): String = value

fun String.toFoundationId(): FoundationId = FoundationId(this)

fun Instant.toSqlMillis(): Long = toEpochMilliseconds()

fun Long.toInstantFromSql(): Instant = Instant.fromEpochMilliseconds(this)

fun WeightKg?.toSqlDouble(): Double? = this?.value

fun Double?.toWeightKg(): WeightKg? = this?.let(::WeightKg)
