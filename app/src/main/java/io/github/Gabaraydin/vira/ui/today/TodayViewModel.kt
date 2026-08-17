package io.github.Gabaraydin.vira.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.data.repository.WorkoutRepository
import io.github.Gabaraydin.vira.domain.cycle.WorkoutForCycle
import io.github.Gabaraydin.vira.domain.cycle.computeCycle
import io.github.Gabaraydin.vira.domain.model.Program
import io.github.Gabaraydin.vira.domain.model.ProgramDay
import io.github.Gabaraydin.vira.domain.model.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = programRepository.observeActiveProgram()
        .flatMapLatest { program -> observeStateForProgram(program) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun observeStateForProgram(program: Program?) =
        if (program == null) {
            flowOf(TodayUiState(isLoading = false, programName = null))
        } else {
            combine(
                programRepository.observeDaysForProgram(program.id),
                workoutRepository.observeCompletedForCycle(),
                workoutRepository.observeMostRecentFinished(),
                workoutRepository.observeUnfinishedSession(),
            ) { days, completed, mostRecent, unfinished ->
                RawState(program, days, completed, mostRecent, unfinished)
            }.mapLatest { it.toUiState() }
        }

    private suspend fun RawState.toUiState(): TodayUiState {
        if (days.isEmpty()) {
            return TodayUiState(isLoading = false, programName = program.name)
        }

        val daysByPosition = days.associateBy { it.position }
        val workoutsForCycle = completed.map { workout ->
            val position = daysByPosition.values.firstOrNull { it.id == workout.programDayId }?.position
            WorkoutForCycle(position = position, date = workout.date, startedAt = workout.startedAt)
        }
        val cycle = computeCycle(dayCount = days.size, workouts = workoutsForCycle)

        val cycleDayModels = cycle.days.map { dayState ->
            val day = daysByPosition.getValue(dayState.position)
            CycleDayUiModel(
                position = dayState.position,
                dayNumber = dayState.position + 1,
                name = day.name,
                isRest = day.isRest,
                status = dayState.status,
                doneDate = dayState.workoutDate,
            )
        }

        val nextDayInfo = daysByPosition[cycle.nextPosition]?.let { day ->
            val exerciseCount = programRepository.observeExercisesForDay(day.id).first().size
            NextDayInfo(
                programDayId = day.id,
                name = day.name,
                position = day.position,
                totalDays = days.size,
                isRest = day.isRest,
                plannedExerciseCount = exerciseCount,
            )
        }

        val today = LocalDate.now()
        val gap = mostRecent?.let { ChronoUnit.DAYS.between(it.date, today).toInt() }

        return TodayUiState(
            isLoading = false,
            programName = program.name,
            cycleDays = cycleDayModels,
            nextDay = nextDayInfo,
            lastWorkoutDayName = if (gap == 1) mostRecent.dayNameSnapshot else null,
            daysSinceLastWorkout = gap,
            unfinishedSessionId = unfinished?.id,
        )
    }

    suspend fun startWorkout(): Long {
        val state = uiState.value
        val day = requireNotNull(state.nextDay) { "startWorkout called with no next day" }
        return workoutRepository.startSession(
            programDayId = day.programDayId,
            dayNameSnapshot = day.name,
            programNameSnapshot = state.programName,
            date = LocalDate.now(),
            startedAt = System.currentTimeMillis(),
        )
    }

    suspend fun startAdHocWorkout(dayName: String): Long = workoutRepository.startSession(
        programDayId = null,
        dayNameSnapshot = dayName,
        programNameSnapshot = null,
        date = LocalDate.now(),
        startedAt = System.currentTimeMillis(),
    )

    fun markRestDayDone() {
        val state = uiState.value
        val day = state.nextDay ?: return
        viewModelScope.launch {
            workoutRepository.logRestDay(
                programDayId = day.programDayId,
                dayNameSnapshot = day.name,
                programNameSnapshot = state.programName,
                date = LocalDate.now(),
                at = System.currentTimeMillis(),
            )
        }
    }

    // Called for both a template tap and "build my own" — the Composable resolves which
    // display name to use; loading a template's actual days/exercises is issue #10's job.
    fun createProgram(name: String) {
        viewModelScope.launch {
            val id = programRepository.createProgram(name = name, createdAt = System.currentTimeMillis())
            programRepository.setActiveProgram(id)
        }
    }
}

private data class RawState(
    val program: Program,
    val days: List<ProgramDay>,
    val completed: List<Workout>,
    val mostRecent: Workout?,
    val unfinished: Workout?,
)
