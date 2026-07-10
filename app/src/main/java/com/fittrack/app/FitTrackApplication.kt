package com.fittrack.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.domain.presets.PresetSeeder
import com.fittrack.app.worker.DriveSyncWorker
import com.fittrack.app.worker.ReminderScheduler
import com.fittrack.app.worker.UpdateCheckWorker
import kotlinx.coroutines.flow.first
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FitTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var presetSeeder: PresetSeeder

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            presetSeeder.seedIfNeeded()
            reminderScheduler.rescheduleAll()
            UpdateCheckWorker.schedule(this@FitTrackApplication)
            if (preferencesRepository.preferences.first().driveSyncEnabled) {
                DriveSyncWorker.schedule(this@FitTrackApplication)
            }
        }
    }
}
