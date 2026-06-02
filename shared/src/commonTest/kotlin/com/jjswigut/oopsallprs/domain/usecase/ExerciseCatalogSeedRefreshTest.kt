package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExerciseCatalogSeedRefreshTest {
    @Test
    fun ensureSeededAddsMissingPackagedExercisesToExistingCatalog() = runTest {
        val harness = FoundationHarness()
        harness.exerciseCatalog.ensureSeeded(seedCsv("Bench Press")).successValue()
        val existingBench = harness.exerciseCatalog.search("Bench Press").single()

        harness.exerciseCatalog.ensureSeeded(seedCsv("Bench Press", "Dead Hang")).successValue()

        val updatedBench = harness.exerciseCatalog.search("Bench Press").single()
        val deadHang = harness.exerciseCatalog.search("Dead Hang").singleOrNull()
        val canonicalNames = harness.exerciseCatalog.search("").map { it.canonicalName }
        assertEquals(existingBench.id, updatedBench.id)
        assertNotNull(deadHang)
        assertEquals(canonicalNames.distinct().size, canonicalNames.size)
    }

    @Test
    fun ensureSeededPreservesUserCreatedExercisesDuringRefresh() = runTest {
        val harness = FoundationHarness()
        harness.exerciseCatalog.ensureSeeded(seedCsv("Bench Press")).successValue()
        val custom = harness.exerciseCatalog.createCustomExercise("My Carry", isBodyweight = false).successValue()

        harness.exerciseCatalog.ensureSeeded(seedCsv("Bench Press", "Dead Hang")).successValue()

        assertEquals(custom.id, harness.exerciseCatalog.userCreatedExercises().single { it.displayName == "My Carry" }.id)
    }

    @Test
    fun ensureSeededDoesNotOverwriteUserCreatedCanonicalConflict() = runTest {
        val harness = FoundationHarness()
        harness.exerciseCatalog.ensureSeeded(seedCsv("Bench Press")).successValue()
        val customDeadHang = harness.exerciseCatalog.createCustomExercise("Dead Hang", isBodyweight = true).successValue()

        harness.exerciseCatalog.ensureSeeded(seedCsv("Bench Press", "Dead Hang", "Pull-Up")).successValue()

        val deadHang = harness.exerciseCatalog.search("Dead Hang").single()
        assertEquals(customDeadHang.id, deadHang.id)
        assertTrue(deadHang.isUserCreated)
    }

    private fun seedCsv(vararg names: String): String {
        val rows = names.joinToString("\n") { name ->
            when (name) {
                "Bench Press" -> "Bench Press,\"Chest,Triceps\",Barbell,Push,Strength,Beginner,Upper"
                "Dead Hang" -> "Dead Hang,\"Back,Forearms\",Bodyweight,Static Hold,Strength,Beginner,Upper"
                "Pull-Up" -> "Pull-Up,\"Back,Arms\",Bodyweight,Pull,Strength,Beginner,Upper"
                else -> error("Unhandled seed exercise: $name")
            }
        }
        return "Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region\n$rows"
    }
}
