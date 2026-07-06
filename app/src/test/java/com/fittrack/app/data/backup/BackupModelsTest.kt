package com.fittrack.app.data.backup

import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sample = BackupData(
        appVersion = "1.0.0",
        exportedAt = 1_700_000_000_000,
        templates = listOf(
            BackupTemplate(
                id = 1, name = "Push", description = "Peito/ombro/tríceps",
                category = WorkoutCategory.PUSH, goal = WorkoutGoal.HYPERTROPHY,
                isPreset = false, createdAt = 1_690_000_000_000
            )
        ),
        exercises = listOf(
            BackupExercise(id = 10, templateId = 1, name = "Supino", muscleGroup = "Peito")
        ),
        sessions = listOf(
            BackupSession(id = 100, templateId = 1, startedAt = 1_699_000_000_000, finishedAt = 1_699_003_600_000, totalVolume = 4200f)
        ),
        sets = listOf(
            BackupSet(id = 1000, sessionId = 100, exerciseId = 10, setNumber = 1, reps = 8, weightKg = 80f, rpe = 8.5f)
        ),
        bodyMetrics = listOf(
            BackupBodyMetric(id = 1, date = 1_699_000_000_000, weightKg = 82.5f, bodyFatPct = 18f)
        ),
        cardioSessions = listOf(
            BackupCardio(id = 1, type = CardioType.RUNNING, durationMin = 30, distanceKm = 5f, date = 1_699_000_000_000)
        )
    )

    @Test
    fun `roundtrip json preserva todos os campos`() {
        val encoded = json.encodeToString(BackupData.serializer(), sample)
        val decoded = json.decodeFromString(BackupData.serializer(), encoded)
        assertEquals(sample, decoded)
    }

    @Test
    fun `schema version padrao acompanha a constante`() {
        assertEquals(BACKUP_SCHEMA_VERSION, sample.schemaVersion)
    }

    @Test
    fun `campos opcionais ausentes usam defaults`() {
        // Simula um backup antigo/mínimo: campos opcionais omitidos
        val minimal = """
            {"appVersion":"0.9.0","exportedAt":1,
             "templates":[{"id":1,"name":"A","createdAt":1}],
             "exercises":[],"sessions":[],"sets":[],"bodyMetrics":[],"cardioSessions":[]}
        """.trimIndent()
        val decoded = json.decodeFromString(BackupData.serializer(), minimal)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(WorkoutCategory.CUSTOM, decoded.templates.single().category)
        assertEquals(false, decoded.templates.single().isPreset)
    }

    @Test
    fun `toEntity remapeia ids no merge`() {
        val exercise = sample.exercises.single().toEntity(id = 0, templateId = 55)
        assertEquals(0L, exercise.id)
        assertEquals(55L, exercise.templateId)
        assertEquals("Supino", exercise.name)
    }
}
