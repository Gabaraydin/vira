package io.github.Gabaraydin.vira.domain.model

import java.time.LocalDate

data class BodyMeasurement(
    val id: Long,
    val date: LocalDate,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double?,
    val neckCm: Double?,
    val hipCm: Double?,
    val bodyFatPct: Double?,
    val note: String?,
)
