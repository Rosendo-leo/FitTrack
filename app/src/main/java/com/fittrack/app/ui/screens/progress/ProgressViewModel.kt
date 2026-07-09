package com.fittrack.app.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.local.entities.Goal
import com.fittrack.app.data.local.entities.GoalMetric
import com.fittrack.app.data.repository.MetricsRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.strengthProgression
import com.fittrack.app.domain.weeklyVolume
import com.fittrack.app.ui.common.isAchieved
import com.fittrack.app.ui.common.valueIn
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val metrics: List<BodyMetric> = emptyList(),
    val cardioSessions: List<CardioSession> = emptyList(),
    val goals: List<Goal> = emptyList()
) {
    /** Pontos (data, peso) em ordem cronológica para o gráfico. */
    val weightPoints: List<Pair<Long, Float>>
        get() = metrics.sortedBy { it.date }.map { it.date to it.weightKg }

    /** Minutos de cardio somados por tipo, do maior para o menor. */
    val cardioMinutesByType: List<Pair<CardioType, Int>>
        get() = cardioSessions
            .groupBy { it.type }
            .map { (type, sessions) -> type to sessions.sumOf { it.durationMin } }
            .sortedByDescending { it.second }

    /** Último valor registrado para essa métrica (peso ou uma medida corporal). */
    fun latestValue(metric: GoalMetric): Float? =
        metrics.sortedByDescending { it.date }.firstNotNullOfOrNull { metric.valueIn(it) }
}

data class StrengthUiState(
    val exerciseNames: List<String> = emptyList(),
    val selectedExercise: String? = null,
    /** Pontos (data, 1RM estimado em kg) por sessão. */
    val strengthPoints: List<Pair<Long, Float>> = emptyList(),
    /** Pontos (início da semana, volume em kg) por semana. */
    val weeklyVolumePoints: List<Pair<Long, Float>> = emptyList()
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: MetricsRepository,
    private val workoutRepository: WorkoutRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.observeAllMetrics(),
        repository.observeAllCardio(),
        repository.observeGoals()
    ) { metrics, cardio, goals ->
        ProgressUiState(metrics = metrics, cardioSessions = cardio, goals = goals)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState()
    )

    init {
        // Marca metas como atingidas automaticamente quando o valor atual alcança o alvo.
        viewModelScope.launch {
            uiState.collect { state ->
                state.goals
                    .filter { it.achievedAt == null }
                    .filter { it.isAchieved(state.latestValue(it.metric)) }
                    .forEach { repository.markGoalAchieved(it) }
            }
        }
    }

    // ── Progressão de força ──

    private val selectedExercise = MutableStateFlow<String?>(null)

    /** Exercício efetivo: o escolhido pelo usuário, ou o primeiro disponível. */
    private val effectiveExercise = combine(
        workoutRepository.observeTrainedExerciseNames(),
        selectedExercise
    ) { names, selected -> selected?.takeIf { it in names } ?: names.firstOrNull() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val strengthPoints = effectiveExercise.flatMapLatest { name ->
        if (name == null) flowOf(emptyList())
        else workoutRepository.observeWorkingSetsFor(name).map { sets ->
            strengthProgression(sets.map { Triple(it.date, it.weightKg, it.reps) })
        }
    }

    val strengthState: StateFlow<StrengthUiState> = combine(
        workoutRepository.observeTrainedExerciseNames(),
        effectiveExercise,
        strengthPoints,
        workoutRepository.observeFinishedSessions()
    ) { names, selected, points, sessions ->
        StrengthUiState(
            exerciseNames = names,
            selectedExercise = selected,
            strengthPoints = points,
            weeklyVolumePoints = weeklyVolume(
                sessions.map { it.session.startedAt to it.session.totalVolume }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StrengthUiState()
    )

    fun selectExercise(name: String) {
        selectedExercise.value = name
    }

    fun saveMetric(
        weightKg: Float,
        bodyFatPct: Float?,
        waistCm: Float?,
        armCm: Float?,
        chestCm: Float?,
        armFlexedLeftCm: Float?,
        armFlexedRightCm: Float?,
        armRelaxedLeftCm: Float?,
        armRelaxedRightCm: Float?,
        shoulderCm: Float?,
        thighLeftCm: Float?,
        thighRightCm: Float?,
        forearmLeftCm: Float?,
        forearmRightCm: Float?,
        calfLeftCm: Float?,
        calfRightCm: Float?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.saveMetric(
                BodyMetric(
                    weightKg = weightKg,
                    bodyFatPct = bodyFatPct,
                    waistCm = waistCm,
                    armCm = armCm,
                    chestCm = chestCm,
                    armFlexedLeftCm = armFlexedLeftCm,
                    armFlexedRightCm = armFlexedRightCm,
                    armRelaxedLeftCm = armRelaxedLeftCm,
                    armRelaxedRightCm = armRelaxedRightCm,
                    shoulderCm = shoulderCm,
                    thighLeftCm = thighLeftCm,
                    thighRightCm = thighRightCm,
                    forearmLeftCm = forearmLeftCm,
                    forearmRightCm = forearmRightCm,
                    calfLeftCm = calfLeftCm,
                    calfRightCm = calfRightCm,
                    notes = notes?.ifBlank { null }
                )
            )
            widgetUpdater.refreshAll()
        }
    }

    fun deleteMetric(metric: BodyMetric) {
        viewModelScope.launch {
            repository.deleteMetric(metric)
            widgetUpdater.refreshAll()
        }
    }

    fun saveCardio(
        type: CardioType,
        durationMin: Int,
        distanceKm: Float?,
        calories: Int?,
        avgHeartRate: Int?
    ) {
        viewModelScope.launch {
            repository.saveCardio(
                CardioSession(
                    type = type,
                    durationMin = durationMin,
                    distanceKm = distanceKm,
                    calories = calories,
                    avgHeartRate = avgHeartRate
                )
            )
        }
    }

    fun deleteCardio(session: CardioSession) {
        viewModelScope.launch { repository.deleteCardio(session) }
    }

    // ── Metas ──

    fun saveGoal(metric: GoalMetric, targetValue: Float) {
        viewModelScope.launch {
            val start = uiState.value.latestValue(metric) ?: targetValue
            repository.saveGoal(Goal(metric = metric, targetValue = targetValue, startValue = start))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}
