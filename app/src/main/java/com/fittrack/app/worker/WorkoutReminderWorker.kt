package com.fittrack.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class WorkoutReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper,
    private val scheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        if (prefs.workoutReminderEnabled &&
            LocalDate.now().dayOfWeek.value in prefs.workoutReminderDays
        ) {
            notificationHelper.showWorkoutReminder()
        }
        scheduler.scheduleWorkoutReminder(prefs)
        return Result.success()
    }
}
