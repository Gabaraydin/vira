package io.github.Gabaraydin.vira.domain.calculations

data class OneRepMaxEstimate(val kg: Double, val isApproximate: Boolean)

// Epley formula. reps > 12 is flagged approximate; the caller decides how to show that.
fun estimatedOneRepMax(weightKg: Double, reps: Int): OneRepMaxEstimate {
    require(weightKg > 0) { "weightKg must be positive, was $weightKg" }
    require(reps >= 1) { "reps must be at least 1, was $reps" }

    val kg = weightKg * (1 + reps / 30.0)
    return OneRepMaxEstimate(kg = kg, isApproximate = reps > 12)
}
