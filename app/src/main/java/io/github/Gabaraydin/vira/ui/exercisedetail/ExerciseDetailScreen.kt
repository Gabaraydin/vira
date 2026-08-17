package io.github.Gabaraydin.vira.ui.exercisedetail

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.ui.charts.LineChart
import io.github.Gabaraydin.vira.ui.common.displayName
import kotlinx.coroutines.launch

@Composable
fun ExerciseDetailRoute(
    onArchived: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showArchiveConfirm by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        ExerciseDetailScreen(
            uiState = uiState,
            onUpdateRestOverride = viewModel::updateRestOverride,
            onArchive = { showArchiveConfirm = true },
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(stringResource(R.string.exercise_detail_archive_confirm_title)) },
            text = { Text(stringResource(R.string.exercise_detail_archive_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirm = false
                    scope.launch { viewModel.archive(); onArchived() }
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun ExerciseDetailScreen(
    uiState: ExerciseDetailUiState,
    onUpdateRestOverride: (Int?) -> Unit,
    onArchive: () -> Unit,
) {
    val yAxisFormat = stringResource(R.string.exercise_detail_e1rm_chart_y_format)
    val historySetFormat = stringResource(R.string.exercise_detail_history_set_format)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column {
                Text(uiState.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${uiState.muscle.displayName()} · ${uiState.equipment.displayName()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        item {
            Column {
                Text(stringResource(R.string.exercise_detail_e1rm_chart_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LineChart(
                    points = uiState.chartPoints,
                    emptyStateText = stringResource(R.string.exercise_detail_e1rm_chart_empty),
                    yAxisFormatter = { String.format(yAxisFormat, formatNumber(it)) },
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.exercise_detail_pr_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (uiState.bestOverallE1rm == null && uiState.repPrTable.isEmpty()) {
                        Text(stringResource(R.string.exercise_detail_pr_empty))
                    } else {
                        uiState.bestOverallE1rm?.let {
                            Text(stringResource(R.string.exercise_detail_pr_best_e1rm_format, formatNumber(it)))
                        }
                        uiState.repPrTable.forEach { pr ->
                            Text(stringResource(R.string.exercise_detail_pr_rep_row_format, pr.reps, formatNumber(pr.weightKg)))
                        }
                    }
                }
            }
        }

        item {
            RestOverrideField(uiState.restOverrideSeconds, onUpdateRestOverride)
        }

        item {
            Text(stringResource(R.string.exercise_detail_history_title), style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.history.isEmpty()) {
            item { Text(stringResource(R.string.exercise_detail_history_empty)) }
        } else {
            items(uiState.history) { session ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(session.date.toString(), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            session.sets.joinToString(", ") { String.format(historySetFormat, formatNumber(it.weightKg), it.reps) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        item {
            TextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.exercise_detail_archive))
            }
        }
    }
}

@Composable
private fun RestOverrideField(current: Int?, onCommit: (Int?) -> Unit) {
    var text by remember(current) { mutableStateOf(current?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.filter(Char::isDigit) },
        singleLine = true,
        label = { Text(stringResource(R.string.exercise_detail_rest_override_label)) },
        placeholder = { Text(stringResource(R.string.exercise_detail_rest_override_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                val parsed = text.toIntOrNull()
                if (parsed != current) onCommit(parsed)
            }
        },
    )
}

private fun formatNumber(value: Double): String = if (value == value.toLong().toDouble()) "${value.toLong()}" else "%.1f".format(value)
