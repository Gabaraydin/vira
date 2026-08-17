package io.github.Gabaraydin.vira.domain.model

data class WorkoutSet(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val supersetGroupId: Int?,
)
