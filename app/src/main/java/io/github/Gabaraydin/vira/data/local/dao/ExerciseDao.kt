package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE isArchived = 0 ORDER BY nameEn")
    fun observeActive(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY nameEn")
    fun observeAll(): Flow<List<ExerciseEntity>>

    // Full-table read for backup export — order doesn't matter, id order is fine.
    @Query("SELECT * FROM exercise ORDER BY id")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int
}
