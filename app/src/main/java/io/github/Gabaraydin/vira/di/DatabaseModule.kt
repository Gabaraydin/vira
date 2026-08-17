package io.github.Gabaraydin.vira.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.dao.BodyMeasurementDao
import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutSetDao
import io.github.Gabaraydin.vira.data.local.seed.ExerciseSeeder
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideViraDatabase(
        @ApplicationContext context: Context,
        exerciseSeederProvider: Provider<ExerciseSeeder>,
    ): ViraDatabase =
        Room.databaseBuilder(context, ViraDatabase::class.java, "vira.db")
            .addCallback(SeedOnCreateCallback(exerciseSeederProvider))
            .build()

    @Provides
    fun provideExerciseDao(db: ViraDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideProgramDao(db: ViraDatabase): ProgramDao = db.programDao()

    @Provides
    fun provideProgramDayDao(db: ViraDatabase): ProgramDayDao = db.programDayDao()

    @Provides
    fun provideProgramDayExerciseDao(db: ViraDatabase): ProgramDayExerciseDao = db.programDayExerciseDao()

    @Provides
    fun provideWorkoutDao(db: ViraDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun provideWorkoutSetDao(db: ViraDatabase): WorkoutSetDao = db.workoutSetDao()

    @Provides
    fun provideBodyMeasurementDao(db: ViraDatabase): BodyMeasurementDao = db.bodyMeasurementDao()
}

// A Provider defers resolving ExerciseSeeder (which needs a DAO off this very database)
// until the callback actually fires, breaking what would otherwise be a construction cycle.
private class SeedOnCreateCallback(
    private val exerciseSeederProvider: Provider<ExerciseSeeder>,
) : RoomDatabase.Callback() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch { exerciseSeederProvider.get().seedIfEmpty() }
    }
}
