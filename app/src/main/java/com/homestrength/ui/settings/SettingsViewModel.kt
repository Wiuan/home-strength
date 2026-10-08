package com.homestrength.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.backup.BackupRepository
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsViewModel(
    private val repository: HomeStrengthRepository,
    private val traineeRepository: TraineeRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())

    val profile: StateFlow<TraineeProfileEntity> = traineeRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TraineeProfileEntity())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _importFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importFinished: SharedFlow<Unit> = _importFinished.asSharedFlow()

    fun suggestedBackupFileName(): String {
        val day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "trainee-backup-$day.json"
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportToUri(context, uri)
                .onSuccess { _messages.emit("已导出备份") }
                .onFailure { _messages.emit(it.message ?: "导出失败") }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            backupRepository.importFromUri(context, uri)
                .onSuccess {
                    _messages.emit("导入成功，即将重启以加载数据")
                    _importFinished.emit(Unit)
                }
                .onFailure { _messages.emit(it.message ?: "导入失败") }
        }
    }

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
            traineeRepository: TraineeRepository,
            backupRepository: BackupRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, traineeRepository, backupRepository) as T
                }
            }
    }
}
