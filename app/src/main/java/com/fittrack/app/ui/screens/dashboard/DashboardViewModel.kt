package com.fittrack.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.repository.MetricsRepository
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val latestWeight: BodyMetric? = null,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val activeSession: WorkoutSession? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    metricsRepository: MetricsRepository,
    workoutRepository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        metricsRepository.observeLatestMetric(),
        workoutRepository.observeAllSessions(),
        workoutRepository.observeActiveSession()
    ) { latest, sessions, active ->
        DashboardUiState(
            latestWeight = latest,
            recentSessions = sessions.take(5),
            activeSession = active
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )
}
