package com.homestrength.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.domain.workout.PreviousPerformanceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val history: List<ExerciseHistoryItem> = emptyList(),
    val currentResistance: Int = 0,
    val bestLabel: String = "—",
    val loading: Boolean = true
)

data class ExerciseHistoryItem(
    val dateMillis: Long,
    val summaryLabel: String
)

class ExerciseDetailViewModel(
    private val repository: HomeStrengthRepository,
    private val exerciseId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = repository.getExercise(exerciseId)
            val logs = repository.getRecentLogs(exerciseId, limit = 30)
            val history = logs.map { log ->
                val session = repository.getSession(log.log.sessionId)?.session
                val performance = PreviousPerformanceFormatter.fromLog(log)
                ExerciseHistoryItem(
                    dateMillis = session?.dateTime ?: 0L,
                    summaryLabel = performance.summaryLabel
                )
            }
            val latest = logs.firstOrNull()?.let { PreviousPerformanceFormatter.fromLog(it) }
            val best = logs.maxByOrNull { log ->
                log.sets.mapNotNull { it.set.reps ?: it.set.durationSeconds }.sum()
            }?.let { PreviousPerformanceFormatter.fromLog(it) }

            _uiState.value = ExerciseDetailUiState(
                exercise = exercise,
                history = history,
                currentResistance = latest?.totalResistance ?: 0,
                bestLabel = best?.valuesLabel ?: "—",
                loading = false
            )
        }
    }

    companion object {
        fun factory(
            repository: HomeStrengthRepository,
            exerciseId: Long
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExerciseDetailViewModel(repository, exerciseId) as T
                }
            }
    }
}
