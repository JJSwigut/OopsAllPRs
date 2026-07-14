package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.data.repository.SqlBackupRepository
import com.jjswigut.oopsallprs.data.repository.SqlFoundationStoreTestHarness
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BackupV1GoldenTest {
    @Test
    fun releasedV1PackageDecodesAndRestoresWithoutReinterpretingBodyweightLoad() = runTest {
        val content = checkNotNull(javaClass.getResourceAsStream("/backup/v1-released-golden.json"))
            .bufferedReader()
            .use { it.readText() }
        val decoded = when (val result = BackupPackageCodec().decode(content)) {
            is FoundationResult.Success -> result.value
            is FoundationResult.Failure -> error(result.error.message)
        }

        assertEquals(BACKUP_FORMAT_VERSION_V1, decoded.formatVersion)
        assertEquals(
            LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.id.value,
            decoded.completedWorkouts.single().exercises.single().loggedSets.single().captureConfigurationId
        )

        val harness = SqlFoundationStoreTestHarness()
        val repositories = harness.repositories()
        val restored = assertIs<FoundationResult.Success<*>>(
            SqlBackupRepository(harness.database, repositories.store).restore(decoded)
        )
        val set = repositories.workouts.completedWorkouts().single().exercises.single().loggedSets.single()

        assertEquals(10.0, set.weight?.value)
        assertEquals(LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.id, set.captureConfigurationId)
        assertEquals(1, restored.value.let { (it as com.jjswigut.oopsallprs.domain.model.BackupRestoreResult).restoredSummary.workoutCount })
    }
}
