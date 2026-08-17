package io.github.Gabaraydin.vira.ui.today

import io.github.Gabaraydin.vira.domain.cycle.DayCycleStatus
import io.github.Gabaraydin.vira.domain.model.ProgramTemplate
import java.time.LocalDate

data class CycleDayUiModel(
    val position: Int,
    val dayNumber: Int,
    val name: String,
    val isRest: Boolean,
    val status: DayCycleStatus,
    val doneDate: LocalDate?,
)

data class NextDayInfo(
    val programDayId: Long,
    val name: String,
    val position: Int,
    val totalDays: Int,
    val isRest: Boolean,
    val plannedExerciseCount: Int,
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val programName: String? = null,
    val cycleDays: List<CycleDayUiModel> = emptyList(),
    val nextDay: NextDayInfo? = null,
    // Non-null only when the most recent finished workout was exactly yesterday.
    val lastWorkoutDayName: String? = null,
    // null = never logged a workout; 0 = today; otherwise days since the last one.
    val daysSinceLastWorkout: Int? = null,
    val unfinishedSessionId: Long? = null,
    val availableTemplates: List<ProgramTemplate> = ProgramTemplate.entries,
) {
    val hasActiveProgram: Boolean get() = programName != null
    val hasUnfinishedSession: Boolean get() = unfinishedSessionId != null
}
