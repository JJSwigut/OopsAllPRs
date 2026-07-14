package com.jjswigut.oopsallprs.data

import com.jjswigut.oopsallprs.data.backup.toDto
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class LoggingConfigurationIdentityTest {
    @Test
    fun canonicalRepresentationAndHashAreStableAcrossSqlAndBackupConsumers() {
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("identity-contract"),
            schemaVersion = LoggingSchemaVersion(2),
            measures = listOf(
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.ASSISTANCE)
            ),
            observedEffort = ObservedEffortSpec(listOf(EffortKind.RIR, EffortKind.FAILURE_OUTCOME))
        )

        assertEquals(
            "schema_version=2|measures=0:distance:required:meters;1:load:optional:kilograms:assistance|" +
                "effort=0:rir;1:failure_outcome",
            LoggingConfigurationIdentity.canonicalRepresentation(configuration)
        )
        assertEquals(
            "eb873a72aa8e5d419f81b26ef2a6036a2b04efb24fb9219c61126afcbdec4fa0",
            LoggingConfigurationIdentity.contentHash(configuration)
        )
        assertEquals(
            LoggingConfigurationIdentity.contentHash(configuration),
            configuration.toDto().contentHash
        )
    }
}
