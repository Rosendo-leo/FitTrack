package com.fittrack.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.SessionWithTemplateName
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.repository.MetricsRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.currentStreak
import com.fittrack.app.domain.weekTrainedFlags
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val latestWeight: BodyMetric? = null,
    /** Variação de peso vs. o registro mais próximo de 7 dias atrás. */
    val weekDeltaKg: Float? = null,
    /** Dias consecutivos (terminando hoje ou ontem) com treino finalizado. */
    val streakDays: Int = 0,
    /** Seg..Dom da semana atual: true = treinou no dia. */
    val weekDays: List<Boolean> = List(7) { false },
    val weightPoints: List<Pair<Long, Float>> = emptyList(),
    val recentSessions: List<SessionWithTemplateName> = emptyList(),
    val activeSession: WorkoutSession? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val metricsRepository: MetricsRepository,
    private val widgetUpdater: WidgetUpdater,
    workoutRepository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        metricsRepository.observeAllMetrics(),
        workoutRepository.observeAllSessions(),
        workoutRepository.observeActiveSession(),
        workoutRepository.observeFinishedSessions()
    ) { metrics, sessions, active, finishedWithName ->
        val zone = ZoneId.systemDefault()
        val latest = metrics.maxByOrNull { it.date }

        val weekDelta = latest?.let { current ->
            val target = current.date - 7L * 24 * 60 * 60 * 1000
            metrics
                .filter { it.id != current.id }
                .minByOrNull { kotlin.math.abs(it.date - target) }
                ?.let { current.weightKg - it.weightKg }
        }

        val trainedDays: Set<LocalDate> = sessions
            .filter { it.finishedAt != null }
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSet()

        val today = LocalDate.now(zone)

        DashboardUiState(
            latestWeight = latest,
            weekDeltaKg = weekDelta,
            streakDays = currentStreak(trainedDays, today),
            weekDays = weekTrainedFlags(trainedDays, today),
            weightPoints = metrics.sortedBy { it.date }
                .takeLast(30)
                .map { it.date to it.weightKg },
            recentSessions = finishedWithName.take(5),
            activeSession = active
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    /** Atalho da esquemática: registrar peso direto do Dashboard. */
    fun quickRegisterWeight(weightKg: Float) {
        viewModelScope.launch {
            metricsRepository.saveMetric(BodyMetric(weightKg = weightKg))
            widgetUpdater.refreshAll()
        }
    }
}
