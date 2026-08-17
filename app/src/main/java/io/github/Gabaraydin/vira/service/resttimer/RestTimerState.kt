package io.github.Gabaraydin.vira.service.resttimer

data class RestTimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val exerciseName: String,
)
