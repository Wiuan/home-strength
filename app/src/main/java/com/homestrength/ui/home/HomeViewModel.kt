package com.homestrength.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.TraineeGrade
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HomeUiState(
    val greeting: String = "",
    val profile: TraineeProfileEntity = TraineeProfileEntity(),
    val gradeLabel: String = TraineeGrade.F.label,
    val gradeLetter: String = "F",
    val powerList: List<PowerListItemEntity> = emptyList(),
    val nextWorkoutType: WorkoutType = WorkoutType.A,
    val isSuggestedTrainingDay: Boolean = true,
    val weeklyGoal: Int = 3,
    val weeklyCompleted: Int = 0,
    val lastSession: SessionWithLogs? = null,
    val incompleteSession: SessionWithLogs? = null,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: HomeStrengthRepository,
    private val traineeRepository: TraineeRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private val _gradeUpgrade = MutableSharedFlow<TraineeGrade>(extraBufferCapacity = 1)
    val gradeUpgrade: SharedFlow<TraineeGrade> = _gradeUpgrade.asSharedFlow()

    init {
        viewModelScope.launch {
            traineeRepository.getProfile()
            traineeRepository.observeProfile().collect {
                traineeRepository.consumeGradeUpgradeIfAny()?.let { grade ->
                    _gradeUpgrade.emit(grade)
                }
            }
        }
    }

    private data class StrengthSlice(
        val settings: AppSettingsEntity,
        val last: SessionWithLogs?,
        val incomplete: SessionWithLogs?,
        val weeklyCount: Int
    )

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            repository.observeSettings(),
            repository.observeLastCompleted(),
            repository.observeIncomplete(),
            weeklyCountFlow()
        ) { settings, last, incomplete, weeklyCount ->
            StrengthSlice(settings, last, incomplete, weeklyCount)
        },
        traineeRepository.observeProfile(),
        traineeRepository.observePowerList()
    ) { strength, profile, powerList ->
        val grade = TraineeGrade.fromFans(profile.fans)
        HomeUiState(
            greeting = greetingForNow(),
            profile = profile,
            gradeLabel = grade.label,
            gradeLetter = if (grade == TraineeGrade.PRE_DEBUT) "★" else grade.name,
            powerList = powerList,
            nextWorkoutType = strength.settings.nextWorkoutType,
            isSuggestedTrainingDay = isDefaultTrainingDay(LocalDate.now()),
            weeklyGoal = strength.settings.weeklyGoal,
            weeklyCompleted = strength.weeklyCount,
            lastSession = strength.last,
            incompleteSession = strength.incomplete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun addPowerItem(title: String, track: PracticeTrack?) {
        viewModelScope.launch {
            traineeRepository.addPowerItem(title, track)
        }
    }

    fun togglePowerItem(id: Long) {
        viewModelScope.launch { traineeRepository.togglePowerItemDone(id) }
    }

    fun deletePowerItem(id: Long) {
        viewModelScope.launch { traineeRepository.deletePowerItem(id) }
    }

    /**
     * @return null on success, otherwise a short message for snackbar.
     */
    suspend fun completeEarlySleep(): String? = traineeRepository.completeEarlySleep()

    suspend fun undoEarlySleep() = traineeRepository.undoEarlySleep()

    suspend fun completeMeditation() = traineeRepository.completeMeditation()

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
        fun factory(
            repository: HomeStrengthRepository,
            traineeRepository: TraineeRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository, traineeRepository) as T
                }
            }
    }
}
