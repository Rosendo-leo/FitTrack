package com.fittrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.ui.navigation.FitTrackNavHost
import com.fittrack.app.ui.theme.FitTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by userPreferencesRepository.preferences
                .collectAsState(initial = UserPreferences())
            FitTrackTheme(themeMode = prefs.themeMode) {
                FitTrackNavHost()
            }
        }
    }
}
