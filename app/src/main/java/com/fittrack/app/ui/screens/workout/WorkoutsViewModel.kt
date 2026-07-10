package com.fittrack.app.ui.screens.workout

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.WorkoutTemplate
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.data.share.SharedWorkoutFormatException
import com.fittrack.app.data.share.SharedWorkoutTooLargeException
import com.fittrack.app.data.share.WorkoutShareManager
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutsUiState(
    val myTemplates: List<WorkoutTemplate> = emptyList(),
    val presets: List<WorkoutTemplate> = emptyList()
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WorkoutRepository,
    private val shareManager: WorkoutShareManager,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    val uiState: StateFlow<WorkoutsUiState> = combine(
        repository.observeMyTemplates(),
        repository.observePresetTemplates()
    ) { mine, presets ->
        WorkoutsUiState(myTemplates = mine, presets = presets)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutsUiState()
    )

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
            widgetUpdater.refreshAll()
        }
    }

    fun copyPresetToMine(template: WorkoutTemplate, onCopied: (Long) -> Unit = {}) {
        viewModelScope.launch {
            repository.duplicateAsMine(template.id)?.let(onCopied)
        }
    }

    /** Inicia (ou retoma, se já houver uma ativa) uma sessão e devolve o id dela. */
    fun startSession(templateId: Long, onStarted: (sessionId: Long) -> Unit) {
        viewModelScope.launch {
            val active = repository.getActiveSession()
            val sessionId = active?.id ?: repository.startSession(templateId)
            widgetUpdater.refreshAll()
            onStarted(sessionId)
        }
    }

    /** Escreve o treino num arquivo temporário e devolve o Intent de compartilhamento. */
    fun shareTemplate(templateId: Long, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val workout = shareManager.collectSharedWorkout(templateId)
            if (workout == null) {
                _message.value = "Treino não encontrado."
                return@launch
            }
            val file = shareManager.writeToCacheFile(workout)
            onReady(shareManager.shareIntent(file))
        }
    }

    /** Gera um QR code com o treino codificado, para mostrar na tela (sem precisar de arquivo). */
    fun shareTemplateAsQr(templateId: Long, onReady: (Bitmap) -> Unit) {
        viewModelScope.launch {
            val workout = shareManager.collectSharedWorkout(templateId)
            if (workout == null) {
                _message.value = "Treino não encontrado."
                return@launch
            }
            try {
                onReady(shareManager.generateQrBitmap(workout))
            } catch (e: SharedWorkoutTooLargeException) {
                _message.value = e.message
            }
        }
    }

    /** Lê um treino de [uri] e o adiciona a "Meus treinos". */
    fun importFromUri(uri: Uri, onImported: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Não foi possível ler o arquivo.")
                val data = shareManager.parseSharedWorkout(input)
                val newId = shareManager.importAsNewTemplate(data)
                widgetUpdater.refreshAll()
                _message.value = "Treino \"${data.template.name}\" importado ✅"
                onImported(newId)
            } catch (e: SharedWorkoutFormatException) {
                _message.value = e.message
            } catch (e: Exception) {
                _message.value = "Erro ao importar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }
}
