package com.fittrack.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.backup.BackupFormatException
import com.fittrack.app.data.backup.BackupManager
import com.fittrack.app.data.backup.DriveAuthManager
import com.fittrack.app.data.backup.DriveBackupRepository
import com.fittrack.app.data.backup.RestoreMode
import com.fittrack.app.data.backup.RestoreSummary
import com.fittrack.app.data.preferences.DistanceUnit
import com.fittrack.app.data.preferences.ThemeMode
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.data.preferences.WeightUnit
import com.fittrack.app.widget.WidgetUpdater
import com.fittrack.app.worker.DriveSyncWorker
import com.fittrack.app.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado da seção de backup nos Ajustes. */
data class BackupUiState(
    val driveAccountEmail: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    /** Backup lido de um arquivo/Drive aguardando escolha do modo de restore. */
    val pendingRestore: PendingRestore? = null
)

sealed interface PendingRestore {
    data class FromFile(val uri: Uri) : PendingRestore
    data object FromDrive : PendingRestore
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager,
    private val driveAuthManager: DriveAuthManager,
    private val driveBackupRepository: DriveBackupRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    private val _backupState = MutableStateFlow(
        BackupUiState(driveAccountEmail = driveAuthManager.currentAccount()?.email)
    )
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { preferencesRepository.setWeightUnit(unit) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { preferencesRepository.setDistanceUnit(unit) }
    }

    fun setHeightCm(heightCm: Float) {
        viewModelScope.launch { preferencesRepository.setHeightCm(heightCm) }
    }

    fun setWorkoutReminder(enabled: Boolean, days: Set<Int>, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setWorkoutReminder(enabled, days, hour, minute)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setWeightReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setWeightReminder(enabled, hour, minute)
            reminderScheduler.rescheduleAll()
        }
    }

    // ── Backup local (SAF) ──

    fun exportToUri(uri: Uri) {
        runBackupOp("Backup exportado ✅") {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                backupManager.exportToZip(output)
            } ?: error("Não foi possível abrir o arquivo de destino.")
            null
        }
    }

    /** Guarda o arquivo escolhido e abre o diálogo Substituir/Mesclar/Cancelar. */
    fun requestImport(uri: Uri) {
        _backupState.value = _backupState.value.copy(pendingRestore = PendingRestore.FromFile(uri))
    }

    fun requestDriveRestore() {
        _backupState.value = _backupState.value.copy(pendingRestore = PendingRestore.FromDrive)
    }

    fun cancelRestore() {
        _backupState.value = _backupState.value.copy(pendingRestore = null)
    }

    fun confirmRestore(mode: RestoreMode) {
        val pending = _backupState.value.pendingRestore ?: return
        _backupState.value = _backupState.value.copy(pendingRestore = null)
        runBackupOp(null) {
            val summary: RestoreSummary? = when (pending) {
                is PendingRestore.FromFile ->
                    context.contentResolver.openInputStream(pending.uri)?.use { input ->
                        backupManager.restore(backupManager.parseBackup(input), mode)
                    } ?: error("Não foi possível ler o arquivo.")

                is PendingRestore.FromDrive -> driveBackupRepository.restoreLatest(mode)
            }
            if (summary != null) widgetUpdater.refreshAll()
            if (summary == null) {
                "Nenhum backup encontrado no Drive."
            } else if (mode == RestoreMode.REPLACE) {
                "Backup restaurado ✅"
            } else {
                "Mesclado: +${summary.templates} treinos, +${summary.sessions} sessões, " +
                    "+${summary.bodyMetrics} pesos, +${summary.cardioSessions} cardios ✅"
            }
        }
    }

    // ── Google Drive ──

    fun driveSignInIntent(): Intent = driveAuthManager.signInIntent()

    /** Chamado após o resultado do launcher de sign-in. */
    fun onDriveSignInResult() {
        val email = driveAuthManager.currentAccount()?.email
        _backupState.value = _backupState.value.copy(
            driveAccountEmail = email,
            message = if (email != null) "Conectado como $email" else "Falha ao conectar ao Google."
        )
        if (email != null && preferences.value.driveSyncEnabled) {
            DriveSyncWorker.schedule(context)
        }
    }

    fun driveSignOut() {
        driveAuthManager.signOut()
        DriveSyncWorker.cancel(context)
        _backupState.value = _backupState.value.copy(driveAccountEmail = null, message = "Conta desconectada.")
    }

    fun driveBackupNow() {
        runBackupOp("Backup enviado ao Drive ✅") {
            driveBackupRepository.backupNow()
            null
        }
    }

    fun setDriveSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDriveSyncEnabled(enabled)
            if (enabled) DriveSyncWorker.schedule(context) else DriveSyncWorker.cancel(context)
        }
    }

    fun consumeMessage() {
        _backupState.value = _backupState.value.copy(message = null)
    }

    /** Executa a operação com busy/mensagem de erro padronizados. */
    private fun runBackupOp(successMessage: String?, block: suspend () -> String?) {
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(busy = true)
            val message = try {
                block() ?: successMessage
            } catch (e: BackupFormatException) {
                e.message
            } catch (e: Exception) {
                "Erro: ${e.message ?: e.javaClass.simpleName}"
            }
            _backupState.value = _backupState.value.copy(busy = false, message = message)
        }
    }
}
