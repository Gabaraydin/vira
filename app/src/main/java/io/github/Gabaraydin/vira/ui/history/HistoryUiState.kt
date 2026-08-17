package io.github.Gabaraydin.vira.ui.history

import java.time.LocalDate
import java.time.YearMonth

sealed interface HistoryListItem {
    data class CycleHeader(val cycleIndex: Int, val completedDays: Int, val totalDays: Int) : HistoryListItem
    data class WorkoutRow(val workoutId: Long, val date: LocalDate, val dayName: String) : HistoryListItem
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val displayMonth: YearMonth = YearMonth.now(),
    // Every finished workout's date mapped to one workout on that date, for the calendar's
    // dots and tap-to-open; if more than one was logged on a date, an arbitrary one wins.
    val workoutIdByDate: Map<LocalDate, Long> = emptyMap(),
    // Newest first: cycle-grouped program-day workouts interleaved with ad-hoc rows in
    // date order, exactly as they'd naturally sort.
    val listItems: List<HistoryListItem> = emptyList(),
)
