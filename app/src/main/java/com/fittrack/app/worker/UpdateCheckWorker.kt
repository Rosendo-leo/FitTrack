package com.fittrack.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fittrack.app.notifications.NotificationHelper
import com.fittrack.app.update.UpdateChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

private const val WORK_UPDATE_CHECK = "update_check"

/**
 * Checa a GitHub API por releases novos mesmo com o app fechado, para que a
 * notificação de update não dependa do usuário abrir o app manualmente.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checker: UpdateChecker,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        checker.check()?.let { notificationHelper.showUpdateAvailable(it) }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_UPDATE_CHECK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
