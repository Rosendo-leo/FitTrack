package com.fittrack.app.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Host do fluxo de atualização — renderiza o dialog conforme o estado. */
@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val current = state) {
        is UpdateUiState.Hidden -> Unit

        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Nova versão disponível 🚀") },
            text = {
                Column {
                    Text(
                        "FitTrack v${current.info.versionName} · %.1f MB"
                            .format(current.info.apkSizeBytes / 1024f / 1024f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        current.info.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::startDownload) { Text("Atualizar agora") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text("Depois") }
            }
        )

        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = { /* não fecha durante o download */ },
            title = { Text("Baixando atualização…") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { current.percent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${current.percent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {}
        )

        is UpdateUiState.ReadyToInstall -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Pronto para instalar ✓") },
            text = {
                Text(
                    "Download verificado (SHA256 ok). Seus treinos e dados são preservados — " +
                        "a atualização substitui apenas o app."
                )
            },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(viewModel.installIntent(current.file))
                }) { Text("Instalar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text("Cancelar") }
            }
        )

        is UpdateUiState.NeedsInstallPermission -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Permissão necessária") },
            text = {
                Text(
                    "Para instalar a atualização, autorize o FitTrack a instalar apps " +
                        "na tela que vai abrir e depois volte aqui."
                )
            },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(viewModel.installPermissionIntent())
                }) { Text("Autorizar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text("Cancelar") }
            }
        )

        is UpdateUiState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Falha na atualização") },
            text = { Text(current.message) },
            confirmButton = {
                Button(onClick = viewModel::startDownload) { Text("Tentar de novo") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text("Fechar") }
            }
        )
    }
}
