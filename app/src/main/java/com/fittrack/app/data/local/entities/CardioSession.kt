package com.fittrack.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cardio_sessions",
    indices = [Index("date")]
)
data class CardioSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CardioType,
    val durationMin: Int,
    val distanceKm: Float? = null,
    val calories: Int? = null,
    val avgHeartRate: Int? = null,
    val date: Long = System.currentTimeMillis()
)
