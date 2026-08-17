package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Insert
    suspend fun insert(program: ProgramEntity): Long

    @Update
    suspend fun update(program: ProgramEntity)

    @Query("SELECT * FROM program WHERE id = :id")
    suspend fun getById(id: Long): ProgramEntity?

    @Query("SELECT * FROM program WHERE isActive = 1 LIMIT 1")
    fun observeActiveProgram(): Flow<ProgramEntity?>

    @Query("SELECT * FROM program WHERE archivedAt IS NULL ORDER BY createdAt")
    fun observeUnarchived(): Flow<List<ProgramEntity>>

    // Used together, in a transaction, to enforce "exactly one active program" in the
    // repository rather than with a database constraint.
    @Query("UPDATE program SET isActive = 0 WHERE id != :programId")
    suspend fun deactivateAllExcept(programId: Long)

    @Query("UPDATE program SET isActive = 1 WHERE id = :programId")
    suspend fun activate(programId: Long)
}
