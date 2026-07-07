package com.fittrack.app.data.export

import com.fittrack.app.data.repository.MetricsRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

/** Envolve o campo em aspas se contiver vírgula, aspas ou quebra de linha (RFC 4180). */
private fun String.csvEscape(): String =
    if (any { it == ',' || it == '"' || it == '\n' }) "\"${replace("\"", "\"\"")}\"" else this

private fun row(vararg fields: Any?): String =
    fields.joinToString(",") { (it?.toString() ?: "").csvEscape() }

@Singleton
class CsvExporter @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val metricsRepository: MetricsRepository
) {
    /** Uma linha por série registrada em treinos finalizados. */
    suspend fun exportWorkoutSets(): String {
        val sets = workoutRepository.getAllSetsForExport()
        val header = row(
            "data", "treino", "exercicio", "serie", "reps", "peso_kg", "rpe", "aquecimento"
        )
        val lines = sets.map {
            row(
                csvDateFormat.format(Date(it.startedAt)),
                it.templateName ?: "Treino livre",
                it.exerciseName,
                it.setNumber,
                it.reps,
                it.weightKg,
                it.rpe ?: "",
                if (it.isWarmup) "sim" else "não"
            )
        }
        return (listOf(header) + lines).joinToString("\n")
    }

    /** Uma linha por medida corporal e por sessão de cardio, em arquivos separados. */
    suspend fun exportBodyMetrics(): String {
        val metrics = metricsRepository.observeAllMetrics().first().sortedBy { it.date }
        val header = row(
            "data", "peso_kg", "gordura_pct", "cintura_cm", "braco_cm", "peito_cm", "notas"
        )
        val lines = metrics.map {
            row(
                csvDateFormat.format(Date(it.date)),
                it.weightKg,
                it.bodyFatPct ?: "",
                it.waistCm ?: "",
                it.armCm ?: "",
                it.chestCm ?: "",
                it.notes ?: ""
            )
        }
        return (listOf(header) + lines).joinToString("\n")
    }
}
