package io.github.Gabaraydin.vira.data.repository

import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.Exercise
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepository @Inject constructor(private val exerciseDao: ExerciseDao) {

    fun observeActive(): Flow<List<Exercise>> = exerciseDao.observeActive().map { it.map(ExerciseEntity::toDomain) }

    fun observeAll(): Flow<List<Exercise>> = exerciseDao.observeAll().map { it.map(ExerciseEntity::toDomain) }

    suspend fun getById(id: Long): Exercise? = exerciseDao.getById(id)?.toDomain()

    suspend fun addCustom(nameEn: String, primaryMuscle: MuscleGroup, equipment: Equipment): Long =
        exerciseDao.insert(
            ExerciseEntity(
                nameEn = nameEn,
                nameTr = nameEn,
                primaryMuscle = primaryMuscle,
                secondaryMuscles = "",
                equipment = equipment,
                isBodyweight = false,
                isUnilateral = false,
                defaultRestSec = null,
                isCustom = true,
                isArchived = false,
                notes = null,
            ),
        )

    suspend fun archive(exercise: Exercise) {
        exerciseDao.update(exercise.toEntity().copy(isArchived = true))
    }
}

private fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    nameEn = nameEn,
    nameTr = nameTr,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = if (secondaryMuscles.isBlank()) {
        emptyList()
    } else {
        secondaryMuscles.split(",").map { MuscleGroup.valueOf(it.trim()) }
    },
    equipment = equipment,
    isBodyweight = isBodyweight,
    isUnilateral = isUnilateral,
    defaultRestSec = defaultRestSec,
    isCustom = isCustom,
    isArchived = isArchived,
    notes = notes,
)

private fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    nameEn = nameEn,
    nameTr = nameTr,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles.joinToString(",") { it.name },
    equipment = equipment,
    isBodyweight = isBodyweight,
    isUnilateral = isUnilateral,
    defaultRestSec = defaultRestSec,
    isCustom = isCustom,
    isArchived = isArchived,
    notes = notes,
)
