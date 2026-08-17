package io.github.Gabaraydin.vira.ui.workoutdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.data.repository.WorkoutRepository
import io.github.Gabaraydin.vira.domain.calculations.SetForVolume
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
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    // A finished workout is edited rarely and by one user at a time, so a plain reload
    // after each mutation is simpler than threading this screen onto the reactive
    // Flow-combine pattern the active session uses.
    private suspend fun reload() {
        val workout = requireNotNull(workoutRepository.getById(workoutId)) { "No workout with id $workoutId" }
        val finishedAt = requireNotNull(workout.finishedAt) { "Workout $workoutId has not finished yet" }
        val sets = workoutRepository.getSetsForWorkout(workoutId)
        val exercisesById = exerciseRepository.observeAll().first().associateBy { it.id }
        val positionByExercise = sets.groupBy { it.exerciseId }.mapValues { (_, s) -> s.minOf { it.position } }

        val exerciseModels = sets.groupBy { it.exerciseId }
            .map { (exerciseId, exerciseSets) ->
                WorkoutDetailExerciseUiModel(
                    exerciseId = exerciseId,
                    exerciseName = exercisesById[exerciseId]?.displayName() ?: "?",
                    sets = exerciseSets.sortedBy { it.setIndex }.map { it.toUiModel() },
                )
            }
            .sortedBy { positionByExercise.getValue(it.exerciseId) }

        _uiState.value = WorkoutDetailUiState(
            isLoading = false,
            dayName = workout.dayNameSnapshot,
            date = workout.date.toString(),
            durationSec = (finishedAt - workout.startedAt) / 1000,
            totalVolumeKg = totalVolume(sets.map { it.toSetForVolume() }),
            note = workout.note.orEmpty(),
            exercises = exerciseModels,
        )
    }

    fun updateNote(note: String) {
        viewModelScope.launch {
            workoutRepository.updateNote(workoutId, note.ifBlank { null })
            reload()
        }
    }

    fun updateWeight(model: WorkoutDetailSetUiModel, weightKg: Double) = update(model) { it.copy(weightKg = weightKg) }

    fun updateReps(model: WorkoutDetailSetUiModel, reps: Int) = update(model) { it.copy(reps = reps) }

    fun toggleWarmup(model: WorkoutDetailSetUiModel) = update(model) { it.copy(isWarmup = !it.isWarmup) }

    fun deleteSet(model: WorkoutDetailSetUiModel) {
        viewModelScope.launch {
            val current = workoutRepository.getSetsForWorkout(workoutId).first { it.id == model.id }
            workoutRepository.deleteSet(current)
            reload()
        }
    }

    suspend fun deleteWorkout() {
        val workout = requireNotNull(workoutRepository.getById(workoutId)) { "No workout with id $workoutId" }
        workoutRepository.discardSession(workout)
    }

    private fun update(model: WorkoutDetailSetUiModel, transform: (WorkoutSet) -> WorkoutSet) {
        viewModelScope.launch {
            val current = workoutRepository.getSetsForWorkout(workoutId).first { it.id == model.id }
            workoutRepository.updateSet(transform(current))
            reload()
        }
    }
}

private fun WorkoutSet.toUiModel() = WorkoutDetailSetUiModel(
    id = id,
    exerciseId = exerciseId,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    isWarmup = isWarmup,
)

private fun WorkoutSet.toSetForVolume() = SetForVolume(weightKg, reps, isWarmup, isCompleted)
