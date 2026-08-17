package io.github.Gabaraydin.vira.domain.calculations

import kotlin.math.log10
import kotlin.math.pow

private val PLAUSIBLE_BODY_FAT_RANGE = 1.0..70.0

// US Navy method, metric, log base 10.
fun bodyFatPercentMale(waistCm: Double, neckCm: Double, heightCm: Double): Double {
    require(heightCm > 0) { "heightCm must be positive, was $heightCm" }
    val diff = waistCm - neckCm
    require(diff > 0) { "waistCm ($waistCm) must be greater than neckCm ($neckCm)" }

    val bf = 495.0 / (1.0324 - 0.19077 * log10(diff) + 0.15456 * log10(heightCm)) - 450.0
    require(bf in PLAUSIBLE_BODY_FAT_RANGE) {
        "computed body fat $bf% is outside the plausible $PLAUSIBLE_BODY_FAT_RANGE% range"
    }
    return bf
}

fun bodyFatPercentFemale(waistCm: Double, neckCm: Double, hipCm: Double, heightCm: Double): Double {
    require(heightCm > 0) { "heightCm must be positive, was $heightCm" }
    require(hipCm > 0) { "hipCm must be positive, was $hipCm" }
    val sum = waistCm + hipCm - neckCm
    require(sum > 0) { "waistCm + hipCm - neckCm must be positive, was $sum" }

    val bf = 495.0 / (1.29579 - 0.35004 * log10(sum) + 0.22100 * log10(heightCm)) - 450.0
    require(bf in PLAUSIBLE_BODY_FAT_RANGE) {
        "computed body fat $bf% is outside the plausible $PLAUSIBLE_BODY_FAT_RANGE% range"
    }
    return bf
}

fun leanMassKg(weightKg: Double, bodyFatPercent: Double): Double {
    require(weightKg > 0) { "weightKg must be positive, was $weightKg" }
    require(bodyFatPercent in PLAUSIBLE_BODY_FAT_RANGE) {
        "bodyFatPercent $bodyFatPercent% is outside the plausible $PLAUSIBLE_BODY_FAT_RANGE% range"
    }
    return weightKg * (1 - bodyFatPercent / 100.0)
}

fun fatMassKg(weightKg: Double, leanMassKg: Double): Double {
    require(weightKg > 0) { "weightKg must be positive, was $weightKg" }
    require(leanMassKg in 0.0..weightKg) { "leanMassKg ($leanMassKg) must be between 0 and weightKg ($weightKg)" }
    return weightKg - leanMassKg
}

fun bodyMassIndex(weightKg: Double, heightCm: Double): Double {
    require(weightKg > 0) { "weightKg must be positive, was $weightKg" }
    require(heightCm > 0) { "heightCm must be positive, was $heightCm" }
    val heightM = heightCm / 100.0
    return weightKg / heightM.pow(2)
}
