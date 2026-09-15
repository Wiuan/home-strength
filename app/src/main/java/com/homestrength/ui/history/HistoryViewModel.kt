package com.homestrength.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: HomeStrengthRepository
) : ViewModel() {

    val sessions: StateFlow<List<SessionWithLogs>> = repository.observeCompletedHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(sessionId: Long) {
        viewModelScope.launch { repository.deleteSessionAndRecomputeNext(sessionId) }
    }

    companion object {
        fun factory(repository: HomeStrengthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
