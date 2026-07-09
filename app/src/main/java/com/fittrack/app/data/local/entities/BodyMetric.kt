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
    val armFlexedLeftCm: Float? = null,
    val armFlexedRightCm: Float? = null,
    val armRelaxedLeftCm: Float? = null,
    val armRelaxedRightCm: Float? = null,
    val shoulderCm: Float? = null,
    val thighLeftCm: Float? = null,
    val thighRightCm: Float? = null,
    val forearmLeftCm: Float? = null,
    val forearmRightCm: Float? = null,
    val calfLeftCm: Float? = null,
    val calfRightCm: Float? = null,
    val notes: String? = null
)
