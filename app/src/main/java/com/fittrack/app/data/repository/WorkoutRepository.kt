package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.SessionDao
import com.fittrack.app.data.local.dao.TemplateWithExercises
import com.fittrack.app.data.local.dao.WorkoutDao
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.local.entities.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val sessionDao: SessionDao
) {
    // ── Templates ──
    fun observeMyTemplates(): Flow<List<WorkoutTemplate>> = workoutDao.observeMyTemplates()
    fun observePresetTemplates(): Flow<List<WorkoutTemplate>> = workoutDao.observePresetTemplates()
    fun observeTemplateWithExercises(id: Long): Flow<TemplateWithExercises?> =
        workoutDao.observeTemplateWithExercises(id)

    suspend fun saveTemplate(template: WorkoutTemplate): Long = workoutDao.insertTemplate(template)
    suspend fun deleteTemplate(template: WorkoutTemplate) = workoutDao.deleteTemplate(template)

    // ── Exercises ──
    fun observeExercises(templateId: Long): Flow<List<Exercise>> =
        workoutDao.observeExercises(templateId)

    suspend fun saveExercise(exercise: Exercise): Long = workoutDao.insertExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = workoutDao.deleteExercise(exercise)

    // ── Sessions ──
    fun observeAllSessions(): Flow<List<WorkoutSession>> = sessionDao.observeAllSessions()
    fun observeActiveSession(): Flow<WorkoutSession?> = sessionDao.observeActiveSession()
    fun observeSets(sessionId: Long): Flow<List<SetRecord>> = sessionDao.observeSets(sessionId)

    suspend fun startSession(templateId: Long?): Long =
        sessionDao.insertSession(WorkoutSession(templateId = templateId))

    suspend fun finishSession(session: WorkoutSession, totalVolume: Float) {
        sessionDao.updateSession(
            session.copy(finishedAt = System.currentTimeMillis(), totalVolume = totalVolume)
        )
    }

    suspend fun recordSet(set: SetRecord): Long = sessionDao.insertSet(set)

    /** Retorna true se a carga informada é um novo recorde pessoal para o exercício. */
    suspend fun isPersonalRecord(exerciseId: Long, weightKg: Float): Boolean {
        val best = sessionDao.bestWeightFor(exerciseId) ?: return true
        return weightKg > best
    }
}
