package com.fittrack.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class WeightUnit { KG, LB }
enum class DistanceUnit { KM, MI }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Usa as cores do papel de parede (Material You) em vez da paleta do app. Android 12+. */
    val dynamicColorEnabled: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val notificationsEnabled: Boolean = true,
    /** Lembrete de treino: dias da semana habilitados (ISO: 1=Seg … 7=Dom). */
    val workoutReminderEnabled: Boolean = false,
    val workoutReminderDays: Set<Int> = emptySet(),
    val workoutReminderHour: Int = 18,
    val workoutReminderMinute: Int = 0,
    /** Lembrete diário de registro de peso. */
    val weightReminderEnabled: Boolean = false,
    val weightReminderHour: Int = 7,
    val weightReminderMinute: Int = 30,
    /** Altura em cm (0 = não informada); usada para o IMC. */
    val heightCm: Float = 0f,
    /** Backup automático diário no Google Drive. */
    val driveSyncEnabled: Boolean = false,
    /** Epoch millis do último backup enviado ao Drive (0 = nunca). */
    val lastDriveBackupAt: Long = 0L
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val WORKOUT_REMINDER_ENABLED = booleanPreferencesKey("workout_reminder_enabled")
        val WORKOUT_REMINDER_DAYS = stringSetPreferencesKey("workout_reminder_days")
        val WORKOUT_REMINDER_HOUR = intPreferencesKey("workout_reminder_hour")
        val WORKOUT_REMINDER_MINUTE = intPreferencesKey("workout_reminder_minute")
        val WEIGHT_REMINDER_ENABLED = booleanPreferencesKey("weight_reminder_enabled")
        val WEIGHT_REMINDER_HOUR = intPreferencesKey("weight_reminder_hour")
        val WEIGHT_REMINDER_MINUTE = intPreferencesKey("weight_reminder_minute")
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val DRIVE_SYNC_ENABLED = booleanPreferencesKey("drive_sync_enabled")
        val LAST_DRIVE_BACKUP_AT = longPreferencesKey("last_drive_backup_at")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: false,
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
                ?: WeightUnit.KG,
            distanceUnit = prefs[Keys.DISTANCE_UNIT]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                ?: DistanceUnit.KM,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            workoutReminderEnabled = prefs[Keys.WORKOUT_REMINDER_ENABLED] ?: false,
            workoutReminderDays = prefs[Keys.WORKOUT_REMINDER_DAYS]
                ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet(),
            workoutReminderHour = prefs[Keys.WORKOUT_REMINDER_HOUR] ?: 18,
            workoutReminderMinute = prefs[Keys.WORKOUT_REMINDER_MINUTE] ?: 0,
            weightReminderEnabled = prefs[Keys.WEIGHT_REMINDER_ENABLED] ?: false,
            weightReminderHour = prefs[Keys.WEIGHT_REMINDER_HOUR] ?: 7,
            weightReminderMinute = prefs[Keys.WEIGHT_REMINDER_MINUTE] ?: 30,
            heightCm = prefs[Keys.HEIGHT_CM] ?: 0f,
            driveSyncEnabled = prefs[Keys.DRIVE_SYNC_ENABLED] ?: false,
            lastDriveBackupAt = prefs[Keys.LAST_DRIVE_BACKUP_AT] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setWorkoutReminder(enabled: Boolean, days: Set<Int>, hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.WORKOUT_REMINDER_ENABLED] = enabled
            it[Keys.WORKOUT_REMINDER_DAYS] = days.map(Int::toString).toSet()
            it[Keys.WORKOUT_REMINDER_HOUR] = hour
            it[Keys.WORKOUT_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setWeightReminder(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.WEIGHT_REMINDER_ENABLED] = enabled
            it[Keys.WEIGHT_REMINDER_HOUR] = hour
            it[Keys.WEIGHT_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setHeightCm(heightCm: Float) {
        dataStore.edit { it[Keys.HEIGHT_CM] = heightCm }
    }

    suspend fun setDriveSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DRIVE_SYNC_ENABLED] = enabled }
    }

    suspend fun setLastDriveBackupAt(timestamp: Long) {
        dataStore.edit { it[Keys.LAST_DRIVE_BACKUP_AT] = timestamp }
    }
}
