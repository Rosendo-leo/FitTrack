package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplate,
    @Relation(parentColumn = "id", entityColumn = "templateId")
    val exercises: List<Exercise>
)

@Dao
interface WorkoutDao {

    // ── Templates ──
    @Query("SELECT * FROM workout_templates WHERE isPreset = 0 ORDER BY createdAt DESC")
    fun observeMyTemplates(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE isPreset = 1 ORDER BY name")
    fun observePresetTemplates(): Flow<List<WorkoutTemplate>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    fun observeTemplateWithExercises(id: Long): Flow<TemplateWithExercises?>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplate(id: Long): WorkoutTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)

    // ── Exercises ──
    @Query("SELECT * FROM exercises WHERE templateId = :templateId ORDER BY orderIndex")
    fun observeExercises(templateId: Long): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM workout_templates WHERE isPreset = 1")
    suspend fun countPresets(): Int
}
