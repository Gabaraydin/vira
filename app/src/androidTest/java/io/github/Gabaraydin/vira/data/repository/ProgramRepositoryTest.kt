package io.github.Gabaraydin.vira.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayEntity
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.ProgramDay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgramRepositoryTest {

    private lateinit var database: ViraDatabase
    private lateinit var repository: ProgramRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ViraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProgramRepository(
            database,
            database.programDao(),
            database.programDayDao(),
            database.programDayExerciseDao(),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun seedExercises(count: Int): List<Long> = (1..count).map { i ->
        database.exerciseDao().insert(
            ExerciseEntity(
                nameEn = "Exercise $i",
                nameTr = "Egzersiz $i",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = "",
                equipment = Equipment.BARBELL,
                isBodyweight = false,
                isUnilateral = false,
                defaultRestSec = null,
                isCustom = false,
                isArchived = false,
                notes = null,
            ),
        )
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
            fail("expected an IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    // --- one active program ---

    @Test
    fun onlyOneProgramIsEverActive() = runTest {
        val p1 = repository.createProgram("Push Pull Legs", createdAt = 1)
        val p2 = repository.createProgram("Upper Lower", createdAt = 2)

        repository.setActiveProgram(p1)
        assertTrue(requireNotNull(database.programDao().getById(p1)).isActive)
        assertFalse(requireNotNull(database.programDao().getById(p2)).isActive)

        repository.setActiveProgram(p2)
        assertFalse(requireNotNull(database.programDao().getById(p1)).isActive)
        assertTrue(requireNotNull(database.programDao().getById(p2)).isActive)
    }

    // --- contiguous day positions ---

    @Test
    fun addingDaysKeepsPositionsContiguousFromZero() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        repository.addDay(programId, "Day A", isRest = false, libraryCategory = null)
        repository.addDay(programId, "Day B", isRest = false, libraryCategory = null)
        repository.addDay(programId, "Day C", isRest = false, libraryCategory = null)

        val positions = database.programDayDao().getForProgram(programId).sortedBy { it.position }.map { it.position }
        assertEquals(listOf(0, 1, 2), positions)
    }

    @Test
    fun removingAMiddleDayCompactsRemainingPositions() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        repository.addDay(programId, "Day A", isRest = false, libraryCategory = null)
        val dayB = repository.addDay(programId, "Day B", isRest = false, libraryCategory = null)
        repository.addDay(programId, "Day C", isRest = false, libraryCategory = null)

        val toRemove = requireNotNull(database.programDayDao().getById(dayB))
        repository.removeDay(toRemove.toDomain())

        val remaining = database.programDayDao().getForProgram(programId).sortedBy { it.position }
        assertEquals(listOf("Day A", "Day C"), remaining.map { it.name })
        assertEquals(listOf(0, 1), remaining.map { it.position })
    }

    @Test
    fun reorderingDaysAssignsPositionsByTheGivenOrder() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayA = repository.addDay(programId, "Day A", isRest = false, libraryCategory = null)
        val dayB = repository.addDay(programId, "Day B", isRest = false, libraryCategory = null)
        val dayC = repository.addDay(programId, "Day C", isRest = false, libraryCategory = null)

        repository.reorderDays(programId, listOf(dayC, dayA, dayB))

        val byId = database.programDayDao().getForProgram(programId).associateBy { it.id }
        assertEquals(0, byId.getValue(dayC).position)
        assertEquals(1, byId.getValue(dayA).position)
        assertEquals(2, byId.getValue(dayB).position)
    }

    // --- superset group size and contiguity ---

    @Test
    fun groupingTwoContiguousExercisesSucceeds() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val entryIds = seedExercises(2).map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12,
                targetWeightKg = null, restSecOverride = null,
            )
        }

        repository.groupIntoSuperset(dayId, entryIds)

        val entries = database.programDayExerciseDao().getForDay(dayId).sortedBy { it.position }
        assertTrue(entries.all { it.supersetGroupId == entries[0].supersetGroupId })
        assertEquals(listOf(1, 2), entries.map { it.supersetOrder })
    }

    @Test
    fun groupingFewerThanTwoIsRejected() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val entryIds = seedExercises(1).map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = null, targetRepsMax = null,
                targetWeightKg = null, restSecOverride = null,
            )
        }

        assertRejected { repository.groupIntoSuperset(dayId, entryIds) }
    }

    @Test
    fun groupingMoreThanFourIsRejected() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val entryIds = seedExercises(5).map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = null, targetRepsMax = null,
                targetWeightKg = null, restSecOverride = null,
            )
        }

        assertRejected { repository.groupIntoSuperset(dayId, entryIds) }
    }

    @Test
    fun groupingNonContiguousExercisesIsRejected() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val entryIds = seedExercises(3).map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = null, targetRepsMax = null,
                targetWeightKg = null, restSecOverride = null,
            )
        }
        // positions are 0, 1, 2 — take the first and last, skipping the middle one.
        val nonContiguous = listOf(entryIds[0], entryIds[2])

        assertRejected { repository.groupIntoSuperset(dayId, nonContiguous) }
    }

    // --- updateDay / duplicateProgram ---

    @Test
    fun updateDayPersistsANameChangeAndRestToggle() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Day A", isRest = false, libraryCategory = null)
        val day = requireNotNull(database.programDayDao().getById(dayId)).toDomain()

        repository.updateDay(day.copy(name = "Renamed", isRest = true))

        val updated = requireNotNull(database.programDayDao().getById(dayId))
        assertEquals("Renamed", updated.name)
        assertTrue(updated.isRest)
    }

    @Test
    fun duplicateProgramCopiesDaysAndPlannedExercises() = runTest {
        val sourceId = repository.createProgram("Original", createdAt = 1)
        val dayId = repository.addDay(sourceId, "Push", isRest = false, libraryCategory = null)
        repository.addDay(sourceId, "Rest", isRest = true, libraryCategory = null)
        val exerciseId = seedExercises(1).single()
        repository.addExerciseToDay(
            dayId, exerciseId, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12,
            targetWeightKg = null, restSecOverride = null,
        )

        val copyId = repository.duplicateProgram(sourceId, newName = "Original copy", createdAt = 2)

        assertFalse(requireNotNull(database.programDao().getById(copyId)).isActive)
        val copiedDays = database.programDayDao().getForProgram(copyId).sortedBy { it.position }
        assertEquals(listOf("Push", "Rest"), copiedDays.map { it.name })
        val copiedExercises = database.programDayExerciseDao().getForDay(copiedDays[0].id)
        assertEquals(1, copiedExercises.size)
        assertEquals(exerciseId, copiedExercises[0].exerciseId)
        assertEquals(8, copiedExercises[0].targetRepsMin)

        // The source is untouched.
        val sourceDays = database.programDayDao().getForProgram(sourceId)
        assertEquals(2, sourceDays.size)
    }

    // --- removeExerciseFromDay / reorderExercisesInDay ---

    @Test
    fun removingAnExerciseCompactsRemainingPositions() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val exerciseIds = seedExercises(3)
        val entryIds = exerciseIds.map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = null, targetRepsMax = null,
                targetWeightKg = null, restSecOverride = null,
            )
        }
        repository.removeExerciseFromDay(entryIds[1])

        val remaining = database.programDayExerciseDao().getForDay(dayId).sortedBy { it.position }
        assertEquals(2, remaining.size)
        assertEquals(listOf(0, 1), remaining.map { it.position })
    }

    @Test
    fun removingAGroupedExerciseUngroupsTheLoneSurvivor() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val entryIds = seedExercises(2).map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = null, targetRepsMax = null,
                targetWeightKg = null, restSecOverride = null,
            )
        }
        repository.groupIntoSuperset(dayId, entryIds)

        repository.removeExerciseFromDay(entryIds[0])

        val survivor = requireNotNull(database.programDayExerciseDao().getById(entryIds[1]))
        assertEquals(null, survivor.supersetGroupId)
        assertEquals(null, survivor.supersetOrder)
    }

    @Test
    fun reorderingExercisesAssignsPositionsByTheGivenOrder() = runTest {
        val programId = repository.createProgram("Program", createdAt = 1)
        val dayId = repository.addDay(programId, "Push", isRest = false, libraryCategory = null)
        val exerciseIds = seedExercises(3)
        val entryIds = exerciseIds.map { exId ->
            repository.addExerciseToDay(
                dayId, exId, targetSets = 3, targetRepsMin = null, targetRepsMax = null,
                targetWeightKg = null, restSecOverride = null,
            )
        }

        repository.reorderExercisesInDay(dayId, listOf(entryIds[2], entryIds[0], entryIds[1]))

        val byId = database.programDayExerciseDao().getForDay(dayId).associateBy { it.id }
        assertEquals(0, byId.getValue(entryIds[2]).position)
        assertEquals(1, byId.getValue(entryIds[0]).position)
        assertEquals(2, byId.getValue(entryIds[1]).position)
    }
}

private fun ProgramDayEntity.toDomain() = ProgramDay(
    id = id,
    programId = programId,
    position = position,
    name = name,
    isRest = isRest,
    libraryCategory = libraryCategory,
)
