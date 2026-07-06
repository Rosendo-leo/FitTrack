package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.local.entities.WorkoutTemplate

/** Acesso bruto às tabelas para export/import de backup. */
@Dao
interface BackupDao {

    // ── Leitura completa ──
    @Query("SELECT * FROM workout_templates")
    suspend fun allTemplates(): List<WorkoutTemplate>

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<Exercise>

    @Query("SELECT * FROM workout_sessions")
    suspend fun allSessions(): List<WorkoutSession>

    @Query("SELECT * FROM set_records")
    suspend fun allSets(): List<SetRecord>

    @Query("SELECT * FROM body_metrics")
    suspend fun allBodyMetrics(): List<BodyMetric>

    @Query("SELECT * FROM cardio_sessions")
    suspend fun allCardioSessions(): List<CardioSession>

    // ── Limpeza (ordem respeita as FKs) ──
    @Query("DELETE FROM set_records")
    suspend fun clearSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM workout_templates")
    suspend fun clearTemplates()

    @Query("DELETE FROM body_metrics")
    suspend fun clearBodyMetrics()

    @Query("DELETE FROM cardio_sessions")
    suspend fun clearCardioSessions()

    // ── Inserção (id explícito é preservado; id=0 gera novo) ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetric(metric: BodyMetric): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardioSession(session: CardioSession): Long

    // ── Dedup no merge ──
    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE startedAt = :startedAt)")
    suspend fun sessionExistsAt(startedAt: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM body_metrics WHERE date = :date)")
    suspend fun bodyMetricExistsAt(date: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM cardio_sessions WHERE date = :date AND type = :type)")
    suspend fun cardioExistsAt(date: Long, type: String): Boolean

    @Query("SELECT id FROM workout_templates WHERE name = :name AND isPreset = :isPreset LIMIT 1")
    suspend fun templateIdByName(name: String, isPreset: Boolean): Long?

    @Query("SELECT * FROM exercises WHERE templateId = :templateId")
    suspend fun exercisesOf(templateId: Long): List<Exercise>
}
