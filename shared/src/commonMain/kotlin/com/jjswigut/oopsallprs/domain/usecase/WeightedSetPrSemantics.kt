package com.jjswigut.oopsallprs.domain.usecase

internal data class WeightedSetPerformance(
    val weightKg: Double,
    val reps: Int
) {
    fun blocksNewPersonalRecord(candidate: WeightedSetPerformance): Boolean =
        this == candidate || candidate.isStrictlyDominatedBy(this)

    private fun isStrictlyDominatedBy(other: WeightedSetPerformance): Boolean =
        other.weightKg >= weightKg &&
            other.reps >= reps &&
            (other.weightKg > weightKg || other.reps > reps)
}

internal fun <T> Iterable<T>.nondominatedWeightedSets(
    performanceOf: (T) -> WeightedSetPerformance
): List<T> {
    val uniquePerformances = distinctBy(performanceOf)
    return uniquePerformances.filter { candidate ->
        val candidatePerformance = performanceOf(candidate)
        uniquePerformances.none { other ->
            performanceOf(other) != candidatePerformance &&
                performanceOf(other).blocksNewPersonalRecord(candidatePerformance)
        }
    }
}
