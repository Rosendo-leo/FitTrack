package com.fittrack.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.ui.common.LocalUserPreferences
import com.fittrack.app.ui.navigation.FitTrackNavHost
import com.fittrack.app.ui.navigation.WidgetAction
import com.fittrack.app.ui.theme.FitTrackTheme
import com.fittrack.app.update.UpdateDialogHost
import com.fittrack.app.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_REGISTER_WEIGHT = "com.fittrack.app.action.REGISTER_WEIGHT"
        const val ACTION_START_WORKOUT = "com.fittrack.app.action.START_WORKOUT"
        const val ACTION_OPEN_ACTIVE_SESSION = "com.fittrack.app.action.OPEN_ACTIVE_SESSION"
        const val EXTRA_TEMPLATE_ID = "templateId"
        const val EXTRA_SESSION_ID = "sessionId"
    }

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    /** Ação pendente disparada por um widget (atalho direto). */
    private var pendingWidgetAction by mutableStateOf<WidgetAction?>(null)

    private fun consumeWidgetIntent(intent: Intent?) {
        pendingWidgetAction = when (intent?.action) {
            ACTION_REGISTER_WEIGHT -> WidgetAction.RegisterWeight
            ACTION_START_WORKOUT -> WidgetAction.StartWorkout(
                intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L).takeIf { it > 0 }
            )
            ACTION_OPEN_ACTIVE_SESSION -> intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                .takeIf { it > 0 }
                ?.let { WidgetAction.OpenActiveSession(it) }
                ?: pendingWidgetAction
            else -> pendingWidgetAction
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeWidgetIntent(intent)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* opcional */ }

    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        // Retoma o fluxo de update se o usuário voltou da tela de permissão de instalação
        updateViewModel.onInstallPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeWidgetIntent(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val prefs by userPreferencesRepository.preferences
                .collectAsState(initial = UserPreferences())
            CompositionLocalProvider(LocalUserPreferences provides prefs) {
                FitTrackTheme(
                    themeMode = prefs.themeMode,
                    dynamicColorEnabled = prefs.dynamicColorEnabled
                ) {
                    FitTrackNavHost(
                        widgetAction = pendingWidgetAction,
                        onWidgetActionHandled = { pendingWidgetAction = null }
                    )
                    UpdateDialogHost(updateViewModel)
                }
            }
        }
    }
}
