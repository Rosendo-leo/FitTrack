package com.fittrack.app.ui.screens.active_session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseWithSets(
    val exercise: Exercise,
    val sets: List<SetRecord> = emptyList()
)

data class ActiveSessionUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val templateName: String = "Treino livre",
    val exercises: List<ExerciseWithSets> = emptyList(),
    val restSecondsLeft: Int? = null,
    val restTotalSeconds: Int = 90,
    val finished: Boolean = false
) {
    /** Volume total (kg) das séries válidas, excluindo aquecimento. */
    val totalVolume: Float
        get() = exercises
            .flatMap { it.sets }
            .filterNot { it.isWarmup }
            .fold(0f) { acc, set -> acc + set.weightKg * set.reps }

    val totalSets: Int get() = exercises.sumOf { it.sets.size }
}

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val notificationHelper: NotificationHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: -1L

    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    /** Emite o nome do exercício quando um novo PR é detectado. */
    private val _prEvents = MutableSharedFlow<String>()
    val prEvents: SharedFlow<String> = _prEvents.asSharedFlow()

    private var restJob: Job? = null

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session == null) {
                _uiState.update { it.copy(loading = false, finished = true) }
                return@launch
            }
            val templateId = session.templateId
            val template = templateId?.let { repository.getTemplate(it) }
            val exercises = templateId?.let { repository.getExercises(it) }.orEmpty()

            _uiState.update {
                it.copy(
                    loading = false,
                    session = session,
                    templateName = template?.name ?: "Treino livre",
                    exercises = exercises.map { ex -> ExerciseWithSets(ex) }
                )
            }

            repository.observeSets(sessionId).collect { sets ->
                val bySets = sets.groupBy { set -> set.exerciseId }
                _uiState.update { state ->
                    state.copy(exercises = state.exercises.map { ews ->
                        ews.copy(sets = bySets[ews.exercise.id].orEmpty())
                    })
                }
            }
        }
    }

    fun registerSet(exerciseId: Long, weightKg: Float, reps: Int, isWarmup: Boolean) {
        if (weightKg < 0 || reps <= 0) return
        viewModelScope.launch {
            val isPr = !isWarmup && repository.isPersonalRecord(exerciseId, weightKg)
            val setNumber = _uiState.value.exercises
                .firstOrNull { it.exercise.id == exerciseId }
                ?.sets?.size?.plus(1) ?: 1
            repository.recordSet(
                SetRecord(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setNumber = setNumber,
                    reps = reps,
                    weightKg = weightKg,
                    isWarmup = isWarmup
                )
            )
            if (isPr) {
                _uiState.value.exercises
                    .firstOrNull { it.exercise.id == exerciseId }
                    ?.let { ews ->
                        _prEvents.emit(ews.exercise.name)
                        notificationHelper.showPr(ews.exercise.name, weightKg, reps)
                        notificationHelper.vibrate(300)
                    }
            }
            startRest()
        }
    }

    fun deleteSet(set: SetRecord) {
        viewModelScope.launch { repository.deleteSet(set.id) }
    }

    // ── Timer de descanso ──

    fun adjustRestDuration(deltaSeconds: Int) {
        _uiState.update {
            it.copy(restTotalSeconds = (it.restTotalSeconds + deltaSeconds).coerceIn(15, 600))
        }
    }

    fun startRest() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            val total = _uiState.value.restTotalSeconds
            for (remaining in total downTo 1) {
                _uiState.update { it.copy(restSecondsLeft = remaining) }
                notificationHelper.showRestTimer(remaining, total)
                delay(1_000)
            }
            _uiState.update { it.copy(restSecondsLeft = null) }
            notificationHelper.cancelRestTimer()
            notificationHelper.vibrate()
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _uiState.update { it.copy(restSecondsLeft = null) }
        notificationHelper.cancelRestTimer()
    }

    override fun onCleared() {
        notificationHelper.cancelRestTimer()
        super.onCleared()
    }

    // ── Encerramento ──

    fun finishSession() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            restJob?.cancel()
            notificationHelper.cancelRestTimer()
            repository.finishSession(session, _uiState.value.totalVolume)
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun discardSession() {
        viewModelScope.launch {
            restJob?.cancel()
            notificationHelper.cancelRestTimer()
            repository.deleteSession(sessionId)
            _uiState.update { it.copy(finished = true) }
        }
    }
}
