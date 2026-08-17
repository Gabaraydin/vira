package io.github.Gabaraydin.vira.data.repository

import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutSetDao
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.Exercise
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

data class ExerciseSetHistory(val set: WorkoutSet, val date: LocalDate)

class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutSetDao: WorkoutSetDao,
) {

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

    suspend fun updateDefaultRestSec(exercise: Exercise, seconds: Int?) {
        exerciseDao.update(exercise.toEntity().copy(defaultRestSec = seconds))
    }

    // Backs Exercise Detail's e1RM chart, PR table, and session history all at once —
    // every set the exercise has in a finished workout, each tagged with its session date.
    fun observeSetHistory(exerciseId: Long): Flow<List<ExerciseSetHistory>> =
        workoutSetDao.observeAllSetsForExercise(exerciseId).map { rows ->
            rows.map { ExerciseSetHistory(it.set.toDomain(), it.date) }
        }

    fun observeLastPerformedDates(): Flow<Map<Long, LocalDate>> =
        workoutSetDao.observeLastPerformedDates().map { rows -> rows.associate { it.exerciseId to it.lastDate } }
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
