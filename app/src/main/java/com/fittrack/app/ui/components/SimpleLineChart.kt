package com.fittrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chartDateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))

private fun formatChartDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(chartDateFormatter)

/**
 * Gráfico de linha minimalista para séries temporais (peso, volume etc).
 * Pontos ordenados por x (timestamp). Desenha linha, área com gradiente
 * e destaca o último ponto.
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
    val gridColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "máx ${valueFormatter(maxValue)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 6.dp)
        ) {
            val minX = sorted.first().first.toFloat()
            val maxX = sorted.last().first.toFloat()
            val spanX = (maxX - minX).takeIf { it > 0f } ?: 1f
            // Margem vertical de 10% para a linha não encostar nas bordas
            val spanValue = (maxValue - minValue).takeIf { it > 0f } ?: 1f
            val lowValue = minValue - spanValue * 0.1f
            val highValue = maxValue + spanValue * 0.1f
            val spanY = highValue - lowValue

            fun toOffset(point: Pair<Long, Float>): Offset = Offset(
                x = (point.first - minX) / spanX * size.width,
                y = size.height - (point.second - lowValue) / spanY * size.height
            )

            // Linhas-guia horizontais
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            for (fraction in listOf(0f, 0.5f, 1f)) {
                val y = size.height * fraction
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = dash
                )
            }

            val linePath = Path()
            val areaPath = Path()
            sorted.forEachIndexed { index, point ->
                val offset = toOffset(point)
                if (index == 0) {
                    linePath.moveTo(offset.x, offset.y)
                    areaPath.moveTo(offset.x, size.height)
                    areaPath.lineTo(offset.x, offset.y)
                } else {
                    linePath.lineTo(offset.x, offset.y)
                    areaPath.lineTo(offset.x, offset.y)
                }
            }
            areaPath.lineTo(toOffset(sorted.last()).x, size.height)
            areaPath.close()

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f))
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = lineColor,
                radius = 10f,
                center = toOffset(sorted.last())
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
