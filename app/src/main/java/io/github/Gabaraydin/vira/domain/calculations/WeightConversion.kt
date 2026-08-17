package io.github.Gabaraydin.vira.domain.calculations

private const val KG_PER_LB = 0.45359237

fun Double.kgToLb(): Double {
    require(this >= 0) { "kg must not be negative, was $this" }
    return this / KG_PER_LB
}

fun Double.lbToKg(): Double {
    require(this >= 0) { "lb must not be negative, was $this" }
    return this * KG_PER_LB
}
