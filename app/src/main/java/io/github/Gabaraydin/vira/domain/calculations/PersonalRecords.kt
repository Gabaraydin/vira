package io.github.Gabaraydin.vira.domain.calculations

data class SetForPr(val weightKg: Double, val reps: Int, val isWarmup: Boolean, val isCompleted: Boolean)

// True when the best estimated 1RM among newSets strictly beats the best among priorSets.
// No prior history is not treated as a PR — there's nothing to beat yet. Warm-up and
// incomplete sets are excluded on both sides, and a 0kg/0-rep set (never filled in) is
// skipped rather than crashing estimatedOneRepMax's positive-value requirement.
fun isNewPersonalRecord(newSets: List<SetForPr>, priorSets: List<SetForPr>): Boolean {
    val newBest = bestEstimatedOneRepMax(newSets) ?: return false
    val priorBest = bestEstimatedOneRepMax(priorSets) ?: return false
    return newBest > priorBest
}

private fun bestEstimatedOneRepMax(sets: List<SetForPr>): Double? =
    sets
        .filter { it.isCompleted && !it.isWarmup && it.weightKg > 0 && it.reps >= 1 }
        .maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps).kg }
