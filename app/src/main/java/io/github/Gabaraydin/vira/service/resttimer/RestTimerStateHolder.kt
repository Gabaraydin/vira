package io.github.Gabaraydin.vira.service.resttimer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Single source of truth for the running rest timer, written only by RestTimerService and
// read by both the service's own notification builder and the Active Workout UI — this is
// what lets the persistent bar keep showing the countdown even though the service (not the
// ViewModel) owns the actual ticking, so it survives the screen being backgrounded.
@Singleton
class RestTimerStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<RestTimerState?>(null)
    val state: StateFlow<RestTimerState?> = _state.asStateFlow()

    fun update(state: RestTimerState?) {
        _state.value = state
    }
}
