package com.moai.planner.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.moai.planner.data.repository.settings.SettingsRepository

@Suppress("UNCHECKED_CAST")
class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
