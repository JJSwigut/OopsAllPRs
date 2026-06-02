package com.jjswigut.oopsallprs.dev

enum class DeveloperSeedScenario(
    val label: String,
    val description: String
) {
    PROGRESS(
        label = "Progress demo",
        description = "Completed workouts, PRs, history, and charts"
    ),
    ROUTINES(
        label = "Routines demo",
        description = "Reusable routines with planned sets and rests"
    ),
    ACTIVE_RECOVERY(
        label = "Active recovery demo",
        description = "In-progress workout for resume testing"
    )
}

enum class DeveloperSeedOutcome {
    LOADED,
    SKIPPED,
    FAILED
}

data class DeveloperSeedResult(
    val scenario: DeveloperSeedScenario,
    val outcome: DeveloperSeedOutcome,
    val message: String
)

data class DeveloperSeedScenarioRow(
    val scenario: DeveloperSeedScenario,
    val label: String = scenario.label,
    val description: String = scenario.description
)

data class DeveloperSeedState(
    val scenarios: List<DeveloperSeedScenarioRow> = DeveloperSeedScenario.entries.map { DeveloperSeedScenarioRow(it) },
    val loadingScenario: DeveloperSeedScenario? = null,
    val lastResult: DeveloperSeedResult? = null
)
