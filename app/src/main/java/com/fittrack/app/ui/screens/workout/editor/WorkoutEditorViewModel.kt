package com.fittrack.app.ui.screens.workout.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal
import com.fittrack.app.data.local.entities.WorkoutTemplate
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exercício em edição; id == null significa ainda não persistido. */
data class EditorExercise(
    val localKey: Long,
    val id: Long? = null,
    val name: String,
    val muscleGroup: String,
    val notes: String? = null
)

data class EditorUiState(
    val templateId: Long? = null,
    val name: String = "",
    val description: String = "",
    val category: WorkoutCategory = WorkoutCategory.CUSTOM,
    val goal: WorkoutGoal = WorkoutGoal.GENERAL,
    val exercises: List<EditorExercise> = emptyList(),
    val loading: Boolean = true,
    val saved: Boolean = false
) {
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val widgetUpdater: WidgetUpdater,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val argTemplateId: Long = savedStateHandle.get<Long>("templateId") ?: -1L

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var nextLocalKey = 1L
    private val removedExerciseIds = mutableListOf<Long>()

    init {
        viewModelScope.launch {
            if (argTemplateId > 0) {
                val template = repository.getTemplate(argTemplateId)
                val exercises = repository.getExercises(argTemplateId)
                if (template != null) {
                    _uiState.update {
                        EditorUiState(
                            templateId = template.id,
                            name = template.name,
                            description = template.description.orEmpty(),
                            category = template.category,
                            goal = template.goal,
                            exercises = exercises.map { ex ->
                                EditorExercise(
                                    localKey = nextLocalKey++,
                                    id = ex.id,
                                    name = ex.name,
                                    muscleGroup = ex.muscleGroup,
                                    notes = ex.notes
                                )
                            },
                            loading = false
                        )
                    }
                    return@launch
                }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun setCategory(value: WorkoutCategory) = _uiState.update { it.copy(category = value) }
    fun setGoal(value: WorkoutGoal) = _uiState.update { it.copy(goal = value) }

    fun addExercise(name: String, muscleGroup: String, notes: String?) {
        _uiState.update {
            it.copy(
                exercises = it.exercises + EditorExercise(
                    localKey = nextLocalKey++,
                    name = name.trim(),
                    muscleGroup = muscleGroup.trim(),
                    notes = notes?.trim()?.ifBlank { null }
                )
            )
        }
    }

    fun updateExercise(localKey: Long, name: String, muscleGroup: String, notes: String?) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.map { ex ->
                if (ex.localKey == localKey) {
                    ex.copy(
                        name = name.trim(),
                        muscleGroup = muscleGroup.trim(),
                        notes = notes?.trim()?.ifBlank { null }
                    )
                } else ex
            })
        }
    }

    fun removeExercise(localKey: Long) {
        _uiState.update { state ->
            val target = state.exercises.firstOrNull { it.localKey == localKey }
            target?.id?.let { removedExerciseIds += it }
            state.copy(exercises = state.exercises.filterNot { it.localKey == localKey })
        }
    }

    fun moveExercise(localKey: Long, delta: Int) {
        _uiState.update { state ->
            val list = state.exercises.toMutableList()
            val from = list.indexOfFirst { it.localKey == localKey }
            val to = from + delta
            if (from == -1 || to !in list.indices) return@update state
            val item = list.removeAt(from)
            list.add(to, item)
            state.copy(exercises = list)
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val existingId = state.templateId
            // Update em vez de insert(REPLACE) para não disparar o CASCADE nos exercícios.
            val templateId = if (existingId != null) {
                val createdAt = repository.getTemplate(existingId)?.createdAt
                    ?: System.currentTimeMillis()
                repository.updateTemplate(
                    WorkoutTemplate(
                        id = existingId,
                        name = state.name.trim(),
                        description = state.description.trim().ifBlank { null },
                        category = state.category,
                        goal = state.goal,
                        isPreset = false,
                        createdAt = createdAt
                    )
                )
                existingId
            } else {
                repository.saveTemplate(
                    WorkoutTemplate(
                        name = state.name.trim(),
                        description = state.description.trim().ifBlank { null },
                        category = state.category,
                        goal = state.goal,
                        isPreset = false
                    )
                )
            }
            if (removedExerciseIds.isNotEmpty()) {
                repository.deleteExercisesByIds(removedExerciseIds.toList())
                removedExerciseIds.clear()
            }
            repository.saveExercises(
                state.exercises.mapIndexed { index, ex ->
                    Exercise(
                        id = ex.id ?: 0,
                        templateId = templateId,
                        name = ex.name,
                        muscleGroup = ex.muscleGroup,
                        notes = ex.notes,
                        orderIndex = index
                    )
                }
            )
            widgetUpdater.refreshAll()
            _uiState.update { it.copy(saved = true) }
        }
    }
}
