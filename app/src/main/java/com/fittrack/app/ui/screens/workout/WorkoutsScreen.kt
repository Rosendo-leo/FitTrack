package com.fittrack.app.ui.screens.workout

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.local.entities.WorkoutTemplate
import com.fittrack.app.ui.common.label

@Composable
fun WorkoutsScreen(
    onOpenEditor: (Long) -> Unit,
    onOpenSession: (sessionId: Long) -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }

    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFromUri(it) { newId -> onOpenEditor(newId) } } }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { onOpenEditor(-1L) }) {
                    Icon(Icons.Default.Add, contentDescription = "Novo treino")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Treinos",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp)
                )
                IconButton(onClick = {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Importar treino")
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Meus treinos") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pré-definidos") }
                )
            }

            when (selectedTab) {
                0 -> TemplateList(
                    templates = state.myTemplates,
                    emptyMessage = "Nenhum treino ainda.\nCrie um no botão + ou copie um pré-definido.",
                    onClick = { onOpenEditor(it.id) },
                    trailing = { template ->
                        IconButton(onClick = {
                            viewModel.startSession(template.id) { sessionId ->
                                onOpenSession(sessionId)
                            }
                        }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Iniciar treino",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            viewModel.shareTemplate(template.id) { intent ->
                                context.startActivity(intent)
                            }
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { templateToDelete = template }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                1 -> TemplateList(
                    templates = state.presets,
                    emptyMessage = "Carregando treinos pré-definidos…",
                    onClick = { /* presets são somente leitura; copiar para editar */ },
                    trailing = { template ->
                        IconButton(onClick = {
                            viewModel.copyPresetToMine(template) { copyId -> onOpenEditor(copyId) }
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar para meus treinos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Excluir treino") },
            text = { Text("Excluir \"${template.name}\"? Os exercícios dele também serão removidos.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(template)
                    templateToDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TemplateList(
    templates: List<WorkoutTemplate>,
    emptyMessage: String,
    onClick: (WorkoutTemplate) -> Unit,
    trailing: @Composable (WorkoutTemplate) -> Unit
) {
    if (templates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(templates, key = { it.id }) { template ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onClick(template) }) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        template.description?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(template.category.label) }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(template.goal.label) }
                            )
                        }
                    }
                    trailing(template)
                }
            }
        }
    }
}
