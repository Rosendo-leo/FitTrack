package com.fittrack.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.fittrack.app.data.local.dao.MetricDao
import com.fittrack.app.data.local.dao.SessionDao
import com.fittrack.app.data.local.dao.WorkoutDao
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.data.preferences.WeightUnit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.fittrack.app.domain.currentStreak
import com.fittrack.app.domain.weekTrainedFlags
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun workoutDao(): WorkoutDao
    fun sessionDao(): SessionDao
    fun metricDao(): MetricDao
    fun userPreferencesRepository(): UserPreferencesRepository
}

internal fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)

// ── Modelos de dados por widget ──

data class WorkoutDayData(
    val templateId: Long?,
    val templateName: String?,
    val exerciseCount: Int
)

data class WeightData(
    val weightKg: Float?,
    val weekDeltaKg: Float?,
    val weightUnit: WeightUnit = WeightUnit.KG
)

data class WeeklyData(val weekDays: List<Boolean>, val streakDays: Int)

data class ActiveSessionData(
    val active: Boolean,
    val sessionId: Long = -1L,
    val templateName: String = "",
    val totalSets: Int = 0,
    val totalVolume: Float = 0f,
    val weightUnit: WeightUnit = WeightUnit.KG
)

// ── Loaders (consultas one-shot no Room) ──

suspend fun loadWorkoutDayData(context: Context): WorkoutDayData {
    val entryPoint = widgetEntryPoint(context)
    val template = entryPoint.workoutDao().getMyTemplatesOnce().firstOrNull()
        ?: return WorkoutDayData(null, null, 0)
    val count = entryPoint.workoutDao().getExercisesOnce(template.id).size
    return WorkoutDayData(template.id, template.name, count)
}

suspend fun loadWeightData(context: Context): WeightData {
    val entryPoint = widgetEntryPoint(context)
    val unit = entryPoint.userPreferencesRepository().preferences.first().weightUnit
    val metrics = entryPoint.metricDao().getAllOnce()
    val latest = metrics.maxByOrNull { it.date } ?: return WeightData(null, null, unit)
    val target = latest.date - 7L * 24 * 60 * 60 * 1000
    val delta = metrics
        .filter { it.id != latest.id }
        .minByOrNull { kotlin.math.abs(it.date - target) }
        ?.let { latest.weightKg - it.weightKg }
    return WeightData(latest.weightKg, delta, unit)
}

suspend fun loadWeeklyData(context: Context): WeeklyData {
    val zone = ZoneId.systemDefault()
    val trained = widgetEntryPoint(context).sessionDao().getFinishedSessionsOnce()
        .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        .toSet()

    val today = LocalDate.now(zone)
    return WeeklyData(
        weekDays = weekTrainedFlags(trained, today),
        streakDays = currentStreak(trained, today)
    )
}

suspend fun loadActiveSessionData(context: Context): ActiveSessionData {
    val entryPoint = widgetEntryPoint(context)
    val unit = entryPoint.userPreferencesRepository().preferences.first().weightUnit
    val session = entryPoint.sessionDao().getActiveSessionOnce()
        ?: return ActiveSessionData(active = false, weightUnit = unit)
    val sets = entryPoint.sessionDao().getSetsOnce(session.id)
    val templateName = session.templateId
        ?.let { entryPoint.workoutDao().getTemplate(it)?.name }
        ?: "Treino livre"
    return ActiveSessionData(
        active = true,
        sessionId = session.id,
        templateName = templateName,
        totalSets = sets.size,
        totalVolume = sets.filterNot { it.isWarmup }
            .fold(0f) { acc, set -> acc + set.weightKg * set.reps },
        weightUnit = unit
    )
}

/** Atualiza todos os widgets — chamado pelos ViewModels após mutações relevantes. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun refreshAll() {
        WorkoutDayWidget().updateAll(context)
        WeightQuickWidget().updateAll(context)
        WeeklyProgressWidget().updateAll(context)
        ActiveSessionWidget().updateAll(context)
    }
}
