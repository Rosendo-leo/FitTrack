package com.fittrack.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.WorkoutTemplate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Roda num banco Room em memória via Robolectric (JVM, sem emulador), cobrindo o que
 * o ROADMAP marcava como pendente por "exigir instrumentação".
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WorkoutDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insere template e exercicios e recupera na ordem certa`() = runTest {
        val dao = db.workoutDao()
        val templateId = dao.insertTemplate(WorkoutTemplate(name = "Push"))

        dao.insertExercises(
            listOf(
                Exercise(templateId = templateId, name = "Supino", muscleGroup = "Peito", orderIndex = 1),
                Exercise(templateId = templateId, name = "Desenvolvimento", muscleGroup = "Ombro", orderIndex = 0)
            )
        )

        val exercises = dao.getExercisesOnce(templateId)
        assertEquals(listOf("Desenvolvimento", "Supino"), exercises.map { it.name })
    }

    @Test
    fun `deletar template remove exercicios em cascata`() = runTest {
        val dao = db.workoutDao()
        val templateId = dao.insertTemplate(WorkoutTemplate(name = "Pull"))
        dao.insertExercise(Exercise(templateId = templateId, name = "Remada", muscleGroup = "Costas"))

        dao.deleteTemplate(dao.getTemplate(templateId)!!)

        assertTrue(dao.getExercisesOnce(templateId).isEmpty())
        assertNull(dao.getTemplate(templateId))
    }

    @Test
    fun `conta apenas presets`() = runTest {
        val dao = db.workoutDao()
        dao.insertTemplate(WorkoutTemplate(name = "Meu treino", isPreset = false))
        dao.insertTemplate(WorkoutTemplate(name = "PPL", isPreset = true))
        dao.insertTemplate(WorkoutTemplate(name = "ABC", isPreset = true))

        assertEquals(2, dao.countPresets())
    }
}
