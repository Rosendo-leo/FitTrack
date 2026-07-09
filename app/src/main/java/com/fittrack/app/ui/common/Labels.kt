package com.fittrack.app.ui.common

import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioType
import com.fittrack.app.data.local.entities.Goal
import com.fittrack.app.data.local.entities.GoalMetric
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

val GoalMetric.label: String
    get() = when (this) {
        GoalMetric.WEIGHT -> "Peso"
        GoalMetric.WAIST -> "Cintura"
        GoalMetric.CHEST -> "Peito"
        GoalMetric.SHOULDER -> "Ombro"
        GoalMetric.ARM_FLEXED_LEFT -> "Braço contraído esquerdo"
        GoalMetric.ARM_FLEXED_RIGHT -> "Braço contraído direito"
        GoalMetric.ARM_RELAXED_LEFT -> "Braço relaxado esquerdo"
        GoalMetric.ARM_RELAXED_RIGHT -> "Braço relaxado direito"
        GoalMetric.FOREARM_LEFT -> "Antebraço esquerdo"
        GoalMetric.FOREARM_RIGHT -> "Antebraço direito"
        GoalMetric.THIGH_LEFT -> "Coxa esquerda"
        GoalMetric.THIGH_RIGHT -> "Coxa direita"
        GoalMetric.CALF_LEFT -> "Panturrilha esquerda"
        GoalMetric.CALF_RIGHT -> "Panturrilha direita"
    }

/** "kg" para peso, "cm" para as demais medidas. */
val GoalMetric.unitSuffix: String get() = if (this == GoalMetric.WEIGHT) "kg" else "cm"

/** Extrai o valor dessa métrica de um registro de medida corporal, se presente. */
fun GoalMetric.valueIn(metric: BodyMetric): Float? = when (this) {
    GoalMetric.WEIGHT -> metric.weightKg
    GoalMetric.WAIST -> metric.waistCm
    GoalMetric.CHEST -> metric.chestCm
    GoalMetric.SHOULDER -> metric.shoulderCm
    GoalMetric.ARM_FLEXED_LEFT -> metric.armFlexedLeftCm
    GoalMetric.ARM_FLEXED_RIGHT -> metric.armFlexedRightCm
    GoalMetric.ARM_RELAXED_LEFT -> metric.armRelaxedLeftCm
    GoalMetric.ARM_RELAXED_RIGHT -> metric.armRelaxedRightCm
    GoalMetric.FOREARM_LEFT -> metric.forearmLeftCm
    GoalMetric.FOREARM_RIGHT -> metric.forearmRightCm
    GoalMetric.THIGH_LEFT -> metric.thighLeftCm
    GoalMetric.THIGH_RIGHT -> metric.thighRightCm
    GoalMetric.CALF_LEFT -> metric.calfLeftCm
    GoalMetric.CALF_RIGHT -> metric.calfRightCm
}

/** A meta é de reduzir (alvo abaixo do valor inicial) ou de aumentar? */
fun Goal.isAchieved(current: Float?): Boolean {
    if (current == null) return false
    return if (targetValue <= startValue) current <= targetValue else current >= targetValue
}

/** Fração de progresso (0..1) entre o valor inicial e o alvo. */
fun Goal.progressFraction(current: Float?): Float {
    if (current == null) return 0f
    val total = targetValue - startValue
    if (total == 0f) return 1f
    return ((current - startValue) / total).coerceIn(0f, 1f)
}
