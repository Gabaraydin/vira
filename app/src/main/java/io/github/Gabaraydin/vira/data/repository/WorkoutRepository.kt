package io.github.Gabaraydin.vira.data.repository

import androidx.room.withTransaction
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.dao.WorkoutDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutSetDao
import io.github.Gabaraydin.vira.data.local.entity.WorkoutEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity
import io.github.Gabaraydin.vira.domain.model.Workout
import io.github.Gabaraydin.vira.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class WorkoutRepository @Inject constructor(
    private val database: ViraDatabase,
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
) {
    fun observeAll(): Flow<List<Workout>> = workoutDao.observeAll().map { it.map(WorkoutEntity::toDomain) }

    suspend fun getUnfinished(): Workout? = workoutDao.getUnfinished()?.toDomain()

    suspend fun getCompletedForCycle(): List<Workout> = workoutDao.getCompletedForCycle().map { it.toDomain() }

    fun observeSetsForWorkout(workoutId: Long): Flow<List<WorkoutSet>> =
        workoutSetDao.observeForWorkout(workoutId).map { it.map(WorkoutSetEntity::toDomain) }

    // Only one workout may have finishedAt == null at a time; the transaction makes the
    // check-then-insert atomic so two concurrent starts can't both slip through.
    suspend fun startSession(
        programDayId: Long?,
        dayNameSnapshot: String,
        programNameSnapshot: String?,
        date: LocalDate,
        startedAt: Long,
    ): Long = database.withTransaction {
        check(workoutDao.getUnfinished() == null) { "A workout is already in progress; finish or discard it first" }
        workoutDao.insert(
            WorkoutEntity(
                programDayId = programDayId,
                dayNameSnapshot = dayNameSnapshot,
                programNameSnapshot = programNameSnapshot,
                date = date,
                startedAt = startedAt,
                finishedAt = null,
                note = null,
            ),
        )
    }

    suspend fun finishSession(workoutId: Long, finishedAt: Long) {
        val workout = requireNotNull(workoutDao.getById(workoutId)) { "No workout with id $workoutId" }
        workoutDao.update(workout.copy(finishedAt = finishedAt))
    }

    suspend fun discardSession(workout: Workout) {
        workoutDao.delete(workout.toEntity())
    }

    suspend fun addSet(set: WorkoutSet): Long = workoutSetDao.insert(set.toEntity())

    suspend fun updateSet(set: WorkoutSet) {
        workoutSetDao.update(set.toEntity())
    }

    suspend fun deleteSet(set: WorkoutSet) {
        workoutSetDao.delete(set.toEntity())
    }
}

private fun WorkoutEntity.toDomain(): Workout = Workout(
    id = id,
    programDayId = programDayId,
    dayNameSnapshot = dayNameSnapshot,
    programNameSnapshot = programNameSnapshot,
    date = date,
    startedAt = startedAt,
    finishedAt = finishedAt,
    note = note,
)

private fun Workout.toEntity(): WorkoutEntity = WorkoutEntity(
    id = id,
    programDayId = programDayId,
    dayNameSnapshot = dayNameSnapshot,
    programNameSnapshot = programNameSnapshot,
    date = date,
    startedAt = startedAt,
    finishedAt = finishedAt,
    note = note,
)

private fun WorkoutSetEntity.toDomain(): WorkoutSet = WorkoutSet(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isWarmup = isWarmup,
    isCompleted = isCompleted,
    completedAt = completedAt,
    supersetGroupId = supersetGroupId,
)

private fun WorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isWarmup = isWarmup,
    isCompleted = isCompleted,
    completedAt = completedAt,
    supersetGroupId = supersetGroupId,
)
