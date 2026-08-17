package io.github.Gabaraydin.vira.ui.programeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.model.ProgramDay

@Composable
fun DayEditorRoute(viewModel: DayEditorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val newDayLabel = stringResource(R.string.day_editor_new_day_default_name)

    DayEditorScreen(
        uiState = uiState,
        onAddDay = { viewModel.addDay(newDayLabel) },
        onRemoveDay = viewModel::removeDay,
        onToggleRest = viewModel::toggleRest,
        onRenameDay = viewModel::renameDay,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
    )
}

@Composable
private fun DayEditorScreen(
    uiState: DayEditorUiState,
    onAddDay: () -> Unit,
    onRemoveDay: (ProgramDay) -> Unit,
    onToggleRest: (ProgramDay) -> Unit,
    onRenameDay: (ProgramDay, String) -> Unit,
    onMoveUp: (ProgramDay) -> Unit,
    onMoveDown: (ProgramDay) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.day_editor_title, uiState.programName),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddDay, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.day_editor_add_day))
        }
        Spacer(Modifier.height(16.dp))

        if (uiState.days.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.day_editor_empty))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.days, key = { it.id }) { day ->
                    val isFirst = day.position == 0
                    val isLast = day.position == uiState.days.size - 1
                    DayRow(day, isFirst, isLast, onRemoveDay, onToggleRest, onRenameDay, onMoveUp, onMoveDown)
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    day: ProgramDay,
    isFirst: Boolean,
    isLast: Boolean,
    onRemoveDay: (ProgramDay) -> Unit,
    onToggleRest: (ProgramDay) -> Unit,
    onRenameDay: (ProgramDay, String) -> Unit,
    onMoveUp: (ProgramDay) -> Unit,
    onMoveDown: (ProgramDay) -> Unit,
) {
    var name by remember(day.id) { mutableStateOf(day.name) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && name.isNotBlank() && name != day.name) {
                            onRenameDay(day, name)
                        }
                    },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = day.isRest, onCheckedChange = { onToggleRest(day) })
                    Text(stringResource(R.string.day_editor_rest_day_label))
                }
                Row {
                    TextButton(onClick = { onMoveUp(day) }, enabled = !isFirst) {
                        Text(stringResource(R.string.day_editor_move_up))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onMoveDown(day) }, enabled = !isLast) {
                        Text(stringResource(R.string.day_editor_move_down))
                    }
                }
            }
            TextButton(onClick = { onRemoveDay(day) }) {
                Text(stringResource(R.string.day_editor_delete_day))
            }
        }
    }
}
