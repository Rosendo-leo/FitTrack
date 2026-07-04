package com.fittrack.app.ui.common

import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.local.entities.WorkoutCategory
import com.fittrack.app.data.local.entities.WorkoutGoal

val WorkoutCategory.label: String
    get() = when (this) {
        WorkoutCategory.PUSH -> "Push"
        WorkoutCategory.PULL -> "Pull"
        WorkoutCategory.LEGS -> "Pernas"
        WorkoutCategory.UPPER -> "Superiores"
        WorkoutCategory.LOWER -> "Inferiores"
        WorkoutCategory.FULL_BODY -> "Full Body"
        WorkoutCategory.CUSTOM -> "Personalizado"
    }

val WorkoutGoal.label: String
    get() = when (this) {
        WorkoutGoal.HYPERTROPHY -> "Hipertrofia"
        WorkoutGoal.STRENGTH -> "Força"
        WorkoutGoal.ENDURANCE -> "Resistência"
        WorkoutGoal.WEIGHT_LOSS -> "Emagrecimento"
        WorkoutGoal.GENERAL -> "Geral"
    }

val CardioType.label: String
    get() = when (this) {
        CardioType.RUNNING -> "Corrida"
        CardioType.CYCLING -> "Bike"
        CardioType.SWIMMING -> "Natação"
        CardioType.WALKING -> "Caminhada"
        CardioType.ROWING -> "Remo"
        CardioType.ELLIPTICAL -> "Elíptico"
        CardioType.HIIT -> "HIIT"
        CardioType.OTHER -> "Outro"
    }
