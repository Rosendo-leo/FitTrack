package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.local.entities.CardioSession
import kotlinx.coroutines.flow.Flow

@Dao
interface CardioDao {

    @Query("SELECT * FROM cardio_sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<CardioSession>>

    @Query("SELECT * FROM cardio_sessions WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: Long, to: Long): Flow<List<CardioSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: CardioSession): Long

    @Update
    suspend fun update(session: CardioSession)

    @Delete
    suspend fun delete(session: CardioSession)
}
