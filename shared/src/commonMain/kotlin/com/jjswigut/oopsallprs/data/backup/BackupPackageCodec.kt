package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

class BackupPackageCodec(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {
    fun encode(pkg: BackupPackage): FoundationResult<String> =
        try {
            require(pkg.formatVersion == BACKUP_FORMAT_VERSION) {
                "Only Backup V2 packages can be encoded"
            }
            foundationSuccess(json.encodeToString(BackupPackageDto.serializer(), pkg))
        } catch (error: SerializationException) {
            foundationFailure(FoundationError.Persistence("Backup encoding failed: ${error.message}"))
        } catch (error: IllegalArgumentException) {
            foundationFailure(FoundationError.Validation(error.message ?: "Backup package is invalid"))
        }

    fun decode(content: String): FoundationResult<BackupPackage> =
        try {
            val raw = json.parseToJsonElement(content).jsonObject
            val formatVersion = raw["formatVersion"]?.jsonPrimitive?.intOrNull
                ?: return foundationFailure(FoundationError.Validation("Backup format version is missing or invalid"))
            val decoded = when (formatVersion) {
                BACKUP_FORMAT_VERSION_V1 ->
                    json.decodeFromJsonElement(BackupPackageV1Dto.serializer(), raw).normalizeToV2()
                BACKUP_FORMAT_VERSION ->
                    json.decodeFromJsonElement(BackupPackageDto.serializer(), raw)
                else -> return foundationFailure(
                    FoundationError.Validation(
                        if (formatVersion > BACKUP_FORMAT_VERSION) {
                            "Backup format is newer than this app supports"
                        } else {
                            "Backup format version is invalid"
                        }
                    )
                )
            }
            validate(decoded)
        } catch (error: SerializationException) {
            foundationFailure(FoundationError.Validation("Backup file is not a valid Oops All PRs backup"))
        } catch (error: IllegalArgumentException) {
            foundationFailure(FoundationError.Validation(error.message ?: "Backup file is invalid"))
        }

    fun validate(pkg: BackupPackage): FoundationResult<BackupPackage> =
        try {
            BackupPackageValidator.validate(pkg)
            foundationSuccess(pkg)
        } catch (error: IllegalArgumentException) {
            foundationFailure(FoundationError.Validation(error.message ?: "Backup file is invalid"))
        }
}
