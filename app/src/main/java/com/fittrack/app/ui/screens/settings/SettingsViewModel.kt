package com.fittrack.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.preferences.ThemeMode
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setWorkoutReminder(enabled: Boolean, days: Set<Int>, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setWorkoutReminder(enabled, days, hour, minute)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setWeightReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setWeightReminder(enabled, hour, minute)
            reminderScheduler.rescheduleAll()
        }
    }
}
