package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.local.entities.BodyMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {

    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<BodyMetric?>

    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    suspend fun getAllOnce(): List<BodyMetric>

    @Query("SELECT * FROM body_metrics WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: Long, to: Long): Flow<List<BodyMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: BodyMetric): Long

    @Update
    suspend fun update(metric: BodyMetric)

    @Delete
    suspend fun delete(metric: BodyMetric)
}
