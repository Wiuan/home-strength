package com.homestrength.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HistoryDayCell(
    val day: LocalDate,
    val sessionCount: Int
)

data class HistoryUiState(
    val viewingMonth: YearMonth = YearMonth.now(),
    val isCurrentMonth: Boolean = true,
    val selectedDay: LocalDate = LocalDate.now(),
    val days: List<HistoryDayCell> = emptyList(),
    val selectedSessions: List<SessionWithLogs> = emptyList(),
    val monthSessionCount: Int = 0
)

class HistoryViewModel(
    private val repository: HomeStrengthRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val viewingMonth = MutableStateFlow(YearMonth.now(zone))
    private val selectedDay = MutableStateFlow(LocalDate.now(zone))

    private val sessions: StateFlow<List<SessionWithLogs>> = repository.observeCompletedHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<HistoryUiState> = combine(
        sessions,
        viewingMonth,
        selectedDay
    ) { all, month, day ->
        val byDay = all.groupBy { sessionDay(it) }
        val days = (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            HistoryDayCell(date, byDay[date]?.size ?: 0)
        }
        val selected = day.takeIf { YearMonth.from(it) == month } ?: month.atDay(1)
        HistoryUiState(
            viewingMonth = month,
            isCurrentMonth = month == YearMonth.now(zone),
            selectedDay = selected,
            days = days,
            selectedSessions = byDay[selected].orEmpty(),
            monthSessionCount = days.sumOf { it.sessionCount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
        if (YearMonth.from(day) != viewingMonth.value) {
            viewingMonth.value = YearMonth.from(day)
        }
    }

    fun shiftMonth(delta: Int) {
        val next = viewingMonth.value.plusMonths(delta.toLong())
        viewingMonth.value = next
        selectedDay.value = when {
            next == YearMonth.now(zone) -> LocalDate.now(zone)
            else -> next.atDay(1)
        }
    }

    fun goToCurrentMonth() {
        viewingMonth.value = YearMonth.now(zone)
        selectedDay.value = LocalDate.now(zone)
    }

    fun goToMonth(month: YearMonth) {
        viewingMonth.value = month
        selectedDay.value = when {
            month == YearMonth.now(zone) -> LocalDate.now(zone)
            else -> month.atDay(1)
        }
    }

    fun delete(sessionId: Long) {
        viewModelScope.launch { repository.deleteSessionAndRecomputeNext(sessionId) }
    }

    private fun sessionDay(session: SessionWithLogs): LocalDate =
        Instant.ofEpochMilli(session.session.dateTime).atZone(zone).toLocalDate()

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
