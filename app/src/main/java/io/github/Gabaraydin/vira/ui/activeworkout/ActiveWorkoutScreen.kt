package io.github.Gabaraydin.vira.ui.activeworkout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.service.resttimer.RestTimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutRoute(
    workoutId: Long,
    onAddExercise: (Long) -> Unit,
    onFinished: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val restTimerState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showFinishConfirm by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(uiState.keepScreenOn) {
        view.keepScreenOn = uiState.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val onFinish: () -> Unit = {
        if (uiState.hasIncompleteSets) {
            showFinishConfirm = true
        } else {
            scope.launch { viewModel.finishSession(); onFinished() }
        }
    }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        ActiveWorkoutScreen(
            uiState = uiState,
            restTimerState = restTimerState,
            onAddSet = viewModel::addSet,
            onDeleteSet = viewModel::deleteSet,
            onUpdateWeight = viewModel::updateWeight,
            onUpdateReps = viewModel::updateReps,
            onUpdateRpe = viewModel::updateRpe,
            onToggleWarmup = viewModel::toggleWarmup,
            onToggleCompleted = viewModel::toggleCompleted,
            onAddExercise = { onAddExercise(workoutId) },
            onFinish = onFinish,
            onSkipRest = viewModel::skipRest,
            onAdjustRest = viewModel::adjustRest,
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text(stringResource(R.string.active_workout_finish_confirm_title)) },
            text = { Text(stringResource(R.string.active_workout_finish_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirm = false
                    scope.launch { viewModel.finishSession(); onFinished() }
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    restTimerState: RestTimerState?,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (ActiveSetUiModel) -> Unit,
    onUpdateWeight: (ActiveSetUiModel, Double) -> Unit,
    onUpdateReps: (ActiveSetUiModel, Int) -> Unit,
    onUpdateRpe: (ActiveSetUiModel, Double?) -> Unit,
    onToggleWarmup: (ActiveSetUiModel) -> Unit,
    onToggleCompleted: (ActiveSetUiModel) -> Unit,
    onAddExercise: () -> Unit,
    onFinish: () -> Unit,
    onSkipRest: () -> Unit,
    onAdjustRest: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(uiState.dayName, style = MaterialTheme.typography.headlineMedium)
            ElapsedTime(uiState.startedAt)
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.exercises, key = { it.exerciseId }) { exercise ->
                ExerciseSection(
                    exercise, uiState.rpeEnabled, onAddSet, onDeleteSet,
                    onUpdateWeight, onUpdateReps, onUpdateRpe, onToggleWarmup, onToggleCompleted,
                )
            }
        }

        if (restTimerState != null) {
            Spacer(Modifier.height(8.dp))
            RestTimerBar(restTimerState, onSkipRest, onAdjustRest)
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.active_workout_add_exercise))
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.active_workout_finish))
        }
    }
}

@Composable
private fun RestTimerBar(state: RestTimerState, onSkip: () -> Unit, onAdjust: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    stringResource(R.string.rest_timer_bar_label, state.exerciseName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        R.string.rest_timer_remaining_format,
                        state.remainingSeconds / 60,
                        state.remainingSeconds % 60,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onAdjust(-15) }) { Text(stringResource(R.string.rest_timer_action_minus_15)) }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.rest_timer_action_skip)) }
                TextButton(onClick = { onAdjust(15) }) { Text(stringResource(R.string.rest_timer_action_plus_15)) }
            }
        }
    }
}

@Composable
private fun ElapsedTime(startedAt: Long) {
    var elapsedSeconds by remember { mutableLongStateOf((System.currentTimeMillis() - startedAt) / 1000) }
    LaunchedEffect(startedAt) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000
            delay(1000)
        }
    }
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    Text(stringResource(R.string.active_workout_elapsed_format, minutes, seconds))
}

@Composable
private fun ExerciseSection(
    exercise: ActiveExerciseUiModel,
    rpeEnabled: Boolean,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (ActiveSetUiModel) -> Unit,
    onUpdateWeight: (ActiveSetUiModel, Double) -> Unit,
    onUpdateReps: (ActiveSetUiModel, Int) -> Unit,
    onUpdateRpe: (ActiveSetUiModel, Double?) -> Unit,
    onToggleWarmup: (ActiveSetUiModel) -> Unit,
    onToggleCompleted: (ActiveSetUiModel) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                exercise.supersetLabel?.let {
                    Text(it, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(exercise.exerciseName, style = MaterialTheme.typography.headlineMedium)
            }

            val previousSetFormat = stringResource(R.string.active_workout_previous_set_format)
            Text(
                if (exercise.previousSets.isEmpty()) {
                    stringResource(R.string.active_workout_previous_empty)
                } else {
                    exercise.previousSets.joinToString(", ") {
                        String.format(previousSetFormat, formatWeight(it.weightKg), it.reps)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(8.dp))

            exercise.sets.forEach { set ->
                SetRow(set, rpeEnabled, onDeleteSet, onUpdateWeight, onUpdateReps, onUpdateRpe, onToggleWarmup, onToggleCompleted)
            }

            TextButton(onClick = { onAddSet(exercise.exerciseId) }) {
                Text(stringResource(R.string.active_workout_add_set))
            }
        }
    }
}

@Composable
private fun SetRow(
    set: ActiveSetUiModel,
    rpeEnabled: Boolean,
    onDeleteSet: (ActiveSetUiModel) -> Unit,
    onUpdateWeight: (ActiveSetUiModel, Double) -> Unit,
    onUpdateReps: (ActiveSetUiModel, Int) -> Unit,
    onUpdateRpe: (ActiveSetUiModel, Double?) -> Unit,
    onToggleWarmup: (ActiveSetUiModel) -> Unit,
    onToggleCompleted: (ActiveSetUiModel) -> Unit,
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
        if (rpeEnabled) {
            DecimalField(
                label = stringResource(R.string.active_workout_rpe_label),
                value = set.rpe,
                onCommit = { onUpdateRpe(set, it) },
                modifier = Modifier.width(70.dp),
            )
        }
        Checkbox(checked = set.isWarmup, onCheckedChange = { onToggleWarmup(set) })
        Checkbox(checked = set.isCompleted, onCheckedChange = { onToggleCompleted(set) })
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

private fun formatWeight(kg: Double): String = if (kg == kg.toLong().toDouble()) "${kg.toLong()}" else kg.toString()
