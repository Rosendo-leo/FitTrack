package com.fittrack.app.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_WORKOUT_REMINDER = "workout_reminder"
private const val WORK_WEIGHT_REMINDER = "weight_reminder"

/**
 * Agenda os lembretes como OneTimeWork com delay até a próxima ocorrência.
 * Cada worker, ao disparar, reagenda a ocorrência seguinte — padrão que
 * sobrevive a reboot não; o reagendamento também acontece a cada abertura
 * do app (Application.onCreate) e a cada mudança nos Ajustes.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    suspend fun rescheduleAll() {
        val prefs = preferencesRepository.preferences.first()
        scheduleWorkoutReminder(prefs)
        scheduleWeightReminder(prefs)
    }

    fun scheduleWorkoutReminder(prefs: UserPreferences) {
        if (!prefs.workoutReminderEnabled || prefs.workoutReminderDays.isEmpty()) {
            workManager.cancelUniqueWork(WORK_WORKOUT_REMINDER)
            return
        }
        val delay = delayUntilNext(
            time = LocalTime.of(prefs.workoutReminderHour, prefs.workoutReminderMinute),
            allowedIsoDays = prefs.workoutReminderDays
        )
        workManager.enqueueUniqueWork(
            WORK_WORKOUT_REMINDER,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
        )
    }

    fun scheduleWeightReminder(prefs: UserPreferences) {
        if (!prefs.weightReminderEnabled) {
            workManager.cancelUniqueWork(WORK_WEIGHT_REMINDER)
            return
        }
        val delay = delayUntilNext(
            time = LocalTime.of(prefs.weightReminderHour, prefs.weightReminderMinute),
            allowedIsoDays = (1..7).toSet()
        )
        workManager.enqueueUniqueWork(
            WORK_WEIGHT_REMINDER,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WeightReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
        )
    }

    /** Delay até a próxima ocorrência de [time] num dia contido em [allowedIsoDays]. */
    private fun delayUntilNext(time: LocalTime, allowedIsoDays: Set<Int>): Duration {
        val now = LocalDateTime.now()
        for (offset in 0..7L) {
            val candidate = now.toLocalDate().plusDays(offset).atTime(time)
            if (candidate.isAfter(now) && candidate.dayOfWeek.value in allowedIsoDays) {
                return Duration.between(now, candidate)
            }
        }
        return Duration.ofDays(1) // inalcançável com allowedIsoDays não-vazio
    }
}
