package com.homestrength.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.domain.band.BandCombination
import com.homestrength.domain.progression.ProgressionSuggestion
import com.homestrength.domain.progression.ProgressionType
import com.homestrength.domain.workout.PreviousPerformance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val repository: HomeStrengthRepository,
    private val sessionId: Long
) : ViewModel() {

    val session: StateFlow<SessionWithLogs?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _previousByExerciseId = MutableStateFlow<Map<Long, PreviousPerformance>>(emptyMap())
    val previousByExerciseId: StateFlow<Map<Long, PreviousPerformance>> = _previousByExerciseId.asStateFlow()

    private val _suggestionsByExerciseId = MutableStateFlow<Map<Long, ProgressionSuggestion>>(emptyMap())
    val suggestionsByExerciseId: StateFlow<Map<Long, ProgressionSuggestion>> =
        _suggestionsByExerciseId.asStateFlow()

    private val _combinations = MutableStateFlow<List<BandCombination>>(emptyList())
    val combinations: StateFlow<List<BandCombination>> = _combinations.asStateFlow()

    private val _defaultRestSeconds = MutableStateFlow(90)
    val defaultRestSeconds: StateFlow<Int> = _defaultRestSeconds.asStateFlow()

    init {
        viewModelScope.launch {
            _combinations.value = repository.availableCombinations()
            _defaultRestSeconds.value = repository.getSettings().defaultRestSeconds
            val current = repository.getSession(sessionId)
            val ids = current?.logs?.map { it.exercise.id }.orEmpty()
            _previousByExerciseId.value = repository.getPreviousPerformanceMap(ids)
            _suggestionsByExerciseId.value = repository.getProgressionSuggestionMap(ids)
        }
    }

    fun updateSetValue(setId: Long, value: Int?, unit: TargetUnit) {
        viewModelScope.launch {
            repository.updateSetValue(setId = setId, value = value, unit = unit)
        }
    }

    fun applyResistance(logId: Long, combination: BandCombination) {
        viewModelScope.launch {
            repository.applyCombinationToExerciseLog(logId, combination)
        }
    }

    fun applySuggestedResistance(logId: Long, exerciseId: Long) {
        val suggestion = _suggestionsByExerciseId.value[exerciseId] ?: return
        val combo = suggestion.suggestedCombination ?: return
        if (suggestion.type != ProgressionType.ADD_RESISTANCE) return
        applyResistance(logId, combo)
    }

    fun skipExercise(logId: Long) {
        viewModelScope.launch { repository.skipExercise(logId) }
    }

    fun unskipExercise(logId: Long) {
        viewModelScope.launch { repository.unskipExercise(logId) }
    }

    companion object {
        fun factory(
            repository: HomeStrengthRepository,
            sessionId: Long
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ActiveWorkoutViewModel(repository, sessionId) as T
                }
            }
    }
}
