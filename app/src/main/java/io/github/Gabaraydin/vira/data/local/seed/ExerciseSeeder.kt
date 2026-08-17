package io.github.Gabaraydin.vira.data.local.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.domain.seed.SeedExercise
import io.github.Gabaraydin.vira.domain.seed.parseExerciseCsv
import javax.inject.Inject

private const val SEED_ASSET_NAME = "seed_exercises.csv"

class ExerciseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
) {
    suspend fun seedIfEmpty() {
        if (exerciseDao.count() > 0) return

        val csvText = context.assets.open(SEED_ASSET_NAME).bufferedReader().use { it.readText() }
        val entities = parseExerciseCsv(csvText).map { it.toEntity() }
        exerciseDao.insertAll(entities)
    }
}

private fun SeedExercise.toEntity(): ExerciseEntity = ExerciseEntity(
    nameEn = nameEn,
    nameTr = nameTr,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles.joinToString(",") { it.name },
    equipment = equipment,
    isBodyweight = isBodyweight,
    isUnilateral = isUnilateral,
    defaultRestSec = null,
    isCustom = false,
    isArchived = false,
    notes = null,
)
