package io.github.Gabaraydin.vira.domain.model

import java.time.LocalDate

data class Workout(
    val id: Long,
    val programDayId: Long?,
    val dayNameSnapshot: String,
    val programNameSnapshot: String?,
    val date: LocalDate,
    val startedAt: Long,
    val finishedAt: Long?,
    val note: String?,
)
