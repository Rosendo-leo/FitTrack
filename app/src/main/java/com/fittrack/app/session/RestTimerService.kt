package com.fittrack.app.session

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.fittrack.app.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Segundos restantes/totais do descanso em curso, ou `null` quando não há descanso ativo. */
data class RestTimerState(val secondsLeft: Int, val totalSeconds: Int)

/**
 * Roda o countdown do descanso como foreground service, para sobreviver caso o
 * sistema mate o processo do app enquanto ele está em segundo plano (tela apagada,
 * usuário trocou de app etc.) — algo que não acontecia quando o timer vivia só no
 * viewModelScope do ActiveSessionViewModel.
 */
@AndroidEntryPoint
class RestTimerService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob())
    private var countdownJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRest(intent.getIntExtra(EXTRA_TOTAL_SECONDS, 90))
            ACTION_SKIP -> stopRest()
        }
        return START_NOT_STICKY
    }

    private fun startRest(totalSeconds: Int) {
        countdownJob?.cancel()
        notificationHelper.cancelRestFinished()
        startForeground(NotificationHelper.ID_REST_TIMER, notificationHelper.buildRestTimerNotification(totalSeconds, totalSeconds))
        countdownJob = scope.launch {
            _state.value = RestTimerState(totalSeconds, totalSeconds)
            for (remaining in totalSeconds downTo 1) {
                _state.value = RestTimerState(remaining, totalSeconds)
                notificationHelper.showRestTimer(remaining, totalSeconds)
                delay(1_000)
            }
            _state.value = null
            notificationHelper.cancelRestTimer()
            notificationHelper.showRestFinished()
            notificationHelper.vibrate()
            stopSelf()
        }
    }

    private fun stopRest() {
        countdownJob?.cancel()
        _state.value = null
        notificationHelper.cancelRestTimer()
        stopSelf()
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        scope.cancel()
        _state.value = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.fittrack.app.action.START_REST"
        private const val ACTION_SKIP = "com.fittrack.app.action.SKIP_REST"
        private const val EXTRA_TOTAL_SECONDS = "totalSeconds"

        private val _state = MutableStateFlow<RestTimerState?>(null)
        val state: StateFlow<RestTimerState?> = _state.asStateFlow()

        fun start(context: Context, totalSeconds: Int) {
            val intent = Intent(context, RestTimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
            context.startForegroundService(intent)
        }

        fun skip(context: Context) {
            context.startService(Intent(context, RestTimerService::class.java).setAction(ACTION_SKIP))
        }
    }
}
