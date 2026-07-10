package com.fittrack.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chartDateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))

private fun formatChartDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(chartDateFormatter)

/**
 * Gráfico de linha minimalista para séries temporais (peso, volume etc), via Vico.
 * Pontos ordenados por x (timestamp). Min/máx e datas extremas ficam em texto ao
 * redor do gráfico (como no componente Canvas original), então os eixos do Vico
 * ficam ocultos — só a linha e a área com gradiente aparecem.
 */
@Composable
fun SimpleLineChart(
    points: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
    valueFormatter: (Float) -> String = { "%.1f".format(it) }
) {
    if (points.size < 2) {
        Text(
            "Registre pelo menos dois valores para ver o gráfico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val sorted = points.sortedBy { it.first }
    val minValue = sorted.minOf { it.second }
    val maxValue = sorted.maxOf { it.second }
    val lineColor = MaterialTheme.colorScheme.primary

    val modelProducer = remember { CartesianChartModelProducer() }
    // Dias desde a época (não milissegundos) para manter os valores de x num range
    // pequeno — mais amigável para o Vico escalar o eixo internamente.
    val xValues = remember(sorted) { sorted.map { it.first / 86_400_000.0 } }
    val yValues = remember(sorted) { sorted.map { it.second.toDouble() } }

    LaunchedEffect(xValues, yValues) {
        modelProducer.runTransaction {
            lineSeries { series(xValues, yValues) }
        }
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "máx ${valueFormatter(maxValue)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ProvideVicoTheme(rememberM3VicoTheme()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            rememberLine(fill = LineCartesianLayer.LineFill.single(fill(lineColor)))
                        )
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(vertical = 6.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatChartDate(sorted.first().first),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "mín ${valueFormatter(minValue)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatChartDate(sorted.last().first),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
