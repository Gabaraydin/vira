package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    // At most one row should ever satisfy this; the repository enforces that invariant.
    @Query("SELECT * FROM workout WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getUnfinished(): WorkoutEntity?

    @Query("SELECT * FROM workout ORDER BY date DESC, startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    // Feeds the cycle engine: non-ad-hoc, finished workouts only, in chronological order.
    @Query(
        "SELECT * FROM workout WHERE programDayId IS NOT NULL AND finishedAt IS NOT NULL " +
            "ORDER BY date, startedAt",
    )
    suspend fun getCompletedForCycle(): List<WorkoutEntity>
}
