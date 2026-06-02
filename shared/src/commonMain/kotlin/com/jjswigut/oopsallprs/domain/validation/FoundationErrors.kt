package com.jjswigut.oopsallprs.domain.validation

sealed interface FoundationError {
    val message: String

    data class Validation(override val message: String) : FoundationError
    data class NotFound(override val message: String) : FoundationError
    data class Persistence(override val message: String) : FoundationError
    data class Conflict(override val message: String) : FoundationError
    data class Platform(override val message: String) : FoundationError
}
