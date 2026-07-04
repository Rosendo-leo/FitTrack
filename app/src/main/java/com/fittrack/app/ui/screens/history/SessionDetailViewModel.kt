package com.fittrack.app.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.SetWithExercise
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseSets(val exerciseName: String, val muscleGroup: String, val sets: List<SetWithExercise>)

data class SessionDetailUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val templateName: String = "Treino livre",
    val exerciseGroups: List<ExerciseSets> = emptyList(),
    val deleted: Boolean = false
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: -1L

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session == null) {
                _uiState.update { it.copy(loading = false, deleted = true) }
                return@launch
            }
            val templateName = session.templateId
                ?.let { repository.getTemplate(it)?.name }
                ?: "Treino livre"
            _uiState.update {
                it.copy(loading = false, session = session, templateName = templateName)
            }

            repository.observeSetsWithExercise(sessionId).collect { sets ->
                val groups = sets
                    .groupBy { it.exerciseName to it.muscleGroup }
                    .map { (key, groupSets) ->
                        ExerciseSets(
                            exerciseName = key.first,
                            muscleGroup = key.second,
                            sets = groupSets
                        )
                    }
                _uiState.update { it.copy(exerciseGroups = groups) }
            }
        }
    }

    fun deleteSession() {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
