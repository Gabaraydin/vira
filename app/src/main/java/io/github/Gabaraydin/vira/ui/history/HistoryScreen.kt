package io.github.Gabaraydin.vira.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryRoute(
    onOpenWorkout: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCalendar by remember { mutableStateOf(true) }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        HistoryScreen(
            uiState = uiState,
            showCalendar = showCalendar,
            onToggleView = { showCalendar = !showCalendar },
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onOpenWorkout = onOpenWorkout,
        )
    }
}

@Composable
private fun HistoryScreen(
    uiState: HistoryUiState,
    showCalendar: Boolean,
    onToggleView: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewToggleButton(stringResource(R.string.history_tab_calendar), selected = showCalendar, onClick = onToggleView)
            ViewToggleButton(stringResource(R.string.history_tab_list), selected = !showCalendar, onClick = onToggleView)
        }
        Spacer(Modifier.height(16.dp))

        if (showCalendar) {
            CalendarView(uiState.displayMonth, uiState.workoutIdByDate, onPreviousMonth, onNextMonth, onOpenWorkout)
        } else {
            ListView(uiState.listItems, onOpenWorkout)
        }
    }
}

@Composable
private fun ViewToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun CalendarView(
    month: YearMonth,
    workoutIdByDate: Map<LocalDate, Long>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreviousMonth) { Text(stringResource(R.string.history_month_previous)) }
            Text(month.toString(), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNextMonth) { Text(stringResource(R.string.history_month_next)) }
        }
        Spacer(Modifier.height(8.dp))

        val firstDay = month.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value - 1
        val cells = List<LocalDate?>(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val paddedCells = cells + List((7 - cells.size % 7) % 7) { null }

        paddedCells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date -> CalendarCell(date, workoutIdByDate, onOpenWorkout, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    workoutIdByDate: Map<LocalDate, Long>,
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (date != null) {
            val workoutId = workoutIdByDate[date]
            val cellModifier = if (workoutId != null) Modifier.clickable { onOpenWorkout(workoutId) } else Modifier
            Column(
                modifier = cellModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(date.dayOfMonth.toString())
                if (workoutId != null) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ListView(items: List<HistoryListItem>, onOpenWorkout: (Long) -> Unit) {
    if (items.isEmpty()) {
        Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                when (item) {
                    is HistoryListItem.CycleHeader -> Text(
                        stringResource(
                            R.string.history_cycle_header_format,
                            item.cycleIndex + 1,
                            item.completedDays,
                            item.totalDays,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is HistoryListItem.WorkoutRow -> Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenWorkout(item.workoutId) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.dayName)
                            Text(item.date.toString(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}
