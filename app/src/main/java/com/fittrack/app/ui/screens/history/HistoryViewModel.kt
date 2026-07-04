package com.fittrack.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.ExercisePr
import com.fittrack.app.data.local.dao.SessionWithTemplateName
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class WeekVolume(val weekStart: LocalDate, val volumeKg: Float)

data class HistoryUiState(
    val allSessions: List<SessionWithTemplateName> = emptyList(),
    val prs: List<ExercisePr> = emptyList(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate? = null,
    val searchQuery: String = ""
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun SessionWithTemplateName.localDate(): LocalDate =
        Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()

    /** Dias do mês visível que têm treino. */
    val trainedDaysInMonth: Set<Int>
        get() = allSessions
            .map { it.localDate() }
            .filter { YearMonth.from(it) == visibleMonth }
            .map { it.dayOfMonth }
            .toSet()

    /** Sessões após aplicar filtro de dia e busca por nome. */
    val filteredSessions: List<SessionWithTemplateName>
        get() = allSessions
            .filter { selectedDay == null || it.localDate() == selectedDay }
            .filter {
                searchQuery.isBlank() ||
                    (it.templateName ?: "Treino livre").contains(searchQuery, ignoreCase = true)
            }

    /** Volume somado por semana (últimas 8 semanas com treino). */
    val weeklyVolume: List<WeekVolume>
        get() = allSessions
            .groupBy { it.localDate().with(DayOfWeek.MONDAY) }
            .map { (weekStart, sessions) ->
                WeekVolume(weekStart, sessions.fold(0f) { acc, s -> acc + s.session.totalVolume })
            }
            .sortedBy { it.weekStart }
            .takeLast(8)
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: WorkoutRepository
) : ViewModel() {

    private val filters = MutableStateFlow(
        Triple<YearMonth, LocalDate?, String>(YearMonth.now(), null, "")
    )

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeFinishedSessions(),
        repository.observeExercisePrs(),
        filters
    ) { sessions, prs, (month, day, query) ->
        HistoryUiState(
            allSessions = sessions,
            prs = prs,
            visibleMonth = month,
            selectedDay = day,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun changeMonth(delta: Long) {
        filters.update { (month, _, query) ->
            Triple(month.plusMonths(delta), null, query)
        }
    }

    fun selectDay(day: LocalDate?) {
        filters.update { (month, current, query) ->
            Triple(month, if (current == day) null else day, query)
        }
    }

    fun setSearchQuery(query: String) {
        filters.update { (month, day, _) -> Triple(month, day, query) }
    }
}
