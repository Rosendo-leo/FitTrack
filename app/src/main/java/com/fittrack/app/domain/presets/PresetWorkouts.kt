package com.fittrack.app.domain.presets

import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal

data class PresetExercise(val name: String, val muscleGroup: String, val notes: String? = null)

data class PresetWorkout(
    val name: String,
    val description: String,
    val category: WorkoutCategory,
    val goal: WorkoutGoal,
    val exercises: List<PresetExercise>
)

/** Treinos pré-definidos listados na esquemática: PPL, ABC/ABCD, 5x5 StrongLifts e Full Body. */
object PresetWorkouts {

    val all: List<PresetWorkout> = listOf(
        // ── PPL ──
        PresetWorkout(
            name = "PPL — Push",
            description = "Peito, ombros e tríceps. Parte 1 do split Push/Pull/Legs.",
            category = WorkoutCategory.PUSH,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Supino reto", "Peito"),
                PresetExercise("Supino inclinado com halteres", "Peito"),
                PresetExercise("Desenvolvimento militar", "Ombros"),
                PresetExercise("Elevação lateral", "Ombros"),
                PresetExercise("Tríceps testa", "Tríceps"),
                PresetExercise("Tríceps corda", "Tríceps")
            )
        ),
        PresetWorkout(
            name = "PPL — Pull",
            description = "Costas e bíceps. Parte 2 do split Push/Pull/Legs.",
            category = WorkoutCategory.PULL,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Barra fixa", "Costas"),
                PresetExercise("Remada curvada", "Costas"),
                PresetExercise("Puxada alta", "Costas"),
                PresetExercise("Remada baixa", "Costas"),
                PresetExercise("Rosca direta", "Bíceps"),
                PresetExercise("Rosca martelo", "Bíceps")
            )
        ),
        PresetWorkout(
            name = "PPL — Legs",
            description = "Pernas completas. Parte 3 do split Push/Pull/Legs.",
            category = WorkoutCategory.LEGS,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Agachamento livre", "Quadríceps"),
                PresetExercise("Leg press", "Quadríceps"),
                PresetExercise("Cadeira extensora", "Quadríceps"),
                PresetExercise("Mesa flexora", "Posteriores"),
                PresetExercise("Stiff", "Posteriores"),
                PresetExercise("Panturrilha em pé", "Panturrilhas")
            )
        ),

        // ── ABC ──
        PresetWorkout(
            name = "ABC — Treino A",
            description = "Peito e tríceps. Split clássico de 3 dias.",
            category = WorkoutCategory.PUSH,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Supino reto", "Peito"),
                PresetExercise("Supino inclinado", "Peito"),
                PresetExercise("Crucifixo", "Peito"),
                PresetExercise("Paralelas", "Tríceps"),
                PresetExercise("Tríceps francês", "Tríceps")
            )
        ),
        PresetWorkout(
            name = "ABC — Treino B",
            description = "Costas e bíceps. Split clássico de 3 dias.",
            category = WorkoutCategory.PULL,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Puxada alta", "Costas"),
                PresetExercise("Remada curvada", "Costas"),
                PresetExercise("Pulldown", "Costas"),
                PresetExercise("Rosca direta", "Bíceps"),
                PresetExercise("Rosca alternada", "Bíceps")
            )
        ),
        PresetWorkout(
            name = "ABC — Treino C",
            description = "Pernas e ombros. Split clássico de 3 dias.",
            category = WorkoutCategory.LEGS,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Agachamento livre", "Quadríceps"),
                PresetExercise("Leg press", "Quadríceps"),
                PresetExercise("Mesa flexora", "Posteriores"),
                PresetExercise("Desenvolvimento com halteres", "Ombros"),
                PresetExercise("Elevação lateral", "Ombros")
            )
        ),

        // ── ABCD ──
        PresetWorkout(
            name = "ABCD — Treino A",
            description = "Peito. Split de 4 dias com maior volume por grupo.",
            category = WorkoutCategory.PUSH,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Supino reto", "Peito"),
                PresetExercise("Supino inclinado com halteres", "Peito"),
                PresetExercise("Crucifixo inclinado", "Peito"),
                PresetExercise("Cross over", "Peito"),
                PresetExercise("Paralelas", "Peito")
            )
        ),
        PresetWorkout(
            name = "ABCD — Treino B",
            description = "Costas. Split de 4 dias com maior volume por grupo.",
            category = WorkoutCategory.PULL,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Barra fixa", "Costas"),
                PresetExercise("Remada curvada", "Costas"),
                PresetExercise("Puxada alta", "Costas"),
                PresetExercise("Remada unilateral", "Costas"),
                PresetExercise("Pullover", "Costas")
            )
        ),
        PresetWorkout(
            name = "ABCD — Treino C",
            description = "Ombros e braços. Split de 4 dias com maior volume por grupo.",
            category = WorkoutCategory.UPPER,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Desenvolvimento militar", "Ombros"),
                PresetExercise("Elevação lateral", "Ombros"),
                PresetExercise("Elevação posterior", "Ombros"),
                PresetExercise("Rosca direta", "Bíceps"),
                PresetExercise("Tríceps corda", "Tríceps")
            )
        ),
        PresetWorkout(
            name = "ABCD — Treino D",
            description = "Pernas. Split de 4 dias com maior volume por grupo.",
            category = WorkoutCategory.LEGS,
            goal = WorkoutGoal.HYPERTROPHY,
            exercises = listOf(
                PresetExercise("Agachamento livre", "Quadríceps"),
                PresetExercise("Leg press", "Quadríceps"),
                PresetExercise("Cadeira extensora", "Quadríceps"),
                PresetExercise("Stiff", "Posteriores"),
                PresetExercise("Mesa flexora", "Posteriores"),
                PresetExercise("Panturrilha sentado", "Panturrilhas")
            )
        ),

        // ── 5x5 StrongLifts ──
        PresetWorkout(
            name = "5x5 — Treino A",
            description = "StrongLifts 5x5: agachamento, supino e remada. 5 séries de 5 reps.",
            category = WorkoutCategory.FULL_BODY,
            goal = WorkoutGoal.STRENGTH,
            exercises = listOf(
                PresetExercise("Agachamento livre", "Quadríceps", "5x5"),
                PresetExercise("Supino reto", "Peito", "5x5"),
                PresetExercise("Remada curvada", "Costas", "5x5")
            )
        ),
        PresetWorkout(
            name = "5x5 — Treino B",
            description = "StrongLifts 5x5: agachamento, desenvolvimento e levantamento terra.",
            category = WorkoutCategory.FULL_BODY,
            goal = WorkoutGoal.STRENGTH,
            exercises = listOf(
                PresetExercise("Agachamento livre", "Quadríceps", "5x5"),
                PresetExercise("Desenvolvimento militar", "Ombros", "5x5"),
                PresetExercise("Levantamento terra", "Posteriores", "1x5")
            )
        ),

        // ── Full Body ──
        PresetWorkout(
            name = "Full Body 3x/semana",
            description = "Corpo inteiro em uma sessão, ideal para 3 treinos por semana.",
            category = WorkoutCategory.FULL_BODY,
            goal = WorkoutGoal.GENERAL,
            exercises = listOf(
                PresetExercise("Agachamento livre", "Quadríceps"),
                PresetExercise("Supino reto", "Peito"),
                PresetExercise("Remada curvada", "Costas"),
                PresetExercise("Desenvolvimento militar", "Ombros"),
                PresetExercise("Rosca direta", "Bíceps"),
                PresetExercise("Tríceps corda", "Tríceps"),
                PresetExercise("Prancha", "Core", "3x 45s")
            )
        )
    )
}
