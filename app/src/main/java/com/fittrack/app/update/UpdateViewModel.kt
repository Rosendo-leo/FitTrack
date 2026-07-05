package com.fittrack.app.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Hidden : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateUiState
    data class ReadyToInstall(val info: UpdateInfo, val file: File) : UpdateUiState
    data class NeedsInstallPermission(val info: UpdateInfo, val file: File) : UpdateUiState
    data class Failed(val info: UpdateInfo, val message: String) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checker: UpdateChecker,
    private val downloader: UpdateDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Hidden)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        // Esquemática: checa a GitHub API na abertura do app; silencioso se atualizado
        viewModelScope.launch {
            checker.check()?.let { _uiState.value = UpdateUiState.Available(it) }
        }
    }

    fun startDownload() {
        val info = ((_uiState.value as? UpdateUiState.Available)
            ?: (_uiState.value as? UpdateUiState.Failed)?.let { UpdateUiState.Available(it.info) })
            ?.info ?: return
        viewModelScope.launch {
            downloader.download(info).collect { state ->
                _uiState.value = when (state) {
                    is DownloadState.Progress -> UpdateUiState.Downloading(info, state.percent)
                    is DownloadState.Done -> {
                        if (downloader.canInstallPackages()) {
                            UpdateUiState.ReadyToInstall(info, state.file)
                        } else {
                            UpdateUiState.NeedsInstallPermission(info, state.file)
                        }
                    }
                    is DownloadState.Error -> UpdateUiState.Failed(info, state.message)
                }
            }
        }
    }

    /** Chamado ao voltar da tela de permissão de fontes desconhecidas. */
    fun onInstallPermissionResult() {
        val state = _uiState.value
        if (state is UpdateUiState.NeedsInstallPermission && downloader.canInstallPackages()) {
            _uiState.value = UpdateUiState.ReadyToInstall(state.info, state.file)
        }
    }

    fun dismiss() {
        _uiState.value = UpdateUiState.Hidden
    }

    fun installPermissionIntent() = downloader.requestInstallPermissionIntent()
    fun installIntent(file: File) = downloader.installIntent(file)
}
