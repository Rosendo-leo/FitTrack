package com.fittrack.app.data.share

import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal
import com.fittrack.app.data.local.entities.WorkoutTemplate
import kotlinx.serialization.Serializable

/** Versão do formato de treino compartilhado; incrementar se o JSON mudar de forma incompatível. */
const val SHARED_WORKOUT_SCHEMA_VERSION = 1

@Serializable
data class SharedWorkout(
    val schemaVersion: Int = SHARED_WORKOUT_SCHEMA_VERSION,
    val appVersion: String,
    val exportedAt: Long,
    val template: SharedTemplate,
    val exercises: List<SharedExercise>
)

@Serializable
data class SharedTemplate(
    val name: String,
    val description: String? = null,
    val category: WorkoutCategory = WorkoutCategory.CUSTOM,
    val goal: WorkoutGoal = WorkoutGoal.GENERAL
) {
    fun toEntity() = WorkoutTemplate(
        name = name, description = description, category = category, goal = goal
    )

    companion object {
        fun from(e: WorkoutTemplate) = SharedTemplate(
            name = e.name, description = e.description, category = e.category, goal = e.goal
        )
    }
}

@Serializable
data class SharedExercise(
    val name: String,
    val muscleGroup: String,
    val notes: String? = null,
    val orderIndex: Int = 0,
    val restSeconds: Int? = null
) {
    fun toEntity(templateId: Long) = Exercise(
        templateId = templateId, name = name, muscleGroup = muscleGroup,
        notes = notes, orderIndex = orderIndex, restSeconds = restSeconds
    )

    companion object {
        fun from(e: Exercise) = SharedExercise(
            name = e.name, muscleGroup = e.muscleGroup, notes = e.notes,
            orderIndex = e.orderIndex, restSeconds = e.restSeconds
        )
    }
}
