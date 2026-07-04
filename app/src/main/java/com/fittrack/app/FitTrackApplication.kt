package com.fittrack.app

import android.app.Application
import com.fittrack.app.domain.presets.PresetSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FitTrackApplication : Application() {

    @Inject
    lateinit var presetSeeder: PresetSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { presetSeeder.seedIfNeeded() }
    }
}
