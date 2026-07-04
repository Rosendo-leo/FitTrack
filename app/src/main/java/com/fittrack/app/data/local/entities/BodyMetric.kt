package com.fittrack.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_metrics",
    indices = [Index("date")]
)
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val bodyFatPct: Float? = null,
    val waistCm: Float? = null,
    val armCm: Float? = null,
    val chestCm: Float? = null,
    val notes: String? = null
)
