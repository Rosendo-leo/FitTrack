package com.fittrack.app.ui.screens.active_session

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.preferences.WeightUnit
import com.fittrack.app.ui.common.LocalUserPreferences
import com.fittrack.app.ui.common.format
import com.fittrack.app.ui.common.fromKg
import com.fittrack.app.ui.common.suffix
import com.fittrack.app.ui.common.toKg
import kotlinx.coroutines.delay
import java.util.Locale

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

/** Valor para campo de entrada: sem sufixo e sem zeros à direita. */
private fun Float.toInputString(): String =
    if (this == toInt().toFloat()) toInt().toString()
    else String.format(Locale.US, "%.1f", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    onExit: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weightUnit = LocalUserPreferences.current.weightUnit
    val snackbarHostState = remember { SnackbarHostState() }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var sessionNotes by rememberSaveable { mutableStateOf("") }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val isResting = state.restSecondsLeft != null

    // Ao sair com séries registradas, deixa claro que a sessão continua aberta
    val exitKeepingSession: () -> Unit = {
        if (state.totalSets > 0 && !state.finished) {
            Toast.makeText(
                context, "Treino continua em segundo plano", Toast.LENGTH_SHORT
            ).show()
        }
        onExit()
    }
    BackHandler(enabled = state.totalSets > 0 && !state.finished) { exitKeepingSession() }

    LaunchedEffect(state.finished) {
        if (state.finished) onExit()
    }

    LaunchedEffect(viewModel) {
        viewModel.prEvents.collect { exerciseName ->
            snackbarHostState.showSnackbar("🏆 Novo PR em $exerciseName!")
        }
    }

    LaunchedEffect(state.session?.startedAt) {
        val startedAt = state.session?.startedAt ?: return@LaunchedEffect
        while (true) {
            elapsedMillis = System.currentTimeMillis() - startedAt
            delay(1_000)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isResting) {
                TopAppBar(
                    title = {
                        Column {
                            Text(state.templateName, style = MaterialTheme.typography.titleLarge)
                            val progressLabel = if (state.exercises.isNotEmpty()) {
                                "Exercício ${state.currentExerciseIndex + 1} de ${state.exercises.size} · " +
                                    formatDuration(elapsedMillis)
                            } else {
                                formatDuration(elapsedMillis)
                            }
                            Text(
                                progressLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = exitKeepingSession) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDiscardDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Descartar treino",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(onClick = { showFinishDialog = true }) { Text("Finalizar") }
                    }
                )
            }
        }
    ) { padding ->
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        state.restSecondsLeft?.let { remaining ->
            FullScreenRest(
                remaining = remaining,
                total = state.restCurrentTotalSeconds,
                nextExerciseName = state.currentExercise?.exercise?.name,
                onSkip = viewModel::skipRest,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Volume: ${weightUnit.format(state.totalVolume, decimals = 0)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${state.totalSets} séries",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    "Descanso: ${state.restTotalSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { viewModel.adjustRestDuration(-15) }) { Text("−15s") }
                OutlinedButton(onClick = { viewModel.adjustRestDuration(+15) }) { Text("+15s") }
            }

            val current = state.currentExercise
            if (current == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Treino livre — sem exercícios pré-definidos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ExerciseCard(
                        item = current,
                        weightUnit = weightUnit,
                        onRegisterSet = { weight, reps, warmup, rpe ->
                            viewModel.registerSet(
                                current.exercise.id, weightUnit.toKg(weight), reps, warmup, rpe
                            )
                        },
                        onDeleteSet = viewModel::deleteSet
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::previousExercise,
                        enabled = state.hasPreviousExercise,
                        modifier = Modifier.weight(1f)
                    ) { Text("Anterior") }
                    Button(
                        onClick = viewModel::nextExercise,
                        enabled = state.hasNextExercise,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Próximo exercício")
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar treino") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Volume total: ${weightUnit.format(state.totalVolume, decimals = 0)} " +
                            "em ${state.totalSets} séries."
                    )
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Observação (opcional)") },
                        placeholder = { Text("Como foi o treino hoje?") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    viewModel.finishSession(sessionNotes)
                }) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Continuar treinando") }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar treino") },
            text = { Text("Todas as séries desta sessão serão apagadas. Descartar?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.discardSession()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Toma a tela inteira durante o descanso; volta sozinha para o exercício quando zera. */
@Composable
private fun FullScreenRest(
    remaining: Int,
    total: Int,
    nextExerciseName: String?,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (total > 0) remaining / total.toFloat() else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "restProgress"
    )
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Descanso",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                formatDuration(remaining * 1000L),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            nextExerciseName?.let {
                Text(
                    "Em seguida: $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.7f).padding(top = 8.dp)
            )
            OutlinedButton(onClick = onSkip, modifier = Modifier.padding(top = 16.dp)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Text("Pular descanso", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    item: ExerciseWithSets,
    weightUnit: WeightUnit,
    onRegisterSet: (weight: Float, reps: Int, isWarmup: Boolean, rpe: Float?) -> Unit,
    onDeleteSet: (SetRecord) -> Unit
) {
    var weightText by rememberSaveable(item.exercise.id) { mutableStateOf("") }
    var repsText by rememberSaveable(item.exercise.id) { mutableStateOf("") }
    var rpeText by rememberSaveable(item.exercise.id) { mutableStateOf("") }
    var isWarmup by rememberSaveable(item.exercise.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Sugere a carga da última sessão enquanto o usuário ainda não digitou nada
    LaunchedEffect(item.lastSets) {
        if (weightText.isBlank() && item.sets.isEmpty()) {
            item.lastSets.lastOrNull { !it.isWarmup }?.let { last ->
                weightText = weightUnit.fromKg(last.weightKg).toInputString()
                repsText = last.reps.toString()
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
            val subtitle = listOfNotNull(
                item.exercise.muscleGroup.ifBlank { null },
                item.exercise.notes
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val lastWorking = item.lastSets.filterNot { it.isWarmup }
            lastWorking.maxByOrNull { it.weightKg }?.let { best ->
                Text(
                    "Última vez: ${weightUnit.format(best.weightKg)} × ${best.reps}" +
                        " · ${lastWorking.size} " +
                        if (lastWorking.size == 1) "série" else "séries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            item.sets.forEach { set ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${set.setNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${weightUnit.format(set.weightKg)} × ${set.reps}" +
                            (set.rpe?.let { "  @ RPE %.1f".format(it) } ?: "") +
                            if (set.isWarmup) "  (aquecimento)" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    IconButton(onClick = { onDeleteSet(set) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Apagar série",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val focusManager = LocalFocusManager.current
            val registerCurrentSet: () -> Unit = {
                val weight = weightText.replace(',', '.').toFloatOrNull()
                val reps = repsText.toIntOrNull()
                val rpe = rpeText.replace(',', '.').toFloatOrNull()
                    ?.coerceIn(1f, 10f)
                if (weight != null && reps != null && reps > 0) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    focusManager.clearFocus()
                    onRegisterSet(weight, reps, isWarmup, rpe)
                    repsText = ""
                    rpeText = ""
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(weightUnit.suffix) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rpeText,
                    onValueChange = { rpeText = it },
                    label = { Text("RPE") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { registerCurrentSet() }),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                FilterChip(
                    selected = isWarmup,
                    onClick = { isWarmup = !isWarmup },
                    label = { Text("Aq.") }
                )
                IconButton(
                    onClick = registerCurrentSet,
                    enabled = weightText.isNotBlank() && repsText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Registrar série",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
