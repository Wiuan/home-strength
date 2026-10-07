package com.homestrength.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: HomeStrengthRepository,
    private val traineeRepository: TraineeRepository
) : ViewModel() {

    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())

    val profile: StateFlow<TraineeProfileEntity> = traineeRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TraineeProfileEntity())

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

    fun updateRewardRules(
        checklistFans: Int,
        checklistCoins: Int,
        lightFans: Int,
        lightCoins: Int,
        strengthFans: Int,
        strengthCoins: Int,
        sleepEnergyRestore: Int,
        sleepFans: Int,
        sleepCoins: Int,
        meditationEnergyRestore: Int,
        meditationFans: Int,
        meditationCoins: Int
    ) {
        viewModelScope.launch {
            traineeRepository.updateRewardRules(
                checklistFans = checklistFans,
                checklistCoins = checklistCoins,
                lightFans = lightFans,
                lightCoins = lightCoins,
                strengthFans = strengthFans,
                strengthCoins = strengthCoins,
                sleepEnergyRestore = sleepEnergyRestore,
                sleepFans = sleepFans,
                sleepCoins = sleepCoins,
                meditationEnergyRestore = meditationEnergyRestore,
                meditationFans = meditationFans,
                meditationCoins = meditationCoins
            )
        }
    }

    companion object {
        fun factory(
            repository: HomeStrengthRepository,
            traineeRepository: TraineeRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, traineeRepository) as T
                }
            }
    }
}
