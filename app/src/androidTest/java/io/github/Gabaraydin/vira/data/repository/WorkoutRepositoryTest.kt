package io.github.Gabaraydin.vira.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.WorkoutSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {

    private lateinit var database: ViraDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ViraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database, database.workoutDao(), database.workoutSetDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun startingASessionWithNoneInProgressSucceeds() = runTest {
        val id = repository.startSession(
            programDayId = null,
            dayNameSnapshot = "Ad-hoc",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 1),
            startedAt = 1000,
        )

        assertNotNull(database.workoutDao().getById(id))
        assertNull(requireNotNull(database.workoutDao().getById(id)).finishedAt)
    }

    @Test
    fun startingASecondSessionWhileOneIsUnfinishedIsRejected() = runTest {
        repository.startSession(
            programDayId = null,
            dayNameSnapshot = "First",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 1),
            startedAt = 1000,
        )

        try {
            repository.startSession(
                programDayId = null,
                dayNameSnapshot = "Second",
                programNameSnapshot = null,
                date = LocalDate.of(2026, 1, 1),
                startedAt = 2000,
            )
            fail("expected an IllegalStateException")
        } catch (expected: IllegalStateException) {
            // expected
        }

        assertEquals(1, database.workoutDao().observeAll().first().size)
    }

    @Test
    fun startingANewSessionAfterFinishingTheOldOneSucceeds() = runTest {
        val firstId = repository.startSession(
            programDayId = null,
            dayNameSnapshot = "First",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 1),
            startedAt = 1000,
        )
        repository.finishSession(firstId, finishedAt = 1500)

        val secondId = repository.startSession(
            programDayId = null,
            dayNameSnapshot = "Second",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 2),
            startedAt = 2000,
        )

        assertNull(requireNotNull(database.workoutDao().getById(secondId)).finishedAt)
        assertNotNull(requireNotNull(database.workoutDao().getById(firstId)).finishedAt)
    }

    private suspend fun seedExercise(): Long = database.exerciseDao().insert(
        ExerciseEntity(
            nameEn = "Bench Press", nameTr = "Bench Press", primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = "", equipment = Equipment.BARBELL, isBodyweight = false, isUnilateral = false,
            defaultRestSec = null, isCustom = false, isArchived = false, notes = null,
        ),
    )

    // --- previous session reference ---

    @Test
    fun previousSessionSetsComeFromTheMostRecentOtherFinishedWorkout() = runTest {
        val exerciseId = seedExercise()
        val olderId = repository.startSession(null, "Older", null, LocalDate.of(2026, 1, 1), 1000)
        repository.addSet(
            WorkoutSet(0, olderId, exerciseId, 0, 1, 40.0, 10, null, false, true, 1000, null),
        )
        repository.finishSession(olderId, 1100)

        val newerId = repository.startSession(null, "Newer", null, LocalDate.of(2026, 1, 5), 1000)
        repository.addSet(
            WorkoutSet(0, newerId, exerciseId, 0, 1, 60.0, 8, null, false, true, 1000, null),
        )
        repository.addSet(
            WorkoutSet(0, newerId, exerciseId, 1, 2, 60.0, 7, null, false, true, 1000, null),
        )
        repository.finishSession(newerId, 1100)

        val current = repository.startSession(null, "Current", null, LocalDate.of(2026, 1, 10), 1000)

        val previous = repository.getPreviousSessionSets(exerciseId, excludeWorkoutId = current)

        assertEquals(2, previous.size)
        assertTrue(previous.all { it.weightKg == 60.0 })
    }

    @Test
    fun previousSessionSetsIsEmptyWhenTheExerciseHasNeverBeenLogged() = runTest {
        val exerciseId = seedExercise()
        val current = repository.startSession(null, "Current", null, LocalDate.of(2026, 1, 10), 1000)

        val previous = repository.getPreviousSessionSets(exerciseId, excludeWorkoutId = current)

        assertTrue(previous.isEmpty())
    }

    // --- add unplanned exercise mid-session ---

    @Test
    fun addingAnUnplannedExerciseAppendsABlankSetAtTheNextPosition() = runTest {
        val exerciseId = seedExercise()
        val workoutId = repository.startSession(null, "Ad-hoc", null, LocalDate.of(2026, 1, 1), 1000)
        repository.addSet(WorkoutSet(0, workoutId, exerciseId, 0, 1, 40.0, 10, null, false, true, 1000, null))

        val newExerciseId = database.exerciseDao().insert(
            ExerciseEntity(
                nameEn = "Squat", nameTr = "Squat", primaryMuscle = MuscleGroup.QUADS,
                secondaryMuscles = "", equipment = Equipment.BARBELL, isBodyweight = false, isUnilateral = false,
                defaultRestSec = null, isCustom = false, isArchived = false, notes = null,
            ),
        )
        repository.addUnplannedExercise(workoutId, newExerciseId)

        val sets = database.workoutSetDao().getForWorkout(workoutId)
        assertEquals(2, sets.size)
        val added = sets.first { it.exerciseId == newExerciseId }
        assertEquals(1, added.position)
        assertEquals(0.0, added.weightKg, 0.0)
        assertEquals(false, added.isCompleted)
    }
}
