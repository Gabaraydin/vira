package io.github.Gabaraydin.vira.ui.activeworkout

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.WorkoutRepository
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    suspend fun finishSession(workoutId: Long) {
        workoutRepository.finishSession(workoutId, finishedAt = System.currentTimeMillis())
    }
}
