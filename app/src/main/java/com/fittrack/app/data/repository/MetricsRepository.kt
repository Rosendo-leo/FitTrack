package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.CardioDao
import com.fittrack.app.data.local.dao.MetricDao
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsRepository @Inject constructor(
    private val metricDao: MetricDao,
    private val cardioDao: CardioDao
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
}
