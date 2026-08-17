package io.github.Gabaraydin.vira.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.data.repository.WorkoutRepository
import io.github.Gabaraydin.vira.domain.cycle.groupCompletedCycles
import io.github.Gabaraydin.vira.domain.model.ProgramDay
import io.github.Gabaraydin.vira.domain.model.Workout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val displayMonth = MutableStateFlow(YearMonth.now())

    private val daysForActiveProgram = programRepository.observeActiveProgram()
        .flatMapLatest { program ->
            program?.let { programRepository.observeDaysForProgram(it.id) } ?: flowOf(emptyList())
        }

    val uiState: StateFlow<HistoryUiState> = combine(
        workoutRepository.observeAll(),
        daysForActiveProgram,
        displayMonth,
    ) { workouts, days, month ->
        RawState(workouts.filter { it.finishedAt != null }, days, month)
    }
        .mapLatest { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun previousMonth() {
        displayMonth.value = displayMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        displayMonth.value = displayMonth.value.plusMonths(1)
    }

    private fun RawState.toUiState(): HistoryUiState {
        val positionByDayId = days.associate { it.id to it.position }
        val dayCount = days.size

        val cycleWorkouts = workouts.filter { it.programDayId != null && positionByDayId[it.programDayId] != null }
        val adHocWorkouts = workouts.filter { it.programDayId == null || positionByDayId[it.programDayId] == null }

        val cycles = if (dayCount > 0 && cycleWorkouts.isNotEmpty()) {
            groupCompletedCycles(
                dayCount = dayCount,
                items = cycleWorkouts,
                position = { positionByDayId[it.programDayId] },
                date = { it.date },
                startedAt = { it.startedAt },
            )
        } else {
            emptyList()
        }

        // Each cycle group is one block (header + its rows, newest first); ad-hoc sessions
        // are their own single-row blocks. Sorting the blocks themselves by most-recent
        // activity keeps everything in one coherent reverse-chronological list even though
        // a cycle can span a much wider date range than a single ad-hoc entry.
        val cycleBlocks = cycles.map { cycle ->
            val rows = cycle.workouts.sortedByDescending { it.startedAt }
            Block(
                sortKey = rows.first().startedAt,
                items = listOf(HistoryListItem.CycleHeader(cycle.cycleIndex, cycle.completedDays, cycle.totalDays)) +
                    rows.map { it.toRow() },
            )
        }
        val adHocBlocks = adHocWorkouts.map { Block(sortKey = it.startedAt, items = listOf(it.toRow())) }

        return HistoryUiState(
            isLoading = false,
            displayMonth = month,
            workoutIdByDate = workouts.associate { it.date to it.id },
            listItems = (cycleBlocks + adHocBlocks).sortedByDescending { it.sortKey }.flatMap { it.items },
        )
    }
}

private data class Block(val sortKey: Long, val items: List<HistoryListItem>)

private data class RawState(val workouts: List<Workout>, val days: List<ProgramDay>, val month: YearMonth)

private fun Workout.toRow() = HistoryListItem.WorkoutRow(workoutId = id, date = date, dayName = dayNameSnapshot)
