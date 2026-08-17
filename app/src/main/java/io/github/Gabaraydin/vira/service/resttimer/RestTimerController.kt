package io.github.Gabaraydin.vira.service.resttimer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Thin wrapper ViewModels use instead of talking to Context/Intent directly. ACTION_START
// goes through startForegroundService because the service may not be running yet; skip/adjust
// only make sense while it already is, so a plain startService is enough for those.
@Singleton
class RestTimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    stateHolder: RestTimerStateHolder,
) {
    val state: StateFlow<RestTimerState?> = stateHolder.state

    fun start(seconds: Int, exerciseName: String) {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_SECONDS, seconds)
            putExtra(RestTimerService.EXTRA_EXERCISE_NAME, exerciseName)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun skip() {
        context.startService(Intent(context, RestTimerService::class.java).setAction(RestTimerService.ACTION_SKIP))
    }

    fun adjust(deltaSeconds: Int) {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_ADJUST
            putExtra(RestTimerService.EXTRA_DELTA_SECONDS, deltaSeconds)
        }
        context.startService(intent)
    }
}
