package io.github.Gabaraydin.vira.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.Gabaraydin.vira.data.local.dao.BodyMeasurementDao
import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutSetDao
import io.github.Gabaraydin.vira.data.local.entity.BodyMeasurementEntity
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramDayExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ViraDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDao(): ProgramDao
    abstract fun programDayDao(): ProgramDayDao
    abstract fun programDayExerciseDao(): ProgramDayExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
}
