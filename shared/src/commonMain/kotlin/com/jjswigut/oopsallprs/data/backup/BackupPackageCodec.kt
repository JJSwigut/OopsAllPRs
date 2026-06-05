package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class BackupPackageCodec(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {
    fun encode(pkg: BackupPackage): FoundationResult<String> =
        try {
            foundationSuccess(json.encodeToString(BackupPackageDto.serializer(), pkg))
        } catch (error: SerializationException) {
            foundationFailure(FoundationError.Persistence("Backup encoding failed: ${error.message}"))
        }

    fun decode(content: String): FoundationResult<BackupPackage> =
        try {
            val decoded = json.decodeFromString(BackupPackageDto.serializer(), content)
            validate(decoded)
        } catch (error: SerializationException) {
            foundationFailure(FoundationError.Validation("Backup file is not a valid Oops All PRs backup"))
        } catch (error: IllegalArgumentException) {
            foundationFailure(FoundationError.Validation(error.message ?: "Backup file is invalid"))
        }

    fun validate(pkg: BackupPackage): FoundationResult<BackupPackage> =
        when {
            pkg.formatVersion > BACKUP_FORMAT_VERSION ->
                foundationFailure(FoundationError.Validation("Backup format is newer than this app supports"))
            pkg.formatVersion <= 0 ->
                foundationFailure(FoundationError.Validation("Backup format version is invalid"))
            pkg.deviceId.isBlank() ->
                foundationFailure(FoundationError.Validation("Backup is missing device metadata"))
            pkg.lastLocalRevision.isBlank() ->
                foundationFailure(FoundationError.Validation("Backup is missing revision metadata"))
            else -> foundationSuccess(pkg)
        }
}
