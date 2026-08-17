package io.github.Gabaraydin.vira.service.resttimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import io.github.Gabaraydin.vira.MainActivity
import io.github.Gabaraydin.vira.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// Runs as a specialUse foreground service (see the manifest comment) so a long rest — the
// global default plus a per-exercise override, adjustable +/-15s while counting down — is
// never cut off by a system-imposed timeout the way FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
// would. Owns the countdown itself so it keeps running when the Active Workout screen is
// backgrounded; the UI only observes RestTimerStateHolder and sends control intents here.
@AndroidEntryPoint
class RestTimerService : Service() {

    @Inject
    lateinit var stateHolder: RestTimerStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var totalSeconds = 0
    private var remainingSeconds = 0
    private var exerciseName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.rest_timer_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.rest_timer_channel_description) }
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_SKIP -> handleSkip()
            ACTION_ADJUST -> handleAdjust(intent.getIntExtra(EXTRA_DELTA_SECONDS, 0))
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        totalSeconds = intent.getIntExtra(EXTRA_SECONDS, 90).coerceAtLeast(1)
        remainingSeconds = totalSeconds
        exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty()
        publishState()
        startForeground(NOTIFICATION_ID, buildNotification())
        restartTicker()
    }

    private fun handleSkip() {
        if (tickJob == null) return
        finish()
    }

    private fun handleAdjust(deltaSeconds: Int) {
        if (tickJob == null) return
        remainingSeconds = (remainingSeconds + deltaSeconds).coerceAtLeast(0)
        totalSeconds = maxOf(totalSeconds, remainingSeconds)
        if (remainingSeconds <= 0) {
            finish()
        } else {
            publishState()
            updateNotification()
        }
    }

    private fun restartTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
                publishState()
                updateNotification()
            }
            finish()
        }
    }

    private fun finish() {
        tickJob?.cancel()
        tickJob = null
        publishState(clear = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishState(clear: Boolean = false) {
        stateHolder.update(if (clear) null else RestTimerState(totalSeconds, remainingSeconds, exerciseName))
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(getString(R.string.rest_timer_notification_title, exerciseName))
            .setContentText(getString(R.string.rest_timer_notification_remaining, minutes, seconds))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.rest_timer_action_minus_15), actionPendingIntent(ACTION_ADJUST, -15))
            .addAction(0, getString(R.string.rest_timer_action_skip), actionPendingIntent(ACTION_SKIP, 0))
            .addAction(0, getString(R.string.rest_timer_action_plus_15), actionPendingIntent(ACTION_ADJUST, 15))
            .build()
    }

    private fun actionPendingIntent(action: String, deltaSeconds: Int): PendingIntent {
        val intent = Intent(this, RestTimerService::class.java).apply {
            this.action = action
            if (action == ACTION_ADJUST) putExtra(EXTRA_DELTA_SECONDS, deltaSeconds)
        }
        val requestCode = when (action) {
            ACTION_SKIP -> 1
            else -> if (deltaSeconds > 0) 2 else 3
        }
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "io.github.Gabaraydin.vira.action.REST_TIMER_START"
        const val ACTION_SKIP = "io.github.Gabaraydin.vira.action.REST_TIMER_SKIP"
        const val ACTION_ADJUST = "io.github.Gabaraydin.vira.action.REST_TIMER_ADJUST"
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_EXERCISE_NAME = "exerciseName"
        const val EXTRA_DELTA_SECONDS = "deltaSeconds"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "rest_timer"
    }
}
