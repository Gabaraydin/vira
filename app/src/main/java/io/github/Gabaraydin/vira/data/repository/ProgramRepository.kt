package io.github.Gabaraydin.vira.data.repository

import androidx.room.withTransaction
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.dao.ProgramDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayExerciseDao
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramEntity
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.Program
import io.github.Gabaraydin.vira.domain.model.ProgramDay
import io.github.Gabaraydin.vira.domain.model.ProgramDayExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgramRepository @Inject constructor(
    private val database: ViraDatabase,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programDayExerciseDao: ProgramDayExerciseDao,
) {
    fun observeActiveProgram(): Flow<Program?> = programDao.observeActiveProgram().map { it?.toDomain() }

    fun observeUnarchivedPrograms(): Flow<List<Program>> =
        programDao.observeUnarchived().map { it.map(ProgramEntity::toDomain) }

    fun observeProgram(programId: Long): Flow<Program?> =
        programDao.observeUnarchived().map { list -> list.firstOrNull { it.id == programId }?.toDomain() }

    fun observeDaysForProgram(programId: Long): Flow<List<ProgramDay>> =
        programDayDao.observeForProgram(programId).map { it.map(ProgramDayEntity::toDomain) }

    fun observeExercisesForDay(programDayId: Long): Flow<List<ProgramDayExercise>> =
        programDayExerciseDao.observeForDay(programDayId).map { it.map(ProgramDayExerciseEntity::toDomain) }

    suspend fun createProgram(name: String, createdAt: Long): Long =
        programDao.insert(ProgramEntity(name = name, isActive = false, createdAt = createdAt, archivedAt = null))

    // Exactly one program may be active; both updates run in one transaction so no
    // intermediate state with zero or two active programs is ever observable.
    suspend fun setActiveProgram(programId: Long) = database.withTransaction {
        programDao.deactivateAllExcept(programId)
        programDao.activate(programId)
    }

    suspend fun archiveProgram(programId: Long, archivedAt: Long) {
        val program = requireNotNull(programDao.getById(programId)) { "No program with id $programId" }
        programDao.update(program.copy(archivedAt = archivedAt, isActive = false))
    }

    // Full copy: the program row, every day (same positions), and every day's planned
    // exercises. The copy is never active — the user opts into that separately.
    suspend fun duplicateProgram(programId: Long, newName: String, createdAt: Long): Long = database.withTransaction {
        val sourceDays = programDayDao.getForProgram(programId).sortedBy { it.position }
        val newProgramId = programDao.insert(
            ProgramEntity(name = newName, isActive = false, createdAt = createdAt, archivedAt = null),
        )
        sourceDays.forEach { sourceDay ->
            val newDayId = programDayDao.insert(
                ProgramDayEntity(
                    programId = newProgramId,
                    position = sourceDay.position,
                    name = sourceDay.name,
                    isRest = sourceDay.isRest,
                    libraryCategory = sourceDay.libraryCategory,
                ),
            )
            programDayExerciseDao.getForDay(sourceDay.id).forEach { sourceEntry ->
                programDayExerciseDao.insert(
                    ProgramDayExerciseEntity(
                        programDayId = newDayId,
                        exerciseId = sourceEntry.exerciseId,
                        position = sourceEntry.position,
                        supersetGroupId = sourceEntry.supersetGroupId,
                        supersetOrder = sourceEntry.supersetOrder,
                        targetSets = sourceEntry.targetSets,
                        targetRepsMin = sourceEntry.targetRepsMin,
                        targetRepsMax = sourceEntry.targetRepsMax,
                        targetWeightKg = sourceEntry.targetWeightKg,
                        restSecOverride = sourceEntry.restSecOverride,
                    ),
                )
            }
        }
        newProgramId
    }

    // Appends at the end, so position stays 0-based and contiguous by construction.
    suspend fun addDay(programId: Long, name: String, isRest: Boolean, libraryCategory: MuscleGroup?): Long {
        val position = programDayDao.getForProgram(programId).size
        return programDayDao.insert(
            ProgramDayEntity(
                programId = programId,
                position = position,
                name = name,
                isRest = isRest,
                libraryCategory = libraryCategory,
            ),
        )
    }

    // Renaming, toggling rest, or changing library category — anything that isn't a
    // position/contiguity change goes through here rather than a dedicated method.
    suspend fun updateDay(day: ProgramDay) {
        programDayDao.update(day.toEntity())
    }

    // Renumbers every day to its index in orderedDayIds, keeping positions contiguous.
    // Two passes: an arbitrary permutation applied directly can momentarily assign a day
    // the position another not-yet-updated day still holds, tripping the (programId,
    // position) unique index. Moving everything out of range first avoids that collision.
    suspend fun reorderDays(programId: Long, orderedDayIds: List<Long>) = database.withTransaction {
        val days = programDayDao.getForProgram(programId)
        require(orderedDayIds.toSet() == days.map { it.id }.toSet()) {
            "orderedDayIds must contain exactly the program's current days"
        }
        val byId = days.associateBy { it.id }
        val outOfRangeOffset = days.size

        orderedDayIds.forEach { dayId ->
            val day = byId.getValue(dayId)
            programDayDao.update(day.copy(position = day.position + outOfRangeOffset))
        }
        orderedDayIds.forEachIndexed { index, dayId ->
            programDayDao.update(byId.getValue(dayId).copy(position = index))
        }
    }

    // Deletes then re-compacts remaining positions, so a gap never persists.
    suspend fun removeDay(day: ProgramDay) = database.withTransaction {
        programDayDao.delete(day.toEntity())
        val remaining = programDayDao.getForProgram(day.programId).sortedBy { it.position }
        remaining.forEachIndexed { index, remainingDay ->
            if (remainingDay.position != index) {
                programDayDao.update(remainingDay.copy(position = index))
            }
        }
    }

    // Appends at the end of the day, same contiguity approach as addDay.
    suspend fun addExerciseToDay(
        programDayId: Long,
        exerciseId: Long,
        targetSets: Int,
        targetRepsMin: Int?,
        targetRepsMax: Int?,
        targetWeightKg: Double?,
        restSecOverride: Int?,
    ): Long {
        val position = programDayExerciseDao.getForDay(programDayId).size
        return programDayExerciseDao.insert(
            ProgramDayExerciseEntity(
                programDayId = programDayId,
                exerciseId = exerciseId,
                position = position,
                supersetGroupId = null,
                supersetOrder = null,
                targetSets = targetSets,
                targetRepsMin = targetRepsMin,
                targetRepsMax = targetRepsMax,
                targetWeightKg = targetWeightKg,
                restSecOverride = restSecOverride,
            ),
        )
    }

    // A superset group holds 2-4 exercises that must already be contiguous by position;
    // reordering to make room is a UI concern (drag-to-reorder), not this method's job.
    suspend fun groupIntoSuperset(programDayId: Long, entryIds: List<Long>) = database.withTransaction {
        require(entryIds.size in 2..4) { "a superset group must have 2 to 4 exercises, got ${entryIds.size}" }

        val dayEntries = programDayExerciseDao.getForDay(programDayId)
        val entries = dayEntries.filter { it.id in entryIds }
        require(entries.size == entryIds.size) { "one or more entryIds do not belong to programDayId $programDayId" }

        val sorted = entries.sortedBy { it.position }
        val positions = sorted.map { it.position }
        val isContiguous = positions.zipWithNext().all { (a, b) -> b == a + 1 }
        require(isContiguous) { "entries must be contiguous by position to form a superset, got positions $positions" }

        val nextGroupId = (dayEntries.mapNotNull { it.supersetGroupId }.maxOrNull() ?: 0) + 1
        sorted.forEachIndexed { index, entry ->
            programDayExerciseDao.update(entry.copy(supersetGroupId = nextGroupId, supersetOrder = index + 1))
        }
    }

    suspend fun ungroupSuperset(entryIds: List<Long>) = database.withTransaction {
        entryIds.forEach { id ->
            val entry = programDayExerciseDao.getById(id) ?: return@forEach
            programDayExerciseDao.update(entry.copy(supersetGroupId = null, supersetOrder = null))
        }
    }
}

private fun ProgramEntity.toDomain(): Program = Program(
    id = id,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
    archivedAt = archivedAt,
)

private fun ProgramDayEntity.toDomain(): ProgramDay = ProgramDay(
    id = id,
    programId = programId,
    position = position,
    name = name,
    isRest = isRest,
    libraryCategory = libraryCategory,
)

private fun ProgramDay.toEntity(): ProgramDayEntity = ProgramDayEntity(
    id = id,
    programId = programId,
    position = position,
    name = name,
    isRest = isRest,
    libraryCategory = libraryCategory,
)

private fun ProgramDayExerciseEntity.toDomain(): ProgramDayExercise = ProgramDayExercise(
    id = id,
    programDayId = programDayId,
    exerciseId = exerciseId,
    position = position,
    supersetGroupId = supersetGroupId,
    supersetOrder = supersetOrder,
    targetSets = targetSets,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetWeightKg = targetWeightKg,
    restSecOverride = restSecOverride,
)
