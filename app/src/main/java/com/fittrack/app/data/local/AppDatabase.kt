package com.fittrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fittrack.app.data.local.dao.BackupDao
import com.fittrack.app.data.local.dao.CardioDao
import com.fittrack.app.data.local.dao.GoalDao
import com.fittrack.app.data.local.dao.MetricDao
import com.fittrack.app.data.local.dao.SessionDao
import com.fittrack.app.data.local.dao.WorkoutDao
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.Exercise
import com.fittrack.app.data.local.entities.Goal
import com.fittrack.app.data.local.entities.SetRecord
import com.fittrack.app.data.local.entities.WorkoutSession
import com.fittrack.app.data.local.entities.WorkoutTemplate

@Database(
    entities = [
        WorkoutTemplate::class,
        Exercise::class,
        WorkoutSession::class,
        SetRecord::class,
        BodyMetric::class,
        CardioSession::class,
        Goal::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun metricDao(): MetricDao
    abstract fun cardioDao(): CardioDao
    abstract fun backupDao(): BackupDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val NAME = "fittrack.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN restSeconds INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN armFlexedLeftCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN armFlexedRightCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN armRelaxedLeftCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN armRelaxedRightCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN shoulderCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN thighLeftCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN thighRightCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN forearmLeftCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN forearmRightCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN calfLeftCm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE body_metrics ADD COLUMN calfRightCm REAL DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `metric` TEXT NOT NULL,
                        `targetValue` REAL NOT NULL,
                        `startValue` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `achievedAt` INTEGER
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
