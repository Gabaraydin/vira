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

    // Explicit-PK inserts (backup import).
    @Insert
    suspend fun insertAll(workouts: List<WorkoutEntity>)

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    // Full-table read for backup export — every workout regardless of finished state.
    @Query("SELECT * FROM workout ORDER BY id")
    suspend fun getAll(): List<WorkoutEntity>

    // At most one row should ever satisfy this; the repository enforces that invariant.
    @Query("SELECT * FROM workout WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getUnfinished(): WorkoutEntity?

    @Query("SELECT * FROM workout WHERE finishedAt IS NULL LIMIT 1")
    fun observeUnfinished(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workout ORDER BY date DESC, startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    // For the Today screen's streak/gap line: any finished workout, ad-hoc included.
    @Query("SELECT * FROM workout WHERE finishedAt IS NOT NULL ORDER BY date DESC, startedAt DESC LIMIT 1")
    fun observeMostRecentFinished(): Flow<WorkoutEntity?>

    // Feeds the cycle engine: non-ad-hoc, finished workouts only, in chronological order.
    @Query(
        "SELECT * FROM workout WHERE programDayId IS NOT NULL AND finishedAt IS NOT NULL " +
            "ORDER BY date, startedAt",
    )
    suspend fun getCompletedForCycle(): List<WorkoutEntity>

    @Query(
        "SELECT * FROM workout WHERE programDayId IS NOT NULL AND finishedAt IS NOT NULL " +
            "ORDER BY date, startedAt",
    )
    fun observeCompletedForCycle(): Flow<List<WorkoutEntity>>

    // The Workout Summary screen's "same day, previous cycle" comparison: the most recent
    // *other* finished workout for this exact program day.
    @Query(
        "SELECT * FROM workout WHERE programDayId = :programDayId AND id != :excludeWorkoutId " +
            "AND finishedAt IS NOT NULL ORDER BY date DESC, startedAt DESC LIMIT 1",
    )
    suspend fun getPreviousWorkoutForDay(programDayId: Long, excludeWorkoutId: Long): WorkoutEntity?
}
