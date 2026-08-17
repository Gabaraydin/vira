package io.github.Gabaraydin.vira.ui.activeworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.Gabaraydin.vira.R
import kotlinx.coroutines.launch

// Placeholder: the full active-workout experience (exercise list, set logging, rest
// timer, superset labels) is issue #12. This exists so #9's start/resume flow actually
// lands somewhere and a session can be finished end to end.
@Composable
fun ActiveWorkoutRoute(
    workoutId: Long,
    onFinished: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.active_workout_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.active_workout_placeholder_note),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = { scope.launch { viewModel.finishSession(workoutId); onFinished() } }) {
            Text(stringResource(R.string.active_workout_finish))
        }
    }
}
