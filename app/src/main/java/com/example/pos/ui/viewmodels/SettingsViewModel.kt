package com.example.pos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.entity.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDao: SettingsDao
) : ViewModel() {

    val settings: StateFlow<Settings?> = settingsDao.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            settingsDao.insertOrUpdateSettings(settings)
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            settingsDao.deleteAllSettings()
        }
    }

    class Factory(private val settingsDao: SettingsDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}