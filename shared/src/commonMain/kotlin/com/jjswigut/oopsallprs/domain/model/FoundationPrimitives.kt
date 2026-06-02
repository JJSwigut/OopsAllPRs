package com.jjswigut.oopsallprs.domain.model

import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline
import kotlin.random.Random

@JvmInline
value class FoundationId(val value: String) {
    init {
        require(value.isNotBlank()) { "FoundationId cannot be blank" }
    }

    override fun toString(): String = value
}

@JvmInline
value class OrderedPosition(val value: Int) {
    init {
        require(value >= 0) { "Position must be zero or greater" }
    }
}

data class AuditMetadata(
    val createdAt: Instant,
    val updatedAt: Instant = createdAt
)

sealed interface FoundationResult<out T> {
    data class Success<T>(val value: T) : FoundationResult<T>
    data class Failure(val error: FoundationError) : FoundationResult<Nothing>
}

fun <T> foundationSuccess(value: T): FoundationResult<T> = FoundationResult.Success(value)

fun foundationFailure(error: FoundationError): FoundationResult<Nothing> = FoundationResult.Failure(error)

fun newFoundationId(prefix: String = "id"): FoundationId {
    val random = Random.nextLong().toString(36).replace("-", "")
    val time = Clock.System.now().toEpochMilliseconds().toString(36)
    return FoundationId("$prefix-$time-$random")
}

fun List<OrderedPosition>.isContiguous(): Boolean =
    map { it.value }.sorted() == indices.toList()
