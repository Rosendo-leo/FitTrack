package com.fittrack.app.data.backup

import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.local.entities.WorkoutTemplate
import kotlinx.serialization.Serializable

/** Versão do formato de backup; incrementar quando o JSON mudar de forma incompatível. */
const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupData(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val appVersion: String,
    val exportedAt: Long,
    val templates: List<BackupTemplate>,
    val exercises: List<BackupExercise>,
    val sessions: List<BackupSession>,
    val sets: List<BackupSet>,
    val bodyMetrics: List<BackupBodyMetric>,
    val cardioSessions: List<BackupCardio>
)

@Serializable
data class BackupTemplate(
    val id: Long,
    val name: String,
    val description: String? = null,
    val category: WorkoutCategory = WorkoutCategory.CUSTOM,
    val goal: WorkoutGoal = WorkoutGoal.GENERAL,
    val isPreset: Boolean = false,
    val createdAt: Long
) {
    fun toEntity(id: Long = this.id) = WorkoutTemplate(
        id = id, name = name, description = description,
        category = category, goal = goal, isPreset = isPreset, createdAt = createdAt
    )

    companion object {
        fun from(e: WorkoutTemplate) = BackupTemplate(
            id = e.id, name = e.name, description = e.description,
            category = e.category, goal = e.goal, isPreset = e.isPreset, createdAt = e.createdAt
        )
    }
}

@Serializable
data class BackupExercise(
    val id: Long,
    val templateId: Long,
    val name: String,
    val muscleGroup: String,
    val notes: String? = null,
    val orderIndex: Int = 0
) {
    fun toEntity(id: Long = this.id, templateId: Long = this.templateId) = Exercise(
        id = id, templateId = templateId, name = name,
        muscleGroup = muscleGroup, notes = notes, orderIndex = orderIndex
    )

    companion object {
        fun from(e: Exercise) = BackupExercise(
            id = e.id, templateId = e.templateId, name = e.name,
            muscleGroup = e.muscleGroup, notes = e.notes, orderIndex = e.orderIndex
        )
    }
}

@Serializable
data class BackupSession(
    val id: Long,
    val templateId: Long? = null,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val totalVolume: Float = 0f,
    val notes: String? = null
) {
    fun toEntity(id: Long = this.id, templateId: Long? = this.templateId) = WorkoutSession(
        id = id, templateId = templateId, startedAt = startedAt,
        finishedAt = finishedAt, totalVolume = totalVolume, notes = notes
    )

    companion object {
        fun from(e: WorkoutSession) = BackupSession(
            id = e.id, templateId = e.templateId, startedAt = e.startedAt,
            finishedAt = e.finishedAt, totalVolume = e.totalVolume, notes = e.notes
        )
    }
}

@Serializable
data class BackupSet(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val rpe: Float? = null,
    val isWarmup: Boolean = false
) {
    fun toEntity(id: Long = this.id, sessionId: Long = this.sessionId, exerciseId: Long = this.exerciseId) =
        SetRecord(
            id = id, sessionId = sessionId, exerciseId = exerciseId, setNumber = setNumber,
            reps = reps, weightKg = weightKg, rpe = rpe, isWarmup = isWarmup
        )

    companion object {
        fun from(e: SetRecord) = BackupSet(
            id = e.id, sessionId = e.sessionId, exerciseId = e.exerciseId, setNumber = e.setNumber,
            reps = e.reps, weightKg = e.weightKg, rpe = e.rpe, isWarmup = e.isWarmup
        )
    }
}

@Serializable
data class BackupBodyMetric(
    val id: Long,
    val date: Long,
    val weightKg: Float,
    val bodyFatPct: Float? = null,
    val waistCm: Float? = null,
    val armCm: Float? = null,
    val chestCm: Float? = null,
    val notes: String? = null
) {
    fun toEntity(id: Long = this.id) = BodyMetric(
        id = id, date = date, weightKg = weightKg, bodyFatPct = bodyFatPct,
        waistCm = waistCm, armCm = armCm, chestCm = chestCm, notes = notes
    )

    companion object {
        fun from(e: BodyMetric) = BackupBodyMetric(
            id = e.id, date = e.date, weightKg = e.weightKg, bodyFatPct = e.bodyFatPct,
            waistCm = e.waistCm, armCm = e.armCm, chestCm = e.chestCm, notes = e.notes
        )
    }
}

@Serializable
data class BackupCardio(
    val id: Long,
    val type: CardioType,
    val durationMin: Int,
    val distanceKm: Float? = null,
    val calories: Int? = null,
    val avgHeartRate: Int? = null,
    val date: Long
) {
    fun toEntity(id: Long = this.id) = CardioSession(
        id = id, type = type, durationMin = durationMin, distanceKm = distanceKm,
        calories = calories, avgHeartRate = avgHeartRate, date = date
    )

    companion object {
        fun from(e: CardioSession) = BackupCardio(
            id = e.id, type = e.type, durationMin = e.durationMin, distanceKm = e.distanceKm,
            calories = e.calories, avgHeartRate = e.avgHeartRate, date = e.date
        )
    }
}

/** O que fazer com os dados locais ao restaurar um backup. */
enum class RestoreMode {
    /** Apaga tudo e restaura exatamente o conteúdo do backup. */
    REPLACE,

    /** Mantém os dados locais e adiciona o que existe só no backup (dedup por data/nome). */
    MERGE
}

/** Resumo do restore para exibir na UI. */
data class RestoreSummary(
    val templates: Int,
    val sessions: Int,
    val bodyMetrics: Int,
    val cardioSessions: Int
)
