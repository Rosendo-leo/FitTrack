package com.fittrack.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fittrack.app.R
import com.fittrack.app.update.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_REST_TIMER = "rest_timer"
        const val CHANNEL_REST_DONE = "rest_done"
        const val CHANNEL_ACHIEVEMENTS = "achievements"
        const val CHANNEL_UPDATES = "updates"

        const val ID_WORKOUT_REMINDER = 1001
        const val ID_WEIGHT_REMINDER = 1002
        const val ID_REST_TIMER = 1003
        const val ID_PR = 1004
        const val ID_REST_DONE = 1005
        const val ID_UPDATE_AVAILABLE = 1006
    }

    init {
        ensureChannels()
    }

    private fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Lembretes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Lembretes de treino e de registro de peso" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REST_TIMER,
                "Timer de descanso",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Contagem do descanso durante a sessão"; setSound(null, null) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REST_DONE,
                "Fim do descanso",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerta quando o tempo de descanso termina"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Conquistas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Novos recordes pessoais" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UPDATES,
                "Atualizações",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisa quando uma nova versão do app está disponível" }
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        return PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun builder(channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)

    fun showWorkoutReminder() {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(
            ID_WORKOUT_REMINDER,
            builder(CHANNEL_REMINDERS)
                .setContentTitle("Hora do treino! 💪")
                .setContentText("Seu treino de hoje está esperando. Bora?")
                .build()
        )
    }

    fun showWeightReminder() {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(
            ID_WEIGHT_REMINDER,
            builder(CHANNEL_REMINDERS)
                .setContentTitle("Registrar peso ⚖️")
                .setContentText("Registre seu peso de hoje para acompanhar o progresso.")
                .build()
        )
    }

    fun showPr(exerciseName: String, weightKg: Float, reps: Int) {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(
            ID_PR,
            builder(CHANNEL_ACHIEVEMENTS)
                .setContentTitle("Novo recorde! 🏆")
                .setContentText(
                    "%s: %.1f kg × %d".format(exerciseName, weightKg, reps)
                )
                .build()
        )
    }

    /** Notificação persistente e silenciosa com o countdown do descanso. */
    fun showRestTimer(remainingSeconds: Int, totalSeconds: Int) {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(ID_REST_TIMER, buildRestTimerNotification(remainingSeconds, totalSeconds))
    }

    /** Usado pelo [com.fittrack.app.session.RestTimerService] para iniciar em primeiro plano. */
    fun buildRestTimerNotification(remainingSeconds: Int, totalSeconds: Int) =
        builder(CHANNEL_REST_TIMER)
            .setContentTitle("Descanso ⏱️")
            .setContentText(
                "%02d:%02d restantes".format(remainingSeconds / 60, remainingSeconds % 60)
            )
            .setProgress(totalSeconds, totalSeconds - remainingSeconds, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()

    fun cancelRestTimer() {
        NotificationManagerCompat.from(context).cancel(ID_REST_TIMER)
    }

    /** Heads-up com som/vibração quando o descanso termina (tela apagada inclusive). */
    fun showRestFinished() {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(
            ID_REST_DONE,
            builder(CHANNEL_REST_DONE)
                .setContentTitle("Descanso concluído! 💪")
                .setContentText("Hora da próxima série.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    fun cancelRestFinished() {
        NotificationManagerCompat.from(context).cancel(ID_REST_DONE)
    }

    /** Disparado pelo [com.fittrack.app.worker.UpdateCheckWorker] quando há release novo no GitHub. */
    fun showUpdateAvailable(info: UpdateInfo) {
        if (!canNotify()) return
        NotificationManagerCompat.from(context).notify(
            ID_UPDATE_AVAILABLE,
            builder(CHANNEL_UPDATES)
                .setContentTitle("Atualização disponível 🚀")
                .setContentText("FitTrack ${info.versionName} já pode ser instalado.")
                .build()
        )
    }

    fun vibrate(durationMs: Long = 500) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
