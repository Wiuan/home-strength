package com.homestrength.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PeriodGoalStatus
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.data.room.MemberProgress
import com.homestrength.data.room.RoomSyncRepository
import com.homestrength.domain.trainee.PeriodReview
import com.homestrength.domain.trainee.PeriodReviewBuilder
import com.homestrength.domain.trainee.PlanPeriod
import com.homestrength.domain.trainee.PlanPeriodKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class RoomCompareScope { DAY, WEEK, MONTH }

data class ProfileUiState(
    val profile: TraineeProfileEntity = TraineeProfileEntity(),
    val weekStrengthCount: Int = 0,
    val weekLightCount: Int = 0,
    val weekChecklistDone: Int = 0,
    val viewingPeriod: PlanPeriod = PlanPeriod.current(PlanPeriodKind.MONTH),
    val isCurrentPeriod: Boolean = true,
    val periodGoals: List<PeriodGoalEntity> = emptyList(),
    val goalLimit: Int = 5,
    val periodReview: PeriodReview = PeriodReviewBuilder.empty(),
    val selectedDay: LocalDate = LocalDate.now(),
    val message: String? = null,
    val roomEnabled: Boolean = false,
    val roomSyncing: Boolean = false,
    val roomMe: MemberProgress? = null,
    val roomPeer: MemberProgress? = null,
    val roomScope: RoomCompareScope = RoomCompareScope.WEEK
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val repository: HomeStrengthRepository,
    private val traineeRepository: TraineeRepository,
    private val roomSyncRepository: RoomSyncRepository? = null
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val viewingPeriod = MutableStateFlow(PlanPeriod.current(PlanPeriodKind.MONTH))
    private val selectedDay = MutableStateFlow(LocalDate.now(zone))
    private val periodReview = MutableStateFlow(PeriodReviewBuilder.empty())
    private val uiMessage = MutableStateFlow<String?>(null)
    private val roomSyncing = MutableStateFlow(false)
    private val roomMe = MutableStateFlow<MemberProgress?>(null)
    private val roomPeer = MutableStateFlow<MemberProgress?>(null)
    private val roomScope = MutableStateFlow(RoomCompareScope.WEEK)

    private data class WeekSlice(
        val strength: Int,
        val light: Int,
        val checklist: Int
    )

    private data class PeriodSlice(
        val period: PlanPeriod,
        val goals: List<PeriodGoalEntity>,
        val review: PeriodReview,
        val day: LocalDate,
        val message: String?
    )

    private data class RoomSlice(
        val syncing: Boolean,
        val me: MemberProgress?,
        val peer: MemberProgress?,
        val scope: RoomCompareScope
    )

    init {
        refreshPeriodReview()
        if (roomSyncRepository?.isReady() == true) {
            syncRoom(silent = true)
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        traineeRepository.observeProfile(),
        combine(
            weekStrengthFlow(),
            traineeRepository.observeRecentLightPractices(40),
            traineeRepository.observePowerList()
        ) { strength, light, power ->
            val weekStart = weekStartMillis()
            WeekSlice(
                strength = strength,
                light = light.count { it.startedAt >= weekStart && it.completed },
                checklist = power.count { it.status == PowerItemStatus.DONE }
            )
        },
        combine(
            viewingPeriod,
            viewingPeriod.flatMapLatest { traineeRepository.observePeriodGoals(it) },
            periodReview,
            selectedDay,
            uiMessage
        ) { period, goals, review, day, message ->
            PeriodSlice(period, goals, review, day, message)
        },
        combine(
            combine(roomSyncing, roomMe) { syncing, me -> syncing to me },
            combine(roomPeer, roomScope) { peer, scope -> peer to scope }
        ) { a, b ->
            RoomSlice(a.first, a.second, b.first, b.second)
        }
    ) { profile, week, slice, room ->
        ProfileUiState(
            profile = profile,
            weekStrengthCount = week.strength,
            weekLightCount = week.light,
            weekChecklistDone = week.checklist,
            viewingPeriod = slice.period,
            isCurrentPeriod = slice.period.isCurrent(),
            periodGoals = slice.goals,
            goalLimit = traineeRepository.goalLimit(slice.period.kind),
            periodReview = slice.review,
            selectedDay = slice.day,
            message = slice.message,
            roomEnabled = roomSyncRepository?.prefs()?.enabled == true,
            roomSyncing = room.syncing,
            roomMe = room.me,
            roomPeer = room.peer,
            roomScope = room.scope
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
    }

    fun setPeriodKind(kind: PlanPeriodKind) {
        if (viewingPeriod.value.kind == kind) return
        val next = PlanPeriod.current(kind)
        viewingPeriod.value = next
        syncSelectedDay(next)
        refreshPeriodReview()
    }

    fun shiftPeriod(delta: Int) {
        val next = viewingPeriod.value.shift(delta)
        if (next == viewingPeriod.value) return
        viewingPeriod.value = next
        syncSelectedDay(next)
        refreshPeriodReview()
    }

    fun goToCurrentPeriod() {
        goToPeriod(PlanPeriod.current(viewingPeriod.value.kind))
    }

    fun goToPeriod(period: PlanPeriod) {
        viewingPeriod.value = period
        syncSelectedDay(period)
        refreshPeriodReview()
    }

    fun goToMonth(month: YearMonth) {
        goToPeriod(PlanPeriod.month(month))
    }

    fun refreshPeriodReview() {
        viewModelScope.launch {
            val period = viewingPeriod.value
            val months = period.months()
            val start = months.first().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = months.last().plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sessions = repository.getCompletedSessionsInRange(start, end)
            periodReview.value = traineeRepository.loadPeriodReview(period, sessions)
        }
    }

    fun addPeriodGoal(
        title: String,
        targetParts: Int = 1,
        track: PracticeTrack? = null
    ) {
        viewModelScope.launch {
            val period = viewingPeriod.value
            val err = traineeRepository.addPeriodGoal(
                title = title,
                period = period,
                track = track,
                targetParts = targetParts
            )
            uiMessage.value = err ?: "已加入${period.planTitle()}"
        }
    }

    fun markGoalDone(id: Long) {
        viewModelScope.launch {
            traineeRepository.setPeriodGoalStatus(id, PeriodGoalStatus.DONE)
        }
    }

    fun dropGoal(id: Long) {
        viewModelScope.launch {
            traineeRepository.setPeriodGoalStatus(id, PeriodGoalStatus.DROPPED)
        }
    }

    fun bumpGoalProgress(id: Long, delta: Int) {
        viewModelScope.launch {
            val err = traineeRepository.bumpPeriodGoalProgress(id, delta)
            if (err != null) uiMessage.value = err
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch { traineeRepository.deletePeriodGoal(id) }
    }

    fun placeGoalIntoToday(id: Long, targetDurationSeconds: Int = 0) {
        viewModelScope.launch {
            uiMessage.value = traineeRepository.placePeriodGoalIntoToday(
                id = id,
                targetDurationSeconds = targetDurationSeconds
            ) ?: "已放进今天清单"
            refreshPeriodReview()
        }
    }

    fun consumeMessage() {
        uiMessage.value = null
    }

    fun setRoomScope(scope: RoomCompareScope) {
        roomScope.value = scope
    }

    fun syncRoom(silent: Boolean = false) {
        val sync = roomSyncRepository ?: return
        if (!sync.isReady()) {
            if (!silent) uiMessage.value = "先在设置里打开练习室并填令牌"
            return
        }
        viewModelScope.launch {
            roomSyncing.value = true
            sync.syncAndCompare()
                .onSuccess { snap ->
                    roomMe.value = snap.me
                    roomPeer.value = snap.peer
                    if (!silent) {
                        uiMessage.value = if (snap.peer == null) {
                            "已上传 · 对方还没同步"
                        } else {
                            "已同步 · ${snap.peer.nickname}"
                        }
                    }
                }
                .onFailure {
                    if (!silent) uiMessage.value = it.message ?: "同步失败"
                }
            roomSyncing.value = false
        }
    }

    private fun syncSelectedDay(period: PlanPeriod) {
        val today = LocalDate.now(zone)
        selectedDay.value = when {
            period.isCurrent() -> today
            period.kind == PlanPeriodKind.MONTH -> period.months().first().atDay(1)
            else -> period.months().first().atDay(1)
        }
    }

    private fun weekStrengthFlow() = run {
        val start = weekStartMillis()
        val end = start + 7L * 24 * 60 * 60 * 1000
        repository.observeWeeklyCompletedCount(start, end)
    }

    private fun weekStartMillis(): Long =
        LocalDate.now(zone)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    companion object {
        fun factory(
            repository: HomeStrengthRepository,
            traineeRepository: TraineeRepository,
            roomSyncRepository: RoomSyncRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(repository, traineeRepository, roomSyncRepository) as T
                }
            }
    }
}
