package com.jjswigut.oopsallprs.data.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseSeedRow
import com.jjswigut.oopsallprs.domain.model.RejectedSeedRow
import com.jjswigut.oopsallprs.domain.model.SeedImportReport

object ExerciseCsvParser {
    private val expectedHeader = listOf(
        "Exercise Name",
        "Muscle Group",
        "Equipment",
        "Movement Pattern",
        "Exercise Type",
        "Experience Level",
        "Body Region"
    )

    fun parse(csv: String): SeedImportReport {
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) {
            return SeedImportReport(emptyList(), listOf(RejectedSeedRow(1, "", "Missing CSV header")), emptySet())
        }

        val header = parseCsvLine(lines.first())
        if (header != expectedHeader) {
            return SeedImportReport(emptyList(), listOf(RejectedSeedRow(1, lines.first(), "Invalid CSV header")), emptySet())
        }

        val accepted = mutableListOf<ExerciseSeedRow>()
        val rejected = mutableListOf<RejectedSeedRow>()
        val seen = mutableMapOf<String, Int>()
        val duplicates = mutableSetOf<String>()

        lines.drop(1).forEachIndexed { index, raw ->
            val rowNumber = index + 2
            val columns = parseCsvLine(raw)
            if (columns.size != expectedHeader.size) {
                rejected += RejectedSeedRow(rowNumber, raw, "Expected ${expectedHeader.size} columns but found ${columns.size}")
                return@forEachIndexed
            }
            if (columns.any { it.isBlank() }) {
                rejected += RejectedSeedRow(rowNumber, raw, "Required classification field is blank")
                return@forEachIndexed
            }

            val row = ExerciseSeedRow(
                exerciseName = columns[0].trim(),
                muscleGroup = columns[1].trim(),
                equipment = columns[2].trim(),
                movementPattern = columns[3].trim(),
                exerciseType = columns[4].trim(),
                experienceLevel = columns[5].trim(),
                bodyRegion = columns[6].trim(),
                rowNumber = rowNumber
            )

            if (seen.put(row.canonicalName, rowNumber) != null) {
                duplicates += row.canonicalName
            }
            accepted += row
        }

        return SeedImportReport(accepted, rejected, duplicates)
    }

    fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        cells += current.toString()
        return cells
    }
}
