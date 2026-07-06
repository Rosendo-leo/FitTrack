package com.fittrack.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.ui.common.LocalUserPreferences
import com.fittrack.app.ui.common.format
import com.fittrack.app.ui.components.SimpleLineChart
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))
private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yy · HH:mm", Locale("pt", "BR"))
private val weekHeader = listOf("S", "T", "Q", "Q", "S", "S", "D")

@Composable
fun HistoryScreen(
    onOpenSession: (sessionId: Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weightUnit = LocalUserPreferences.current.weightUnit

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Histórico", style = MaterialTheme.typography.headlineLarge)
        }

        // ── Calendário ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.changeMonth(-1) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mês anterior"
                            )
                        }
                        Text(
                            state.visibleMonth.format(monthFormatter)
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.changeMonth(+1) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Próximo mês"
                            )
                        }
                    }
                    MonthCalendar(
                        month = state.visibleMonth,
                        trainedDays = state.trainedDaysInMonth,
                        selectedDay = state.selectedDay,
                        onDayClick = viewModel::selectDay
                    )
                }
            }
        }

        // ── Volume semanal ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Volume por semana", style = MaterialTheme.typography.titleMedium)
                    SimpleLineChart(
                        points = state.weeklyVolume.map { weekVolume ->
                            val millis = weekVolume.weekStart
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            millis to weekVolume.volumeKg
                        },
                        valueFormatter = { weightUnit.format(it, decimals = 0) }
                    )
                }
            }
        }

        // ── PRs ──
        if (state.prs.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Recordes por exercício 🏆", style = MaterialTheme.typography.titleMedium)
                        state.prs.take(10).forEach { pr ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pr.exerciseName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${weightUnit.format(pr.weightKg)} × ${pr.reps}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Busca + lista de sessões ──
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Buscar por treino") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(state.filteredSessions, key = { it.session.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSession(item.session.id) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        item.templateName ?: "Treino livre",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val started = Instant.ofEpochMilli(item.session.startedAt)
                        .atZone(ZoneId.systemDefault())
                    val durationMin = item.session.finishedAt
                        ?.let { (it - item.session.startedAt) / 60_000 }
                    Text(
                        listOfNotNull(
                            started.format(dayFormatter),
                            durationMin?.let { "$it min" },
                            "volume ${weightUnit.format(item.session.totalVolume, decimals = 0)}"
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.filteredSessions.isEmpty()) {
            item {
                Text(
                    if (state.selectedDay != null) "Nenhum treino no dia selecionado."
                    else "Nenhum treino finalizado ainda.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    trainedDays: Set<Int>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeader.forEach { letter ->
                Text(
                    letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Offset do primeiro dia (semana começando na segunda)
        val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0..6) {
                    val dayNumber = row * 7 + column - firstDayOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val trained = dayNumber in trainedDays
                        val selected = date == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .background(
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.primary
                                        trained -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    shape = CircleShape
                                )
                                .clickable { onDayClick(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$dayNumber",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    trained -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (trained) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
