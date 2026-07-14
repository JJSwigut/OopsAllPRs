package com.jjswigut.oopsallprs.data

import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import okio.ByteString.Companion.encodeUtf8

internal object LoggingConfigurationIdentity {
    fun canonicalRepresentation(configuration: LoggingConfiguration): String = with(configuration) {
        val measureContent = measures.mapIndexed { position, measure ->
            buildString {
                append(position)
                append(':')
                append(measure.kind.wireCode.value)
                append(':')
                append(measure.requirement.wireCode.value)
                append(':')
                append(measure.kind.canonicalUnit.wireCode.value)
                measure.loadRole?.let {
                    append(':')
                    append(it.wireCode.value)
                }
            }
        }.joinToString(";")
        val effortContent = observedEffort?.kinds.orEmpty()
            .mapIndexed { position, kind -> "$position:${kind.wireCode.value}" }
            .joinToString(";")
        "schema_version=${schemaVersion.value}|measures=$measureContent|effort=$effortContent"
    }

    fun contentHash(configuration: LoggingConfiguration): String =
        canonicalRepresentation(configuration).encodeUtf8().sha256().hex()

    fun hasSameSemanticContent(
        first: LoggingConfiguration,
        second: LoggingConfiguration
    ): Boolean =
        first.schemaVersion == second.schemaVersion &&
            first.measures == second.measures &&
            first.observedEffort == second.observedEffort
}
