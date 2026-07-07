package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import kotlinx.coroutines.flow.Flow

data class SessionWithTemplateName(
    @Embedded val session: WorkoutSession,
    val templateName: String?
)

data class SetWithExercise(
    @Embedded val set: SetRecord,
    val exerciseName: String,
    val muscleGroup: String
)

data class SetSample(
    val date: Long,
    val weightKg: Float,
    val reps: Int
)

data class SetExportRow(
    val startedAt: Long,
    val templateName: String?,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val rpe: Float?,
    val isWarmup: Boolean
)

data class ExercisePr(
    val exerciseName: String,
    val weightKg: Float,
    val reps: Int,
    val date: Long
)

@Dao
interface SessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getActiveSessionOnce(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL")
    suspend fun getFinishedSessionsOnce(): List<WorkoutSession>

    @Query("SELECT * FROM set_records WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber")
    suspend fun getSetsOnce(sessionId: Long): List<SetRecord>

    @Query("SELECT * FROM workout_sessions WHERE startedAt BETWEEN :from AND :to ORDER BY startedAt DESC")
    fun observeSessionsBetween(from: Long, to: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    // ── Sets ──
    @Query("SELECT * FROM set_records WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber")
    fun observeSets(sessionId: Long): Flow<List<SetRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetRecord): Long

    @Update
    suspend fun updateSet(set: SetRecord)

    @Query("DELETE FROM set_records WHERE id = :id")
    suspend fun deleteSet(id: Long)

    // Todas as séries de sessões finalizadas, para exportação CSV
    @Query(
        "SELECT s.startedAt AS startedAt, t.name AS templateName, " +
        "e.name AS exerciseName, sr.setNumber AS setNumber, sr.reps AS reps, " +
        "sr.weightKg AS weightKg, sr.rpe AS rpe, sr.isWarmup AS isWarmup " +
        "FROM set_records sr " +
        "JOIN exercises e ON sr.exerciseId = e.id " +
        "JOIN workout_sessions s ON sr.sessionId = s.id " +
        "LEFT JOIN workout_templates t ON s.templateId = t.id " +
        "WHERE s.finishedAt IS NOT NULL " +
        "ORDER BY s.startedAt, e.orderIndex, sr.setNumber"
    )
    suspend fun getAllSetsForExportOnce(): List<SetExportRow>

    // ── Histórico ──
    @Query(
        "SELECT s.*, t.name AS templateName FROM workout_sessions s " +
        "LEFT JOIN workout_templates t ON s.templateId = t.id " +
        "WHERE s.finishedAt IS NOT NULL ORDER BY s.startedAt DESC"
    )
    fun observeFinishedSessionsWithName(): Flow<List<SessionWithTemplateName>>

    @Query(
        "SELECT sr.*, e.name AS exerciseName, e.muscleGroup AS muscleGroup " +
        "FROM set_records sr JOIN exercises e ON sr.exerciseId = e.id " +
        "WHERE sr.sessionId = :sessionId ORDER BY e.orderIndex, sr.setNumber"
    )
    fun observeSetsWithExercise(sessionId: Long): Flow<List<SetWithExercise>>

    // MAX() em GROUP BY no SQLite retorna as demais colunas da linha do máximo
    @Query(
        "SELECT e.name AS exerciseName, MAX(sr.weightKg) AS weightKg, " +
        "sr.reps AS reps, s.startedAt AS date " +
        "FROM set_records sr " +
        "JOIN exercises e ON sr.exerciseId = e.id " +
        "JOIN workout_sessions s ON sr.sessionId = s.id " +
        "WHERE sr.isWarmup = 0 " +
        "GROUP BY e.name ORDER BY weightKg DESC"
    )
    fun observeExercisePrs(): Flow<List<ExercisePr>>

    // Nomes de exercícios que já têm séries válidas registradas (para progressão)
    @Query(
        "SELECT DISTINCT e.name FROM set_records sr " +
        "JOIN exercises e ON sr.exerciseId = e.id " +
        "WHERE sr.isWarmup = 0 ORDER BY e.name COLLATE NOCASE"
    )
    fun observeTrainedExerciseNames(): Flow<List<String>>

    // Séries válidas de um exercício (por nome, agregando templates duplicados)
    @Query(
        "SELECT s.startedAt AS date, sr.weightKg AS weightKg, sr.reps AS reps " +
        "FROM set_records sr " +
        "JOIN exercises e ON sr.exerciseId = e.id " +
        "JOIN workout_sessions s ON sr.sessionId = s.id " +
        "WHERE e.name = :exerciseName AND sr.isWarmup = 0 AND s.finishedAt IS NOT NULL " +
        "ORDER BY s.startedAt"
    )
    fun observeWorkingSetsFor(exerciseName: String): Flow<List<SetSample>>

    // Séries do exercício na sessão finalizada mais recente (exceto a sessão atual)
    @Query(
        "SELECT * FROM set_records WHERE exerciseId = :exerciseId AND sessionId = (" +
        "SELECT sr.sessionId FROM set_records sr " +
        "JOIN workout_sessions s ON sr.sessionId = s.id " +
        "WHERE sr.exerciseId = :exerciseId AND sr.sessionId != :excludeSessionId " +
        "AND s.finishedAt IS NOT NULL " +
        "ORDER BY s.startedAt DESC LIMIT 1" +
        ") ORDER BY setNumber"
    )
    suspend fun lastSetsForExercise(exerciseId: Long, excludeSessionId: Long): List<SetRecord>

    // Melhor carga registrada para um exercício (detecção de PR)
    @Query(
        "SELECT MAX(weightKg) FROM set_records " +
        "WHERE exerciseId = :exerciseId AND isWarmup = 0 AND reps >= :minReps"
    )
    suspend fun bestWeightFor(exerciseId: Long, minReps: Int = 1): Float?
}
