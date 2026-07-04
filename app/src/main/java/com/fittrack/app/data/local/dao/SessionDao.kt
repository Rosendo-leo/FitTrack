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

    // Melhor carga registrada para um exercício (detecção de PR)
    @Query(
        "SELECT MAX(weightKg) FROM set_records " +
        "WHERE exerciseId = :exerciseId AND isWarmup = 0 AND reps >= :minReps"
    )
    suspend fun bestWeightFor(exerciseId: Long, minReps: Int = 1): Float?
}
