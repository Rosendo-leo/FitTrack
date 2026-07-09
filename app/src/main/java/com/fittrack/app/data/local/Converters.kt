package com.fittrack.app.data.local

import androidx.room.TypeConverter
import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.local.entities.GoalMetric
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal

class Converters {
    @TypeConverter
    fun fromCategory(value: WorkoutCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): WorkoutCategory =
        WorkoutCategory.entries.firstOrNull { it.name == value } ?: WorkoutCategory.CUSTOM

    @TypeConverter
    fun fromGoal(value: WorkoutGoal): String = value.name

    @TypeConverter
    fun toGoal(value: String): WorkoutGoal =
        WorkoutGoal.entries.firstOrNull { it.name == value } ?: WorkoutGoal.GENERAL

    @TypeConverter
    fun fromCardioType(value: CardioType): String = value.name

    @TypeConverter
    fun toCardioType(value: String): CardioType =
        CardioType.entries.firstOrNull { it.name == value } ?: CardioType.OTHER

    @TypeConverter
    fun fromGoalMetric(value: GoalMetric): String = value.name

    @TypeConverter
    fun toGoalMetric(value: String): GoalMetric =
        GoalMetric.entries.firstOrNull { it.name == value } ?: GoalMetric.WEIGHT
}
