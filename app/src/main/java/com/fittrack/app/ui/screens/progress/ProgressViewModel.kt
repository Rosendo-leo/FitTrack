package com.fittrack.app.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.repository.MetricsRepository
import com.fittrack.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val metrics: List<BodyMetric> = emptyList(),
    val cardioSessions: List<CardioSession> = emptyList()
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
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: MetricsRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.observeAllMetrics(),
        repository.observeAllCardio()
    ) { metrics, cardio ->
        ProgressUiState(metrics = metrics, cardioSessions = cardio)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState()
    )

    fun saveMetric(
        weightKg: Float,
        bodyFatPct: Float?,
        waistCm: Float?,
        armCm: Float?,
        chestCm: Float?,
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
}
