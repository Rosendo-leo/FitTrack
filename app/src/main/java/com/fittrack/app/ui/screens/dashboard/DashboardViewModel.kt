package com.fittrack.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.repository.MetricsRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
    val recentSessions: List<WorkoutSession> = emptyList(),
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
        workoutRepository.observeActiveSession()
    ) { metrics, sessions, active ->
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
        var streak = 0
        var cursor = if (today in trainedDays) today else today.minusDays(1)
        while (cursor in trainedDays) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val monday = today.with(DayOfWeek.MONDAY)
        val weekDays = (0..6).map { offset -> monday.plusDays(offset.toLong()) in trainedDays }

        DashboardUiState(
            latestWeight = latest,
            weekDeltaKg = weekDelta,
            streakDays = streak,
            weekDays = weekDays,
            weightPoints = metrics.sortedBy { it.date }
                .takeLast(30)
                .map { it.date to it.weightKg },
            recentSessions = sessions.filter { it.finishedAt != null }.take(5),
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
