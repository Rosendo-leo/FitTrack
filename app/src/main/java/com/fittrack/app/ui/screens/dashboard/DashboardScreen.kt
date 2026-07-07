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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.preferences.WeightUnit
import com.fittrack.app.ui.common.LocalUserPreferences
import com.fittrack.app.ui.common.format
import com.fittrack.app.ui.common.suffix
import com.fittrack.app.ui.common.toKg
import com.fittrack.app.ui.components.SimpleLineChart
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekLetters = listOf("S", "T", "Q", "Q", "S", "S", "D")
private val recentFormatter = DateTimeFormatter.ofPattern("dd/MM · HH:mm", Locale("pt", "BR"))

@Composable
fun DashboardScreen(
    onOpenSession: (sessionId: Long) -> Unit,
    autoOpenWeightDialog: Boolean = false,
    onAutoOpenHandled: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weightUnit = LocalUserPreferences.current.weightUnit
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(autoOpenWeightDialog) {
        if (autoOpenWeightDialog) {
            showWeightDialog = true
            onAutoOpenHandled()
        }
    }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Sessão em andamento",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            "Você tem um treino aberto.",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = { onOpenSession(session.id) },
                            modifier = Modifier.padding(top = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
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
                            state.latestWeight?.let { weightUnit.format(it.weightKg) } ?: "—",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        state.weekDeltaKg?.let { delta ->
                            val sign = if (delta > 0) "+" else ""
                            Text(
                                "$sign${weightUnit.format(delta)} na semana",
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (trained) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    weekLetters[index],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (trained) MaterialTheme.colorScheme.onTertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
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
                        valueFormatter = { weightUnit.format(it) }
                    )
                    FilledTonalButton(
                        onClick = { showWeightDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
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
                        state.recentSessions.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        item.templateName ?: "Treino livre",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        Instant.ofEpochMilli(item.session.startedAt)
                                            .atZone(ZoneId.systemDefault())
                                            .format(recentFormatter),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    weightUnit.format(item.session.totalVolume, decimals = 0),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWeightDialog) {
        QuickWeightDialog(
            unit = weightUnit,
            onDismiss = { showWeightDialog = false },
            onConfirm = { weight ->
                viewModel.quickRegisterWeight(weightUnit.toKg(weight))
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun QuickWeightDialog(
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var weightText by rememberSaveable { mutableStateOf("") }
    val parsed = weightText.replace(',', '.').toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar peso") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Peso (${unit.suffix})") },
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
