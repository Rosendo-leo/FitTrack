package com.fittrack.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val category: WorkoutCategory = WorkoutCategory.CUSTOM,
    val goal: WorkoutGoal = WorkoutGoal.GENERAL,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
