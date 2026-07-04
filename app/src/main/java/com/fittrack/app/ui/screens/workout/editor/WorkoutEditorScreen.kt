package com.fittrack.app.ui.screens.workout.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal
import com.fittrack.app.ui.common.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    onBack: () -> Unit,
    viewModel: WorkoutEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var exerciseDialogTarget by remember { mutableStateOf<EditorExercise?>(null) }
    var showNewExerciseDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.templateId == null) "Novo treino" else "Editar treino") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = state.canSave) {
                        Text("Salvar")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("Nome do treino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    label = { Text("Descrição (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnumDropdown(
                        label = "Categoria",
                        options = WorkoutCategory.entries,
                        selected = state.category,
                        optionLabel = { it.label },
                        onSelect = viewModel::setCategory,
                        modifier = Modifier.weight(1f)
                    )
                    EnumDropdown(
                        label = "Objetivo",
                        options = WorkoutGoal.entries,
                        selected = state.goal,
                        optionLabel = { it.label },
                        onSelect = viewModel::setGoal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exercícios", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showNewExerciseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Adicionar")
                    }
                }
            }
            if (state.exercises.isEmpty()) {
                item {
                    Text(
                        "Nenhum exercício. Toque em Adicionar para começar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.exercises, key = { it.localKey }) { exercise ->
                val index = state.exercises.indexOf(exercise)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        ) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(exercise.muscleGroup.ifBlank { null }, exercise.notes)
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.moveExercise(exercise.localKey, -1) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Mover para cima")
                        }
                        IconButton(
                            onClick = { viewModel.moveExercise(exercise.localKey, +1) },
                            enabled = index < state.exercises.lastIndex
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mover para baixo")
                        }
                        IconButton(onClick = { exerciseDialogTarget = exercise }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remover exercício",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewExerciseDialog) {
        ExerciseDialog(
            title = "Novo exercício",
            onDismiss = { showNewExerciseDialog = false },
            onConfirm = { name, group, notes ->
                viewModel.addExercise(name, group, notes)
                showNewExerciseDialog = false
            }
        )
    }

    exerciseDialogTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { exerciseDialogTarget = null },
            title = { Text("Remover exercício") },
            text = { Text("Remover \"${target.name}\" deste treino?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeExercise(target.localKey)
                    exerciseDialogTarget = null
                }) { Text("Remover", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { exerciseDialogTarget = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscleGroup: String, notes: String?) -> Unit,
    initialName: String = "",
    initialGroup: String = "",
    initialNotes: String = ""
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var group by rememberSaveable { mutableStateOf(initialGroup) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do exercício") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Grupo muscular") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (ex: 4x10)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, group, notes.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
