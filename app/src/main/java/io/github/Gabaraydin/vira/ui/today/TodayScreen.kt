package io.github.Gabaraydin.vira.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.cycle.DayCycleStatus
import io.github.Gabaraydin.vira.domain.model.ProgramTemplate
import kotlinx.coroutines.launch

@Composable
fun TodayRoute(
    onNavigateToActiveWorkout: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val adHocDayName = stringResource(R.string.today_ad_hoc_day_name)
    val templateNames = templateDisplayNames()

    TodayScreen(
        uiState = uiState,
        templateNames = templateNames,
        onStartWorkout = {
            scope.launch { onNavigateToActiveWorkout(viewModel.startWorkout()) }
        },
        onMarkRestDayDone = viewModel::markRestDayDone,
        onStartAdHoc = {
            scope.launch { onNavigateToActiveWorkout(viewModel.startAdHocWorkout(adHocDayName)) }
        },
        onResumeSession = { uiState.unfinishedSessionId?.let(onNavigateToActiveWorkout) },
        onCreateProgram = viewModel::createProgram,
    )
}

@Composable
private fun templateDisplayNames(): Map<ProgramTemplate, String> = mapOf(
    ProgramTemplate.FIVE_DAY_SPLIT to stringResource(R.string.template_five_day_split),
    ProgramTemplate.PUSH_PULL_LEGS to stringResource(R.string.template_push_pull_legs),
    ProgramTemplate.PUSH_PULL_LEGS_DOUBLE to stringResource(R.string.template_push_pull_legs_double),
    ProgramTemplate.UPPER_LOWER to stringResource(R.string.template_upper_lower),
    ProgramTemplate.FULL_BODY_THREE_DAY to stringResource(R.string.template_full_body_three_day),
    ProgramTemplate.BRO_SPLIT to stringResource(R.string.template_bro_split),
)

@Composable
private fun TodayScreen(
    uiState: TodayUiState,
    templateNames: Map<ProgramTemplate, String>,
    onStartWorkout: () -> Unit,
    onMarkRestDayDone: () -> Unit,
    onStartAdHoc: () -> Unit,
    onResumeSession: () -> Unit,
    onCreateProgram: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            !uiState.hasActiveProgram -> EmptyStateTemplatePicker(templateNames, onCreateProgram)
            else -> TodayContent(uiState, onStartWorkout, onMarkRestDayDone, onStartAdHoc, onResumeSession)
        }
    }
}

@Composable
private fun EmptyStateTemplatePicker(
    templateNames: Map<ProgramTemplate, String>,
    onCreateProgram: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.today_empty_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.today_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))
        ProgramTemplate.entries.forEach { template ->
            val name = templateNames.getValue(template)
            OutlinedButton(
                onClick = { onCreateProgram(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) { Text(name) }
        }
        Spacer(Modifier.height(8.dp))
        val buildOwnLabel = stringResource(R.string.today_template_build_own)
        Button(
            onClick = { onCreateProgram(buildOwnLabel) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(buildOwnLabel) }
    }
}

@Composable
private fun TodayContent(
    uiState: TodayUiState,
    onStartWorkout: () -> Unit,
    onMarkRestDayDone: () -> Unit,
    onStartAdHoc: () -> Unit,
    onResumeSession: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (uiState.hasUnfinishedSession) {
            ResumeBanner(onResumeSession)
            Spacer(Modifier.height(16.dp))
        }

        CycleStrip(uiState.cycleDays)
        Spacer(Modifier.height(24.dp))

        uiState.nextDay?.let { day ->
            NextDayCard(
                dayName = day.name,
                position = day.position,
                totalDays = day.totalDays,
                isRest = day.isRest,
                plannedExerciseCount = day.plannedExerciseCount,
                lastWorkoutDayName = uiState.lastWorkoutDayName,
                daysSinceLastWorkout = uiState.daysSinceLastWorkout,
            )
            Spacer(Modifier.height(24.dp))

            if (day.isRest) {
                Button(onClick = onMarkRestDayDone, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.today_action_mark_rest_day))
                }
            } else {
                Button(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.today_action_start_workout))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onStartAdHoc, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.today_action_ad_hoc))
            }
        }
    }
}

@Composable
private fun ResumeBanner(onResumeSession: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.today_resume_banner_text))
            TextButton(onClick = onResumeSession) {
                Text(stringResource(R.string.today_resume_banner_action))
            }
        }
    }
}

@Composable
private fun CycleStrip(days: List<CycleDayUiModel>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(days, key = { it.position }) { day -> CycleDayCell(day) }
    }
}

@Composable
private fun CycleDayCell(day: CycleDayUiModel) {
    val color = when (day.status) {
        DayCycleStatus.DONE -> Color(0xFF4CAF50)
        DayCycleStatus.NEXT -> MaterialTheme.colorScheme.primary
        DayCycleStatus.SKIPPED -> MaterialTheme.colorScheme.error
        DayCycleStatus.UPCOMING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (day.status == DayCycleStatus.NEXT) color else Color.Transparent,
        modifier = Modifier.width(64.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.height(4.dp))
            Text(day.dayNumber.toString(), style = MaterialTheme.typography.bodyLarge)
            Text(
                day.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NextDayCard(
    dayName: String,
    position: Int,
    totalDays: Int,
    isRest: Boolean,
    plannedExerciseCount: Int,
    lastWorkoutDayName: String?,
    daysSinceLastWorkout: Int?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(dayName, style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.today_next_day_position, position + 1, totalDays),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!isRest) {
                Text(
                    stringResource(R.string.today_planned_exercise_count, plannedExerciseCount),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.height(8.dp))
            val streakText = when {
                lastWorkoutDayName != null -> stringResource(R.string.today_streak_did_yesterday, lastWorkoutDayName)
                daysSinceLastWorkout == null -> stringResource(R.string.today_streak_never_logged)
                daysSinceLastWorkout == 0 -> null
                else -> stringResource(R.string.today_streak_gap_days, daysSinceLastWorkout)
            }
            streakText?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}
