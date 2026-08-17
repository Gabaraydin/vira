package io.github.Gabaraydin.vira.ui.workoutsummary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R

@Composable
fun WorkoutSummaryRoute(
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        WorkoutSummaryScreen(uiState = uiState, onDone = onDone)
    }
}

@Composable
private fun WorkoutSummaryScreen(uiState: WorkoutSummaryUiState, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(uiState.dayName, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        StatRow(
            label = stringResource(R.string.workout_summary_duration_label),
            value = durationText(uiState.durationSec),
            previousValue = uiState.comparison?.let { durationText(it.previousDurationSec) },
        )
        StatRow(
            label = stringResource(R.string.workout_summary_volume_label),
            value = stringResource(R.string.workout_summary_volume_format, formatVolume(uiState.totalVolumeKg)),
            previousValue = uiState.comparison?.let {
                stringResource(R.string.workout_summary_volume_format, formatVolume(it.previousVolumeKg))
            },
        )
        StatRow(
            label = stringResource(R.string.workout_summary_set_count_label),
            value = uiState.setCount.toString(),
            previousValue = uiState.comparison?.previousSetCount?.toString(),
        )

        if (uiState.newPrExerciseNames.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.workout_summary_new_pr_title), style = MaterialTheme.typography.titleMedium)
                    uiState.newPrExerciseNames.forEach { name ->
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_summary_done))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, previousValue: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.headlineSmall)
        previousValue?.let {
            Text(
                stringResource(R.string.workout_summary_previous_label, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

private fun durationText(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatVolume(kg: Double): String = if (kg == kg.toLong().toDouble()) "${kg.toLong()}" else "%.1f".format(kg)
