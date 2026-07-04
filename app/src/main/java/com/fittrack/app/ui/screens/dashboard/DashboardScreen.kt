package com.fittrack.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Peso atual", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = state.latestWeight?.let { "%.1f kg".format(it.weightKg) }
                        ?: "Nenhum registro ainda",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Treinos recentes", style = MaterialTheme.typography.labelSmall)
                if (state.recentSessions.isEmpty()) {
                    Text(
                        "Nenhum treino registrado — comece na aba Treinos!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    state.recentSessions.forEach { session ->
                        Text(
                            "Sessão #${session.id} — volume %.0f kg".format(session.totalVolume),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
