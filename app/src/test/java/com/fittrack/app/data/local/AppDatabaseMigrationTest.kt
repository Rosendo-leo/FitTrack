package com.fittrack.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_DB = "migration-test"

/**
 * Exercita as migrations reais (2→3→4) contra um banco v2 gerado a partir do schema
 * exportado em app/schemas, em vez de só confiar no CREATE fresco do Room. Só há
 * schema exportado para a v2 (as migrations 2→3 e 3→4 nunca rodaram um build local
 * que regenerasse app/schemas/.../3.json e 4.json) — o teste abre o resultado com o
 * Room real (que valida contra as entidades compiladas) em vez de usar
 * helper.runMigrationsAndValidate, que exigiria o json da versão de destino.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun `migra de v2 para v4 preservando dados e adicionando colunas novas`() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO body_metrics (id, date, weightKg, bodyFatPct, waistCm, armCm, chestCm, notes) " +
                    "VALUES (1, 1700000000000, 80.5, NULL, NULL, NULL, NULL, NULL)"
            )
            execSQL(
                "INSERT INTO workout_templates (id, name, description, category, goal, isPreset, createdAt) " +
                    "VALUES (1, 'Push', NULL, 'CUSTOM', 'GENERAL', 0, 1700000000000)"
            )
            close()
        }

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            AppDatabase::class.java,
            TEST_DB
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

        db.openHelper.writableDatabase
            .query("SELECT weightKg, armFlexedLeftCm FROM body_metrics WHERE id = 1")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(80.5, cursor.getDouble(0), 0.001)
                assertTrue(cursor.isNull(1))
            }

        runBlocking { assertEquals(1, db.workoutDao().getMyTemplatesOnce().size) }
        db.close()
    }
}
