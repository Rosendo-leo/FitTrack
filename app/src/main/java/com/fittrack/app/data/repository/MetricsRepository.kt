package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.CardioDao
import com.fittrack.app.data.local.dao.GoalDao
import com.fittrack.app.data.local.dao.MetricDao
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsRepository @Inject constructor(
    private val metricDao: MetricDao,
    private val cardioDao: CardioDao,
    private val goalDao: GoalDao
) {
    // ── Body metrics ──
    fun observeAllMetrics(): Flow<List<BodyMetric>> = metricDao.observeAll()
    fun observeLatestMetric(): Flow<BodyMetric?> = metricDao.observeLatest()
    fun observeMetricsBetween(from: Long, to: Long): Flow<List<BodyMetric>> =
        metricDao.observeBetween(from, to)

    suspend fun saveMetric(metric: BodyMetric): Long = metricDao.insert(metric)
    suspend fun deleteMetric(metric: BodyMetric) = metricDao.delete(metric)

    // ── Cardio ──
    fun observeAllCardio(): Flow<List<CardioSession>> = cardioDao.observeAll()
    fun observeCardioBetween(from: Long, to: Long): Flow<List<CardioSession>> =
        cardioDao.observeBetween(from, to)

    suspend fun saveCardio(session: CardioSession): Long = cardioDao.insert(session)
    suspend fun deleteCardio(session: CardioSession) = cardioDao.delete(session)

    // ── Metas ──
    fun observeGoals(): Flow<List<Goal>> = goalDao.observeAll()
    suspend fun saveGoal(goal: Goal): Long = goalDao.insert(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.delete(goal)
    suspend fun markGoalAchieved(goal: Goal) =
        goalDao.update(goal.copy(achievedAt = System.currentTimeMillis()))
}
