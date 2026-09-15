package com.homestrength.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModeSelectUiState(
    val nextWorkoutType: WorkoutType = WorkoutType.A,
    val hasIncomplete: Boolean = false,
    val incompleteSessionId: Long? = null,
    val starting: Boolean = false,
    val error: String? = null
)

class ModeSelectViewModel(
    private val repository: HomeStrengthRepository
) : ViewModel() {

    private val starting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModeSelectUiState> = combine(
        repository.observeSettings(),
        repository.observeIncomplete(),
        starting,
        error
    ) { settings, incomplete, isStarting, err ->
        ModeSelectUiState(
            nextWorkoutType = settings.nextWorkoutType,
            hasIncomplete = incomplete != null,
            incompleteSessionId = incomplete?.session?.id,
            starting = isStarting,
            error = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModeSelectUiState())

    fun start(mode: TrainingMode, onStarted: (Long) -> Unit) {
        if (starting.value) return
        viewModelScope.launch {
            starting.value = true
            error.value = null
            runCatching { repository.startWorkout(mode) }
                .onSuccess { sessionId ->
                    starting.value = false
                    onStarted(sessionId)
                }
                .onFailure {
                    starting.value = false
                    error.value = it.message ?: "无法开始训练"
                }
        }
    }

    companion object {
        fun factory(repository: HomeStrengthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ModeSelectViewModel(repository) as T
                }
            }
    }
}
