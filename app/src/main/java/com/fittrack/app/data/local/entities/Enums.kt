package com.fittrack.app.data.local.entities

enum class WorkoutCategory {
    PUSH, PULL, LEGS, UPPER, LOWER, FULL_BODY, CUSTOM
}

enum class WorkoutGoal {
    HYPERTROPHY, STRENGTH, ENDURANCE, WEIGHT_LOSS, GENERAL
}

enum class CardioType {
    RUNNING, CYCLING, SWIMMING, WALKING, ROWING, ELLIPTICAL, HIIT, OTHER
}

/** Métrica que pode ter uma meta (peso ou uma medida corporal em cm). */
enum class GoalMetric {
    WEIGHT, WAIST, CHEST, SHOULDER,
    ARM_FLEXED_LEFT, ARM_FLEXED_RIGHT, ARM_RELAXED_LEFT, ARM_RELAXED_RIGHT,
    FOREARM_LEFT, FOREARM_RIGHT, THIGH_LEFT, THIGH_RIGHT, CALF_LEFT, CALF_RIGHT
}
