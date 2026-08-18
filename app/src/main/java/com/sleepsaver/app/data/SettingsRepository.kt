package com.sleepsaver.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sleepSaverDataStore by preferencesDataStore(name = "sleep_saver_settings")

data class AppSettings(
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 30,
    val wakeHour: Int = 8,
    val wakeMinute: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderAdvanceMinutes: Int = 30,
    val targetSleepMinutes: Int = 7 * 60
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val bedtimeHour = intPreferencesKey("bedtime_hour")
        val bedtimeMinute = intPreferencesKey("bedtime_minute")
        val wakeHour = intPreferencesKey("wake_hour")
        val wakeMinute = intPreferencesKey("wake_minute")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
    }

    val settings: Flow<AppSettings> = context.sleepSaverDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            AppSettings(
                bedtimeHour = preferences[Keys.bedtimeHour] ?: 23,
                bedtimeMinute = preferences[Keys.bedtimeMinute] ?: 30,
                wakeHour = preferences[Keys.wakeHour] ?: 8,
                wakeMinute = preferences[Keys.wakeMinute] ?: 0,
                reminderEnabled = preferences[Keys.reminderEnabled] ?: false
            )
        }

    suspend fun setBedtime(hour: Int, minute: Int) {
        context.sleepSaverDataStore.edit {
            it[Keys.bedtimeHour] = hour.coerceIn(0, 23)
            it[Keys.bedtimeMinute] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setWakeTime(hour: Int, minute: Int) {
        context.sleepSaverDataStore.edit {
            it[Keys.wakeHour] = hour.coerceIn(0, 23)
            it[Keys.wakeMinute] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.sleepSaverDataStore.edit { it[Keys.reminderEnabled] = enabled }
    }
}

