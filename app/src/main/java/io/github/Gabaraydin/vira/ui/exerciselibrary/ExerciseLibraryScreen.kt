package io.github.Gabaraydin.vira.ui.exerciselibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.ui.common.displayName

@Composable
fun ExerciseLibraryRoute(
    onOpenExercise: (Long) -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    ExerciseLibraryScreen(
        uiState = uiState,
        onQueryChange = viewModel::setQuery,
        onMuscleFilterChange = viewModel::setMuscleFilter,
        onEquipmentFilterChange = viewModel::setEquipmentFilter,
        onCustomOnlyChange = viewModel::setCustomOnly,
        onOpenExercise = onOpenExercise,
        onAddCustom = { showAddDialog = true },
    )

    if (showAddDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, muscle, equipment ->
                showAddDialog = false
                viewModel.addCustom(name, muscle, equipment, onOpenExercise)
            },
        )
    }
}

@Composable
private fun ExerciseLibraryScreen(
    uiState: ExerciseLibraryUiState,
    onQueryChange: (String) -> Unit,
    onMuscleFilterChange: (MuscleGroup?) -> Unit,
    onEquipmentFilterChange: (Equipment?) -> Unit,
    onCustomOnlyChange: (Boolean) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onAddCustom: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.exercise_library_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text(stringResource(R.string.exercise_library_search_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = uiState.customOnly,
                    onClick = { onCustomOnlyChange(!uiState.customOnly) },
                    label = { Text(stringResource(R.string.exercise_library_filter_custom_only)) },
                )
            }
            items(MuscleGroup.entries.toList()) { muscle ->
                FilterChip(
                    selected = uiState.muscleFilter == muscle,
                    onClick = { onMuscleFilterChange(muscle) },
                    label = { Text(muscle.displayName()) },
                )
            }
            items(Equipment.entries.toList()) { equipment ->
                FilterChip(
                    selected = uiState.equipmentFilter == equipment,
                    onClick = { onEquipmentFilterChange(equipment) },
                    label = { Text(equipment.displayName()) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (uiState.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.exercise_library_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise, onClick = { onOpenExercise(exercise.id) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exercise_library_add_custom))
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseRowUiModel, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${exercise.muscle.displayName()} · ${exercise.equipment.displayName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                exercise.lastPerformed?.let { stringResource(R.string.exercise_library_last_performed_format, it.toString()) }
                    ?: stringResource(R.string.exercise_library_last_performed_never),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MuscleGroup, Equipment) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var equipment by remember { mutableStateOf(Equipment.BARBELL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exercise_library_add_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.exercise_library_add_dialog_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.exercise_library_add_dialog_muscle_label), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(MuscleGroup.entries.toList()) { m ->
                        FilterChip(selected = muscle == m, onClick = { muscle = m }, label = { Text(m.displayName()) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.exercise_library_add_dialog_equipment_label), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(Equipment.entries.toList()) { e ->
                        FilterChip(selected = equipment == e, onClick = { equipment = e }, label = { Text(e.displayName()) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, muscle, equipment) }) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
