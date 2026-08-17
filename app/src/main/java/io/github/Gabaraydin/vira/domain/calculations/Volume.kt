package io.github.Gabaraydin.vira.domain.calculations

data class SetForVolume(
    val weightKg: Double,
    val reps: Int,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
)

// Sums weightKg * reps over completed, non-warm-up sets only; pass a single exercise's
// sets for a per-exercise total or a whole session's sets for the session total.
fun totalVolume(sets: List<SetForVolume>): Double {
    sets.forEach {
        require(it.weightKg >= 0) { "weightKg must not be negative, was ${it.weightKg}" }
        require(it.reps >= 0) { "reps must not be negative, was ${it.reps}" }
    }

    return sets
        .filter { it.isCompleted && !it.isWarmup }
        .sumOf { it.weightKg * it.reps }
}
