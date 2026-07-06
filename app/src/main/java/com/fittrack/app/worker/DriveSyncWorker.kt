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
import com.fittrack.app.data.backup.DriveAuthManager
import com.fittrack.app.data.backup.DriveBackupRepository
import com.fittrack.app.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val WORK_DRIVE_SYNC = "drive_sync"

/** Backup diário automático no Google Drive (quando habilitado e com conta conectada). */
@HiltWorker
class DriveSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: UserPreferencesRepository,
    private val authManager: DriveAuthManager,
    private val driveBackupRepository: DriveBackupRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.driveSyncEnabled) return Result.success()
        if (authManager.currentAccount() == null) return Result.success()

        return try {
            driveBackupRepository.backupNow()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_DRIVE_SYNC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_DRIVE_SYNC)
        }
    }
}
