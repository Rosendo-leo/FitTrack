package com.fittrack.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.ui.navigation.FitTrackNavHost
import com.fittrack.app.ui.theme.FitTrackTheme
import com.fittrack.app.update.UpdateDialogHost
import com.fittrack.app.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* opcional */ }

    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        // Retoma o fluxo de update se o usuário voltou da tela de permissão de instalação
        updateViewModel.onInstallPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val prefs by userPreferencesRepository.preferences
                .collectAsState(initial = UserPreferences())
            FitTrackTheme(themeMode = prefs.themeMode) {
                FitTrackNavHost()
                UpdateDialogHost(updateViewModel)
            }
        }
    }
}
