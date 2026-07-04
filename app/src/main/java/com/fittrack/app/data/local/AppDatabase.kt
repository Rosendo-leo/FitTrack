package com.fittrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fittrack.app.data.local.dao.CardioDao
import com.fittrack.app.data.local.dao.MetricDao
import com.fittrack.app.data.local.dao.SessionDao
import com.fittrack.app.data.local.dao.WorkoutDao
import com.fittrack.app.data.local.entities.BodyMetric
import com.fittrack.app.data.local.entities.CardioSession
import com.fittrack.app.data.local.entities.Exercise
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
        CardioSession::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun metricDao(): MetricDao
    abstract fun cardioDao(): CardioDao

    companion object {
        const val NAME = "fittrack.db"
    }
}
