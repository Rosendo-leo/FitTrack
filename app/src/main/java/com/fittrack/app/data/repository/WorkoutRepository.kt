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
    suspend fun updateTemplate(template: WorkoutTemplate) = workoutDao.updateTemplate(template)
    suspend fun deleteTemplate(template: WorkoutTemplate) = workoutDao.deleteTemplate(template)

    suspend fun getTemplate(id: Long): WorkoutTemplate? = workoutDao.getTemplate(id)

    // ── Exercises ──
    fun observeExercises(templateId: Long): Flow<List<Exercise>> =
        workoutDao.observeExercises(templateId)

    suspend fun getExercises(templateId: Long): List<Exercise> =
        workoutDao.getExercisesOnce(templateId)

    suspend fun saveExercise(exercise: Exercise): Long = workoutDao.insertExercise(exercise)
    suspend fun saveExercises(exercises: List<Exercise>) = workoutDao.insertExercises(exercises)
    suspend fun deleteExercise(exercise: Exercise) = workoutDao.deleteExercise(exercise)
    suspend fun deleteExercisesByIds(ids: List<Long>) = workoutDao.deleteExercisesByIds(ids)

    suspend fun countPresets(): Int = workoutDao.countPresets()

    /** Copia um treino pré-definido para "Meus treinos". Retorna o id da cópia. */
    suspend fun duplicateAsMine(templateId: Long): Long? {
        val original = workoutDao.getTemplate(templateId) ?: return null
        val exercises = workoutDao.getExercisesOnce(templateId)
        val copyId = workoutDao.insertTemplate(
            original.copy(id = 0, isPreset = false, createdAt = System.currentTimeMillis())
        )
        workoutDao.insertExercises(exercises.map { it.copy(id = 0, templateId = copyId) })
        return copyId
    }

    // ── Sessions ──
    fun observeAllSessions(): Flow<List<WorkoutSession>> = sessionDao.observeAllSessions()
    fun observeActiveSession(): Flow<WorkoutSession?> = sessionDao.observeActiveSession()
    fun observeSets(sessionId: Long): Flow<List<SetRecord>> = sessionDao.observeSets(sessionId)

    suspend fun getSession(id: Long): WorkoutSession? = sessionDao.getSession(id)
    suspend fun getActiveSession(): WorkoutSession? = sessionDao.getActiveSessionOnce()

    suspend fun startSession(templateId: Long?): Long =
        sessionDao.insertSession(WorkoutSession(templateId = templateId))

    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)
    suspend fun deleteSet(id: Long) = sessionDao.deleteSet(id)

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
