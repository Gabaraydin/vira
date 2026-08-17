package io.github.Gabaraydin.vira.ui.exercisedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.data.repository.ExerciseSetHistory
import io.github.Gabaraydin.vira.domain.calculations.SetForPr
import io.github.Gabaraydin.vira.domain.calculations.bestOverallEstimatedOneRepMax
import io.github.Gabaraydin.vira.domain.calculations.bestWeightPerRepCount
import io.github.Gabaraydin.vira.domain.charts.ChartPoint
import io.github.Gabaraydin.vira.domain.model.Exercise
import io.github.Gabaraydin.vira.domain.model.WorkoutSet
import io.github.Gabaraydin.vira.domain.model.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    // Set history is reactive (a Flow), but the exercise's own row — rest override,
    // archived flag — is only touched by this screen's own actions, so it's refreshed
    // explicitly after each one rather than kept as a second live subscription.
    private val exerciseFlow = MutableStateFlow<Exercise?>(null)

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        exerciseFlow.filterNotNull(),
        exerciseRepository.observeSetHistory(exerciseId),
    ) { exercise, history -> buildUiState(exercise, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    init {
        viewModelScope.launch { refreshExercise() }
    }

    private suspend fun refreshExercise() {
        exerciseFlow.value = exerciseRepository.getById(exerciseId)
    }

    fun updateRestOverride(seconds: Int?) {
        viewModelScope.launch {
            val exercise = exerciseFlow.value ?: return@launch
            exerciseRepository.updateDefaultRestSec(exercise, seconds)
            refreshExercise()
        }
    }

    suspend fun archive() {
        val exercise = exerciseFlow.value ?: return
        exerciseRepository.archive(exercise)
    }
}

private fun buildUiState(exercise: Exercise, history: List<ExerciseSetHistory>): ExerciseDetailUiState {
    val bySession = history.groupBy { it.set.workoutId }

    val chartPoints = bySession.mapNotNull { (_, entries) ->
        val date = entries.first().date
        val best = bestOverallEstimatedOneRepMax(entries.map { it.set.toSetForPr() })
        best?.let { ChartPoint(x = date.toEpochDay().toDouble(), y = it) }
    }.sortedBy { it.x }

    val allSets = history.map { it.set.toSetForPr() }

    val historyRows = bySession.map { (_, entries) ->
        SessionHistoryRow(
            date = entries.first().date,
            sets = entries
                .filter { it.set.isCompleted }
                .sortedBy { it.set.setIndex }
                .map { SessionSetUiModel(it.set.weightKg, it.set.reps) },
        )
    }.sortedByDescending { it.date }

    return ExerciseDetailUiState(
        isLoading = false,
        name = exercise.displayName(),
        muscle = exercise.primaryMuscle,
        equipment = exercise.equipment,
        chartPoints = chartPoints,
        bestOverallE1rm = bestOverallEstimatedOneRepMax(allSets),
        repPrTable = bestWeightPerRepCount(allSets),
        history = historyRows,
        restOverrideSeconds = exercise.defaultRestSec,
        isArchived = exercise.isArchived,
    )
}

private fun WorkoutSet.toSetForPr() = SetForPr(weightKg, reps, isWarmup, isCompleted)
