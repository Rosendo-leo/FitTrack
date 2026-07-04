package com.fittrack.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.BuildConfig
import com.fittrack.app.data.preferences.ThemeMode

private val dayLabels = listOf("S", "T", "Q", "Q", "S", "S", "D") // ISO 1=Seg … 7=Dom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showWorkoutTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showWeightTimeDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
        }

        // ── Tema ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        val options = listOf(
                            ThemeMode.SYSTEM to "Sistema",
                            ThemeMode.LIGHT to "Claro",
                            ThemeMode.DARK to "Escuro"
                        )
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                )
                            ) { Text(label) }
                        }
                    }
                }
            }
        }

        // ── Lembrete de treino ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Lembrete de treino 🏋️", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Notificação nos dias escolhidos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.workoutReminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setWorkoutReminder(
                                    enabled,
                                    prefs.workoutReminderDays.ifEmpty { setOf(1, 3, 5) },
                                    prefs.workoutReminderHour,
                                    prefs.workoutReminderMinute
                                )
                            }
                        )
                    }

                    if (prefs.workoutReminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..7).forEach { isoDay ->
                                FilterChip(
                                    selected = isoDay in prefs.workoutReminderDays,
                                    onClick = {
                                        val days = prefs.workoutReminderDays.toMutableSet()
                                        if (!days.add(isoDay)) days.remove(isoDay)
                                        viewModel.setWorkoutReminder(
                                            true, days,
                                            prefs.workoutReminderHour,
                                            prefs.workoutReminderMinute
                                        )
                                    },
                                    label = { Text(dayLabels[isoDay - 1]) }
                                )
                            }
                        }
                        TextButton(onClick = { showWorkoutTimeDialog = true }) {
                            Text(
                                "Horário: %02d:%02d".format(
                                    prefs.workoutReminderHour,
                                    prefs.workoutReminderMinute
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Lembrete de peso ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Registrar peso ⚖️", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Lembrete diário",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.weightReminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setWeightReminder(
                                    enabled,
                                    prefs.weightReminderHour,
                                    prefs.weightReminderMinute
                                )
                            }
                        )
                    }
                    if (prefs.weightReminderEnabled) {
                        TextButton(onClick = { showWeightTimeDialog = true }) {
                            Text(
                                "Horário: %02d:%02d".format(
                                    prefs.weightReminderHour,
                                    prefs.weightReminderMinute
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Sobre ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sobre", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "FitTrack v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Backup no Drive e atualização automática chegam nas próximas versões.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showWorkoutTimeDialog) {
        TimePickerDialog(
            initialHour = prefs.workoutReminderHour,
            initialMinute = prefs.workoutReminderMinute,
            onDismiss = { showWorkoutTimeDialog = false },
            onConfirm = { hour, minute ->
                viewModel.setWorkoutReminder(
                    true, prefs.workoutReminderDays, hour, minute
                )
                showWorkoutTimeDialog = false
            }
        )
    }
    if (showWeightTimeDialog) {
        TimePickerDialog(
            initialHour = prefs.weightReminderHour,
            initialMinute = prefs.weightReminderMinute,
            onDismiss = { showWeightTimeDialog = false },
            onConfirm = { hour, minute ->
                viewModel.setWeightReminder(true, hour, minute)
                showWeightTimeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timeState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolher horário") },
        text = { androidx.compose.material3.TimePicker(state = timeState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
