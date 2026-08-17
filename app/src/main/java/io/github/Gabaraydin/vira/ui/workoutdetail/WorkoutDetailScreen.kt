package io.github.Gabaraydin.vira.ui.workoutdetail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun WorkoutDetailRoute(
    onDeleted: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        WorkoutDetailScreen(
            uiState = uiState,
            onUpdateNote = viewModel::updateNote,
            onUpdateWeight = viewModel::updateWeight,
            onUpdateReps = viewModel::updateReps,
            onToggleWarmup = viewModel::toggleWarmup,
            onDeleteSet = viewModel::deleteSet,
            onDeleteWorkout = { showDeleteConfirm = true },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.workout_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.workout_detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch { viewModel.deleteWorkout(); onDeleted() }
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun WorkoutDetailScreen(
    uiState: WorkoutDetailUiState,
    onUpdateNote: (String) -> Unit,
    onUpdateWeight: (WorkoutDetailSetUiModel, Double) -> Unit,
    onUpdateReps: (WorkoutDetailSetUiModel, Int) -> Unit,
    onToggleWarmup: (WorkoutDetailSetUiModel) -> Unit,
    onDeleteSet: (WorkoutDetailSetUiModel) -> Unit,
    onDeleteWorkout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(uiState.dayName, style = MaterialTheme.typography.headlineMedium)
        Text(uiState.date, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                stringResource(R.string.workout_summary_volume_format, formatVolume(uiState.totalVolumeKg)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(durationText(uiState.durationSec), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))

        NoteField(uiState.note, onUpdateNote)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.exercises, key = { it.exerciseId }) { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                        exercise.sets.forEach { set ->
                            SetRow(set, onUpdateWeight, onUpdateReps, onToggleWarmup, onDeleteSet)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDeleteWorkout, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_detail_delete))
        }
    }
}

@Composable
private fun NoteField(note: String, onCommit: (String) -> Unit) {
    var text by remember(note) { mutableStateOf(note) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.workout_detail_note_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState -> if (!focusState.isFocused && text != note) onCommit(text) },
    )
}

@Composable
private fun SetRow(
    set: WorkoutDetailSetUiModel,
    onUpdateWeight: (WorkoutDetailSetUiModel, Double) -> Unit,
    onUpdateReps: (WorkoutDetailSetUiModel, Int) -> Unit,
    onToggleWarmup: (WorkoutDetailSetUiModel) -> Unit,
    onDeleteSet: (WorkoutDetailSetUiModel) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${set.setIndex}", modifier = Modifier.width(24.dp))
        DecimalField(
            label = stringResource(R.string.active_workout_weight_label),
            value = set.weightKg,
            onCommit = { onUpdateWeight(set, it ?: set.weightKg) },
            modifier = Modifier.width(80.dp),
        )
        IntField(
            label = stringResource(R.string.active_workout_reps_label),
            value = set.reps,
            onCommit = { onUpdateReps(set, it ?: set.reps) },
            modifier = Modifier.width(70.dp),
        )
        Text(stringResource(R.string.active_workout_warmup_label), style = MaterialTheme.typography.bodySmall)
        Checkbox(checked = set.isWarmup, onCheckedChange = { onToggleWarmup(set) })
        TextButton(onClick = { onDeleteSet(set) }) { Text(stringResource(R.string.active_workout_remove_set)) }
    }
}

@Composable
private fun DecimalField(label: String, value: Double?, onCommit: (Double?) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                val parsed = text.toDoubleOrNull()
                if (parsed != value) onCommit(parsed)
            }
        },
    )
}

@Composable
private fun IntField(label: String, value: Int?, onCommit: (Int?) -> Unit, modifier: Modifier = Modifier) {
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

private fun durationText(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatVolume(kg: Double): String = if (kg == kg.toLong().toDouble()) "${kg.toLong()}" else "%.1f".format(kg)
