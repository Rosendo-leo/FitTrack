package com.fittrack.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.ui.components.SimpleLineChart

private val weekLetters = listOf("S", "T", "Q", "Q", "S", "S", "D")

@Composable
fun DashboardScreen(
    onOpenSession: (sessionId: Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
        }

        state.activeSession?.let { session ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sessão em andamento", style = MaterialTheme.typography.labelSmall)
                        Text("Você tem um treino aberto.", style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = { onOpenSession(session.id) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Continuar treino") }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Peso atual + delta da semana
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Peso atual", style = MaterialTheme.typography.labelSmall)
                        Text(
                            state.latestWeight?.let { "%.1f kg".format(it.weightKg) } ?: "—",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        state.weekDeltaKg?.let { delta ->
                            val sign = if (delta > 0) "+" else ""
                            Text(
                                "$sign%.1f kg na semana".format(delta),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (delta <= 0) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                // Streak
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Streak", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "🔥 ${state.streakDays}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            if (state.streakDays == 1) "dia seguido" else "dias seguidos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Esta semana", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        state.weekDays.forEachIndexed { index, trained ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    weekLetters[index],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(14.dp)
                                        .background(
                                            color = if (trained) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Peso — últimos registros", style = MaterialTheme.typography.titleMedium)
                    SimpleLineChart(
                        points = state.weightPoints,
                        valueFormatter = { "%.1f kg".format(it) }
                    )
                    OutlinedButton(onClick = { showWeightDialog = true }) {
                        Text("+ Registrar peso")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Treinos recentes", style = MaterialTheme.typography.labelSmall)
                    if (state.recentSessions.isEmpty()) {
                        Text(
                            "Nenhum treino finalizado ainda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.recentSessions.forEach { session ->
                            Text(
                                "Volume %.0f kg".format(session.totalVolume),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWeightDialog) {
        QuickWeightDialog(
            onDismiss = { showWeightDialog = false },
            onConfirm = { weight ->
                viewModel.quickRegisterWeight(weight)
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun QuickWeightDialog(onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var weightText by rememberSaveable { mutableStateOf("") }
    val parsed = weightText.replace(',', '.').toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar peso") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Peso (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && parsed > 0
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
