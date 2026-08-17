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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R

@Composable
fun PlannedExercisesRoute(
    onAddExercise: (Long) -> Unit,
    viewModel: PlannedExercisesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(setOf<Long>()) }

    PlannedExercisesScreen(
        uiState = uiState,
        selected = selected,
        onToggleSelected = { id ->
            selected = if (id in selected) selected - id else selected + id
        },
        onAddExercise = { onAddExercise(viewModel.programDayId) },
        onRemoveExercise = { id -> viewModel.removeExercise(id); selected = selected - id },
        onUpdateSets = viewModel::updateSets,
        onUpdateRepsMin = viewModel::updateRepsMin,
        onUpdateRepsMax = viewModel::updateRepsMax,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        onGroupSelected = {
            viewModel.groupIntoSuperset(selected.toList())
            selected = emptySet()
        },
        onUngroup = { groupId ->
            val ids = uiState.exercises.filter { it.supersetGroupId == groupId }.map { it.entryId }
            viewModel.ungroupSuperset(ids)
        },
    )
}

@Composable
private fun PlannedExercisesScreen(
    uiState: PlannedExercisesUiState,
    selected: Set<Long>,
    onToggleSelected: (Long) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onUpdateSets: (PlannedExerciseUiModel, Int) -> Unit,
    onUpdateRepsMin: (PlannedExerciseUiModel, Int?) -> Unit,
    onUpdateRepsMax: (PlannedExerciseUiModel, Int?) -> Unit,
    onMoveUp: (PlannedExerciseUiModel) -> Unit,
    onMoveDown: (PlannedExerciseUiModel) -> Unit,
    onGroupSelected: () -> Unit,
    onUngroup: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.day_exercises_title, uiState.dayName),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.day_exercises_add))
        }
        if (selected.size in 2..4) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onGroupSelected, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.day_exercises_group_superset, selected.size))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (uiState.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.day_exercises_empty))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.exercises, key = { it.entryId }) { model ->
                    val isFirst = model.position == 0
                    val isLast = model.position == uiState.exercises.size - 1
                    PlannedExerciseRow(
                        model, isFirst, isLast,
                        isSelected = model.entryId in selected,
                        onToggleSelected = { onToggleSelected(model.entryId) },
                        onRemove = { onRemoveExercise(model.entryId) },
                        onUpdateSets = { onUpdateSets(model, it) },
                        onUpdateRepsMin = { onUpdateRepsMin(model, it) },
                        onUpdateRepsMax = { onUpdateRepsMax(model, it) },
                        onMoveUp = { onMoveUp(model) },
                        onMoveDown = { onMoveDown(model) },
                        onUngroup = { model.supersetGroupId?.let(onUngroup) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedExerciseRow(
    model: PlannedExerciseUiModel,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onRemove: () -> Unit,
    onUpdateSets: (Int) -> Unit,
    onUpdateRepsMin: (Int?) -> Unit,
    onUpdateRepsMax: (Int?) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUngroup: () -> Unit,
) {
    val isGrouped = model.supersetGroupId != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isGrouped) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
                }
                model.supersetLabel?.let {
                    Text(it, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(model.exerciseName, style = MaterialTheme.typography.headlineMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = stringResource(R.string.day_exercises_sets_label),
                    value = model.targetSets,
                    onCommit = { onUpdateSets(it ?: model.targetSets) },
                    modifier = Modifier.width(90.dp),
                )
                NumberField(
                    label = stringResource(R.string.day_exercises_reps_min_label),
                    value = model.targetRepsMin,
                    onCommit = onUpdateRepsMin,
                    modifier = Modifier.width(90.dp),
                )
                NumberField(
                    label = stringResource(R.string.day_exercises_reps_max_label),
                    value = model.targetRepsMax,
                    onCommit = onUpdateRepsMax,
                    modifier = Modifier.width(90.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isGrouped) {
                    TextButton(onClick = onUngroup) { Text(stringResource(R.string.day_exercises_ungroup_superset)) }
                } else {
                    Row {
                        TextButton(onClick = onMoveUp, enabled = !isFirst) {
                            Text(stringResource(R.string.day_editor_move_up))
                        }
                        TextButton(onClick = onMoveDown, enabled = !isLast) {
                            Text(stringResource(R.string.day_editor_move_down))
                        }
                    }
                }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.day_exercises_remove)) }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int?, onCommit: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.filter(Char::isDigit) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                val parsed = text.toIntOrNull()
                if (parsed != value) onCommit(parsed)
            }
        },
    )
}
