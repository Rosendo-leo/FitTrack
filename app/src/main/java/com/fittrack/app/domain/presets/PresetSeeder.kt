package com.fittrack.app.domain.presets

import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.WorkoutTemplate
import com.fittrack.app.data.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Insere os treinos pré-definidos na primeira execução do app. */
@Singleton
class PresetSeeder @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend fun seedIfNeeded() {
        if (repository.countPresets() > 0) return

        PresetWorkouts.all.forEach { preset ->
            val templateId = repository.saveTemplate(
                WorkoutTemplate(
                    name = preset.name,
                    description = preset.description,
                    category = preset.category,
                    goal = preset.goal,
                    isPreset = true
                )
            )
            repository.saveExercises(
                preset.exercises.mapIndexed { index, ex ->
                    Exercise(
                        templateId = templateId,
                        name = ex.name,
                        muscleGroup = ex.muscleGroup,
                        notes = ex.notes,
                        orderIndex = index
                    )
                }
            )
        }
    }
}
