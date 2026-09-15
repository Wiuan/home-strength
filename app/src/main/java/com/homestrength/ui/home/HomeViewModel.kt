package com.homestrength.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HomeUiState(
    val greeting: String = "",
    val nextWorkoutType: WorkoutType = WorkoutType.A,
    val isSuggestedTrainingDay: Boolean = true,
    val weeklyGoal: Int = 3,
    val weeklyCompleted: Int = 0,
    val lastSession: SessionWithLogs? = null,
    val incompleteSession: SessionWithLogs? = null,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: HomeStrengthRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSettings(),
        repository.observeLastCompleted(),
        repository.observeIncomplete(),
        weeklyCountFlow()
    ) { settings, last, incomplete, weeklyCount ->
        HomeUiState(
            greeting = greetingForNow(),
            nextWorkoutType = settings.nextWorkoutType,
            isSuggestedTrainingDay = isDefaultTrainingDay(LocalDate.now()),
            weeklyGoal = settings.weeklyGoal,
            weeklyCompleted = weeklyCount,
            lastSession = last,
            incompleteSession = incomplete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private fun weeklyCountFlow() = run {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd = weekStart + 7L * 24 * 60 * 60 * 1000
        repository.observeWeeklyCompletedCount(weekStart, weekEnd)
    }

    private fun greetingForNow(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 12 -> "早上好"
            hour < 18 -> "下午好"
            else -> "晚上好"
        }
    }

    private fun isDefaultTrainingDay(date: LocalDate): Boolean =
        date.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

    companion object {
        fun factory(repository: HomeStrengthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
