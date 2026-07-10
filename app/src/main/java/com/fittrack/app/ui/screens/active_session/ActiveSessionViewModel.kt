package com.fittrack.app.ui.screens.active_session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.notifications.NotificationHelper
import com.fittrack.app.session.RestTimerService
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val sets: List<SetRecord> = emptyList(),
    /** Séries da última sessão finalizada, para sugestão de carga. */
    val lastSets: List<SetRecord> = emptyList()
)

data class ActiveSessionUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val templateName: String = "Treino livre",
    val exercises: List<ExerciseWithSets> = emptyList(),
    /** Índice do exercício em foco na sessão (um por vez, na ordem do treino). */
    val currentExerciseIndex: Int = 0,
    val restSecondsLeft: Int? = null,
    val restTotalSeconds: Int = 90,
    /** Duração total do descanso em curso (pode vir do exercício, não do padrão). */
    val restCurrentTotalSeconds: Int = 90,
    val finished: Boolean = false
) {
    /** Volume total (kg) das séries válidas, excluindo aquecimento. */
    val totalVolume: Float
        get() = exercises
            .flatMap { it.sets }
            .filterNot { it.isWarmup }
            .fold(0f) { acc, set -> acc + set.weightKg * set.reps }

    val totalSets: Int get() = exercises.sumOf { it.sets.size }

    val currentExercise: ExerciseWithSets? get() = exercises.getOrNull(currentExerciseIndex)
    val hasPreviousExercise: Boolean get() = currentExerciseIndex > 0
    val hasNextExercise: Boolean get() = currentExerciseIndex < exercises.lastIndex
}

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val notificationHelper: NotificationHelper,
    private val widgetUpdater: WidgetUpdater,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: -1L

    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    /** Emite o nome do exercício quando um novo PR é detectado. */
    private val _prEvents = MutableSharedFlow<String>()
    val prEvents: SharedFlow<String> = _prEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            RestTimerService.state.collect { rest ->
                _uiState.update {
                    it.copy(
                        restSecondsLeft = rest?.secondsLeft,
                        restCurrentTotalSeconds = rest?.totalSeconds ?: it.restCurrentTotalSeconds
                    )
                }
            }
        }
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session == null) {
                _uiState.update { it.copy(loading = false, finished = true) }
                return@launch
            }
            val templateId = session.templateId
            val template = templateId?.let { repository.getTemplate(it) }
            val exercises = templateId?.let { repository.getExercises(it) }.orEmpty()

            val withLastSets = exercises.map { ex ->
                ExerciseWithSets(ex, lastSets = repository.lastPerformance(ex.id, sessionId))
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    session = session,
                    templateName = template?.name ?: "Treino livre",
                    exercises = withLastSets
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

    fun registerSet(exerciseId: Long, weightKg: Float, reps: Int, isWarmup: Boolean, rpe: Float? = null) {
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
                    rpe = rpe,
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
            widgetUpdater.refreshAll()
            val exerciseRest = _uiState.value.exercises
                .firstOrNull { it.exercise.id == exerciseId }
                ?.exercise?.restSeconds
            startRest(exerciseRest)
        }
    }

    fun deleteSet(set: SetRecord) {
        viewModelScope.launch { repository.deleteSet(set.id) }
    }

    // ── Navegação entre exercícios (um por vez, na ordem do treino) ──

    fun nextExercise() {
        _uiState.update {
            it.copy(currentExerciseIndex = (it.currentExerciseIndex + 1).coerceAtMost(it.exercises.lastIndex.coerceAtLeast(0)))
        }
    }

    fun previousExercise() {
        _uiState.update {
            it.copy(currentExerciseIndex = (it.currentExerciseIndex - 1).coerceAtLeast(0))
        }
    }

    // ── Timer de descanso ──

    fun adjustRestDuration(deltaSeconds: Int) {
        _uiState.update {
            it.copy(restTotalSeconds = (it.restTotalSeconds + deltaSeconds).coerceIn(15, 600))
        }
    }

    /**
     * [overrideSeconds] usa o descanso próprio do exercício, quando definido.
     * O countdown roda no [RestTimerService] (foreground service), não no viewModelScope,
     * para sobreviver caso o sistema mate o processo do app em segundo plano.
     */
    fun startRest(overrideSeconds: Int? = null) {
        val total = overrideSeconds ?: _uiState.value.restTotalSeconds
        RestTimerService.start(appContext, total)
    }

    fun skipRest() {
        RestTimerService.skip(appContext)
    }

    // ── Encerramento ──

    fun finishSession(notes: String? = null) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            RestTimerService.skip(appContext)
            repository.finishSession(session, _uiState.value.totalVolume, notes?.trim()?.ifBlank { null })
            widgetUpdater.refreshAll()
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun discardSession() {
        viewModelScope.launch {
            RestTimerService.skip(appContext)
            repository.deleteSession(sessionId)
            widgetUpdater.refreshAll()
            _uiState.update { it.copy(finished = true) }
        }
    }
}
