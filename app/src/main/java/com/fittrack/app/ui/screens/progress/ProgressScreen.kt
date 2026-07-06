package com.fittrack.app.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.preferences.DistanceUnit
import com.fittrack.app.data.preferences.WeightUnit
import com.fittrack.app.ui.common.LocalUserPreferences
import com.fittrack.app.ui.common.format
import com.fittrack.app.ui.common.label
import com.fittrack.app.ui.common.suffix
import com.fittrack.app.ui.common.toKg
import com.fittrack.app.ui.common.toKm
import com.fittrack.app.ui.components.SimpleLineChart
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yy", Locale("pt", "BR"))

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)

private fun String.toFloatOrNullPt(): Float? = replace(',', '.').toFloatOrNull()

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showMetricDialog by rememberSaveable { mutableStateOf(false) }
    var showCardioDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedTab == 0) showMetricDialog = true else showCardioDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Registrar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Progresso",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Corpo") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cardio") }
                )
            }
            when (selectedTab) {
                0 -> BodyTab(state, viewModel)
                1 -> CardioTab(state, viewModel)
            }
        }
    }

    val prefs = LocalUserPreferences.current
    if (showMetricDialog) {
        MetricDialog(
            weightUnit = prefs.weightUnit,
            onDismiss = { showMetricDialog = false },
            onConfirm = { weight, fat, waist, arm, chest, notes ->
                viewModel.saveMetric(prefs.weightUnit.toKg(weight), fat, waist, arm, chest, notes)
                showMetricDialog = false
            }
        )
    }
    if (showCardioDialog) {
        CardioDialog(
            distanceUnit = prefs.distanceUnit,
            onDismiss = { showCardioDialog = false },
            onConfirm = { type, duration, distance, calories, hr ->
                viewModel.saveCardio(
                    type, duration, distance?.let { prefs.distanceUnit.toKm(it) }, calories, hr
                )
                showCardioDialog = false
            }
        )
    }
}

/** IMC e classificação segundo a OMS. */
private fun bmiLabel(bmi: Float): String = when {
    bmi < 18.5f -> "Abaixo do peso"
    bmi < 25f -> "Peso normal"
    bmi < 30f -> "Sobrepeso"
    else -> "Obesidade"
}

@Composable
private fun BodyTab(state: ProgressUiState, viewModel: ProgressViewModel) {
    val prefs = LocalUserPreferences.current
    val weightUnit = prefs.weightUnit
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val latestWeight = state.metrics.maxByOrNull { it.date }?.weightKg
        if (latestWeight != null && prefs.heightCm > 0f) {
            item {
                val heightM = prefs.heightCm / 100f
                val bmi = latestWeight / (heightM * heightM)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("IMC", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "%.1f".format(bmi),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        Text(
                            bmiLabel(bmi),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Evolução do peso", style = MaterialTheme.typography.titleMedium)
                    SimpleLineChart(
                        points = state.weightPoints,
                        valueFormatter = { weightUnit.format(it) }
                    )
                }
            }
        }
        items(state.metrics, key = { it.id }) { metric ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${weightUnit.format(metric.weightKg)} — ${formatDate(metric.date)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val details = listOfNotNull(
                            metric.bodyFatPct?.let { "%.1f%% gordura".format(it) },
                            metric.waistCm?.let { "cintura %.0f cm".format(it) },
                            metric.armCm?.let { "braço %.0f cm".format(it) },
                            metric.chestCm?.let { "peito %.0f cm".format(it) },
                            metric.notes
                        ).joinToString(" · ")
                        if (details.isNotBlank()) {
                            Text(
                                details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.deleteMetric(metric) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Apagar registro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (state.metrics.isEmpty()) {
            item {
                Text(
                    "Nenhuma medição ainda. Use o botão + para registrar seu peso.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CardioTab(state: ProgressUiState, viewModel: ProgressViewModel) {
    val distanceUnit = LocalUserPreferences.current.distanceUnit
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.cardioMinutesByType.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Minutos por tipo", style = MaterialTheme.typography.titleMedium)
                        state.cardioMinutesByType.forEach { (type, minutes) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(type.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "$minutes min",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        items(state.cardioSessions, key = { it.id }) { session ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${session.type.label} — ${session.durationMin} min",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val details = listOfNotNull(
                            formatDate(session.date),
                            session.distanceKm?.let { distanceUnit.format(it) },
                            session.calories?.let { "$it kcal" },
                            session.avgHeartRate?.let { "$it bpm" }
                        ).joinToString(" · ")
                        Text(
                            details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteCardio(session) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Apagar sessão",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (state.cardioSessions.isEmpty()) {
            item {
                Text(
                    "Nenhum cardio registrado. Use o botão + para registrar corrida, bike, natação…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricDialog(
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirm: (
        weight: Float, bodyFatPct: Float?, waistCm: Float?,
        armCm: Float?, chestCm: Float?, notes: String?
    ) -> Unit
) {
    var weight by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var waist by rememberSaveable { mutableStateOf("") }
    var arm by rememberSaveable { mutableStateOf("") }
    var chest by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar medidas") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                NumberField(weight, { weight = it }, "Peso (${weightUnit.suffix}) *")
                NumberField(fat, { fat = it }, "% de gordura")
                NumberField(waist, { waist = it }, "Cintura (cm)")
                NumberField(arm, { arm = it }, "Braço (cm)")
                NumberField(chest, { chest = it }, "Peito (cm)")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    weight.toFloatOrNullPt()?.let { w ->
                        onConfirm(
                            w,
                            fat.toFloatOrNullPt(),
                            waist.toFloatOrNullPt(),
                            arm.toFloatOrNullPt(),
                            chest.toFloatOrNullPt(),
                            notes.ifBlank { null }
                        )
                    }
                },
                enabled = weight.toFloatOrNullPt() != null
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardioDialog(
    distanceUnit: DistanceUnit,
    onDismiss: () -> Unit,
    onConfirm: (
        type: CardioType, durationMin: Int, distance: Float?,
        calories: Int?, avgHeartRate: Int?
    ) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(CardioType.RUNNING) }
    var typeExpanded by remember { mutableStateOf(false) }
    var duration by rememberSaveable { mutableStateOf("") }
    var distance by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var heartRate by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar cardio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        CardioType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    type = option
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                NumberField(duration, { duration = it }, "Duração (min) *")
                NumberField(distance, { distance = it }, "Distância (${distanceUnit.suffix})")
                NumberField(calories, { calories = it }, "Calorias")
                NumberField(heartRate, { heartRate = it }, "FC média (bpm)")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    duration.toIntOrNull()?.takeIf { it > 0 }?.let { d ->
                        onConfirm(
                            type,
                            d,
                            distance.toFloatOrNullPt(),
                            calories.toIntOrNull(),
                            heartRate.toIntOrNull()
                        )
                    }
                },
                enabled = (duration.toIntOrNull() ?: 0) > 0
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
