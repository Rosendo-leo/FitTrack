package com.fittrack.app.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.BuildConfig
import com.fittrack.app.data.backup.RestoreMode
import com.fittrack.app.data.preferences.DistanceUnit
import com.fittrack.app.data.preferences.ThemeMode
import com.fittrack.app.data.preferences.WeightUnit
import com.fittrack.app.ui.theme.supportsDynamicColor
import java.text.DateFormat
import java.util.Date

private val dayLabels = listOf("S", "T", "Q", "Q", "S", "S", "D") // ISO 1=Seg … 7=Dom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    var showWorkoutTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showWeightTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showHeightDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(backupState.message) {
        backupState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(viewModel::exportToUri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::requestImport) }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onDriveSignInResult() }

    val exportWorkoutsCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportWorkoutsCsvToUri) }

    val exportMetricsCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportMetricsCsvToUri) }

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

                    if (supportsDynamicColor) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Cores dinâmicas",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Usa as cores do papel de parede (Material You)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.dynamicColorEnabled,
                                onCheckedChange = viewModel::setDynamicColorEnabled
                            )
                        }
                    }
                }
            }
        }

        // ── Unidades & perfil ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Unidades & perfil", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Peso", style = MaterialTheme.typography.bodyLarge)
                        SingleChoiceSegmentedButtonRow {
                            val options = listOf(WeightUnit.KG to "kg", WeightUnit.LB to "lb")
                            options.forEachIndexed { index, (unit, label) ->
                                SegmentedButton(
                                    selected = prefs.weightUnit == unit,
                                    onClick = { viewModel.setWeightUnit(unit) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    )
                                ) { Text(label) }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Distância", style = MaterialTheme.typography.bodyLarge)
                        SingleChoiceSegmentedButtonRow {
                            val options = listOf(DistanceUnit.KM to "km", DistanceUnit.MI to "mi")
                            options.forEachIndexed { index, (unit, label) ->
                                SegmentedButton(
                                    selected = prefs.distanceUnit == unit,
                                    onClick = { viewModel.setDistanceUnit(unit) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    )
                                ) { Text(label) }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Altura", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Usada para calcular o IMC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showHeightDialog = true }) {
                            Text(
                                if (prefs.heightCm > 0f) "%.0f cm".format(prefs.heightCm)
                                else "Informar"
                            )
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

        // ── Backup ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Backup 💾", style = MaterialTheme.typography.titleMedium)
                        if (backupState.busy) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        }
                    }

                    // Arquivo local (SAF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            enabled = !backupState.busy,
                            onClick = { exportLauncher.launch("fittrack-backup.zip") }
                        ) { Text("Exportar arquivo") }
                        TextButton(
                            enabled = !backupState.busy,
                            onClick = {
                                importLauncher.launch(
                                    arrayOf("application/zip", "application/json", "application/octet-stream")
                                )
                            }
                        ) { Text("Importar arquivo") }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Google Drive
                    val email = backupState.driveAccountEmail
                    if (email == null) {
                        Text(
                            "Conecte sua conta Google para guardar backups na pasta oculta do app no Drive.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            enabled = !backupState.busy,
                            onClick = { driveSignInLauncher.launch(viewModel.driveSignInIntent()) }
                        ) { Text("Conectar Google Drive") }
                    } else {
                        Text(
                            "Google Drive: $email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (prefs.lastDriveBackupAt > 0L) {
                            Text(
                                "Último backup: " + DateFormat.getDateTimeInstance(
                                    DateFormat.SHORT, DateFormat.SHORT
                                ).format(Date(prefs.lastDriveBackupAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                enabled = !backupState.busy,
                                onClick = viewModel::driveBackupNow
                            ) { Text("Backup agora") }
                            TextButton(
                                enabled = !backupState.busy,
                                onClick = viewModel::requestDriveRestore
                            ) { Text("Restaurar") }
                            TextButton(
                                enabled = !backupState.busy,
                                onClick = viewModel::driveSignOut
                            ) { Text("Desconectar") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Backup automático diário",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = prefs.driveSyncEnabled,
                                onCheckedChange = viewModel::setDriveSyncEnabled
                            )
                        }
                    }
                }
            }
        }

        // ── Exportar CSV ──
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Exportar CSV 📊", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Planilhas com seus treinos e medidas, para abrir no Excel/Sheets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            enabled = !backupState.busy,
                            onClick = { exportWorkoutsCsvLauncher.launch("fittrack-treinos.csv") }
                        ) { Text("Treinos") }
                        TextButton(
                            enabled = !backupState.busy,
                            onClick = { exportMetricsCsvLauncher.launch("fittrack-medidas.csv") }
                        ) { Text("Medidas") }
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
                        "Backups locais e no Google Drive na seção acima; atualizações chegam via GitHub Releases.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showHeightDialog) {
        HeightDialog(
            initialCm = prefs.heightCm,
            onDismiss = { showHeightDialog = false },
            onConfirm = { cm ->
                viewModel.setHeightCm(cm)
                showHeightDialog = false
            }
        )
    }

    if (backupState.pendingRestore != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Restaurar backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Como aplicar os dados do backup?")
                    Text(
                        "Substituir: apaga tudo que está no app e restaura o backup.\n" +
                            "Mesclar: mantém os dados atuais e adiciona só o que falta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore(RestoreMode.REPLACE) }) {
                    Text("Substituir")
                }
                TextButton(onClick = { viewModel.confirmRestore(RestoreMode.MERGE) }) {
                    Text("Mesclar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("Cancelar") }
            }
        )
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

@Composable
private fun HeightDialog(
    initialCm: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var heightText by rememberSaveable {
        mutableStateOf(if (initialCm > 0f) "%.0f".format(initialCm) else "")
    }
    val parsed = heightText.replace(',', '.').toFloatOrNull()
    val valid = parsed != null && parsed in 80f..250f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Altura") },
        text = {
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text("Altura (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = valid
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
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
