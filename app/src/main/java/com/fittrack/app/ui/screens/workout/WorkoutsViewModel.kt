package com.fittrack.app.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.WorkoutTemplate
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val repository: WorkoutRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

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
}
