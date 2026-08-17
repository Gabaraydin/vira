package io.github.Gabaraydin.vira.ui.exercisepicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.model.Exercise
import io.github.Gabaraydin.vira.domain.model.displayName

@Composable
fun ExercisePickerRoute(
    onDone: () -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExercisePickerScreen(
        uiState = uiState,
        onQueryChange = viewModel::setQuery,
        onToggleSelected = viewModel::toggleSelected,
        onConfirm = { viewModel.confirmSelection(onDone) },
    )
}

@Composable
private fun ExercisePickerScreen(
    uiState: ExercisePickerUiState,
    onQueryChange: (String) -> Unit,
    onToggleSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.exercise_picker_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text(stringResource(R.string.exercise_picker_search_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        if (uiState.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.exercise_picker_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        isSelected = exercise.id in uiState.selectedIds,
                        onToggle = { onToggleSelected(exercise.id) },
                    )
                }
            }
        }

        if (uiState.selectedIds.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.exercise_picker_add_selected, uiState.selectedIds.size))
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        Text(exercise.displayName())
    }
}
