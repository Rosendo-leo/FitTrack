package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSession?>

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

    // Melhor carga registrada para um exercício (detecção de PR)
    @Query(
        "SELECT MAX(weightKg) FROM set_records " +
        "WHERE exerciseId = :exerciseId AND isWarmup = 0 AND reps >= :minReps"
    )
    suspend fun bestWeightFor(exerciseId: Long, minReps: Int = 1): Float?
}
