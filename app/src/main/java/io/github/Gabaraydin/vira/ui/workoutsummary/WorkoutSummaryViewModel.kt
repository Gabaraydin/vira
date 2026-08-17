package io.github.Gabaraydin.vira.ui.workoutsummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.data.repository.WorkoutRepository
import io.github.Gabaraydin.vira.domain.calculations.SetForPr
import io.github.Gabaraydin.vira.domain.calculations.SetForVolume
import io.github.Gabaraydin.vira.domain.calculations.isNewPersonalRecord
import io.github.Gabaraydin.vira.domain.calculations.totalVolume
import io.github.Gabaraydin.vira.domain.model.WorkoutSet
import io.github.Gabaraydin.vira.domain.model.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val workout = requireNotNull(workoutRepository.getById(workoutId)) { "No workout with id $workoutId" }
        val finishedAt = requireNotNull(workout.finishedAt) { "Workout $workoutId has not finished yet" }
        val sets = workoutRepository.getSetsForWorkout(workoutId)
        val exercisesById = exerciseRepository.observeAll().first().associateBy { it.id }

        val newPrExerciseNames = sets.groupBy { it.exerciseId }.mapNotNull { (exerciseId, exerciseSets) ->
            val prior = workoutRepository.getPriorCompletedSets(exerciseId, excludeWorkoutId = workoutId)
            val isPr = isNewPersonalRecord(exerciseSets.map { it.toSetForPr() }, prior.map { it.toSetForPr() })
            exercisesById[exerciseId]?.displayName().takeIf { isPr }
        }

        val comparison = workout.programDayId?.let { dayId ->
            workoutRepository.getPreviousWorkoutForDay(dayId, excludeWorkoutId = workoutId)?.let { previous ->
                val previousSets = workoutRepository.getSetsForWorkout(previous.id)
                PreviousCycleComparison(
                    previousVolumeKg = totalVolume(previousSets.map { it.toSetForVolume() }),
                    previousDurationSec = ((previous.finishedAt ?: previous.startedAt) - previous.startedAt) / 1000,
                    previousSetCount = previousSets.count { it.isCompleted && !it.isWarmup },
                )
            }
        }

        _uiState.value = WorkoutSummaryUiState(
            isLoading = false,
            dayName = workout.dayNameSnapshot,
            durationSec = (finishedAt - workout.startedAt) / 1000,
            totalVolumeKg = totalVolume(sets.map { it.toSetForVolume() }),
            setCount = sets.count { it.isCompleted && !it.isWarmup },
            newPrExerciseNames = newPrExerciseNames,
            comparison = comparison,
        )
    }
}

private fun WorkoutSet.toSetForVolume() = SetForVolume(weightKg, reps, isWarmup, isCompleted)
private fun WorkoutSet.toSetForPr() = SetForPr(weightKg, reps, isWarmup, isCompleted)
