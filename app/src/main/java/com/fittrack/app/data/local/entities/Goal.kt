package com.fittrack.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: GoalMetric,
    val targetValue: Float,
    /** Valor da métrica no momento em que a meta foi criada, para calcular o progresso. */
    val startValue: Float,
    val createdAt: Long = System.currentTimeMillis(),
    val achievedAt: Long? = null
)
