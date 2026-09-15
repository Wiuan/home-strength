package com.homestrength.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: HomeStrengthRepository
) : ViewModel() {

    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())

    fun updateWeeklyGoal(goal: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(weeklyGoal = goal.coerceIn(1, 7)))
        }
    }

    fun updateDefaultSets(sets: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(defaultSets = sets.coerceIn(1, 5)))
        }
    }

    fun updateDefaultRestSeconds(seconds: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(defaultRestSeconds = seconds.coerceIn(30, 300)))
        }
    }

    companion object {
        fun factory(repository: HomeStrengthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
