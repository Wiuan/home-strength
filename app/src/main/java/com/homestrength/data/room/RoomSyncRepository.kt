package com.homestrength.data.room

import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.TraineeGrade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class RoomCompareSnapshot(
    val me: MemberProgress,
    val peer: MemberProgress?,
    val syncedAt: Long = System.currentTimeMillis()
)

data class RoomFeedItem(
    val memberId: String,
    val nickname: String,
    val isMe: Boolean,
    val activity: RoomActivity
)

/**
 * Push my summary + recent activity to Gitee and pull peer for compare / feed.
 * Token stays in [RoomPrefs] only — never in source.
 */
class RoomSyncRepository(
    private val prefs: RoomPrefs,
    private val traineeRepository: TraineeRepository,
    private val homeRepository: HomeStrengthRepository,
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    private val _lastSnapshot = MutableStateFlow<RoomCompareSnapshot?>(null)
    val lastSnapshot: StateFlow<RoomCompareSnapshot?> = _lastSnapshot.asStateFlow()

    fun prefs(): RoomPrefs = prefs

    fun isReady(): Boolean = prefs.isConfigured()

    suspend fun buildMyProgress(): MemberProgress {
        val profile = traineeRepository.getProfile()
        val today = LocalDate.now(zone)
        val month = YearMonth.from(today)
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = homeRepository.getCompletedSessionsInRange(start, end)
        val review = traineeRepository.loadMonthReview(month, sessions)

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEndExclusive = weekStart.plusDays(7)
        val weekDays = review.days.filter {
            !it.day.isBefore(weekStart) && it.day.isBefore(weekEndExclusive)
        }

        val weekStrength = sessions.count { session ->
            val day = Instant.ofEpochMilli(session.dateTime).atZone(zone).toLocalDate()
            !day.isBefore(weekStart) && day.isBefore(weekEndExclusive)
        }
        val weekLight = weekDays.sumOf { day ->
            day.tracks.count { track ->
                track != PracticeTrack.STRENGTH &&
                    track != PracticeTrack.SLEEP &&
                    track != PracticeTrack.MEDITATION
            }
        }

        val sleepToday = profile.sleepDate == today.toString()
        var weekSleep = weekDays.count { PracticeTrack.SLEEP in it.tracks }
        if (sleepToday && PracticeTrack.SLEEP !in review.activityOn(today).tracks) {
            weekSleep += 1
        }

        val monthStrengthDays = review.days.count { PracticeTrack.STRENGTH in it.tracks }
        val monthSleepDays = review.days.count {
            PracticeTrack.SLEEP in it.tracks || (it.day == today && sleepToday)
        }

        val grade = TraineeGrade.fromFans(profile.fans)
        val activities = buildRecentActivities(today, sleepToday)

        return MemberProgress(
            memberId = prefs.myMemberId,
            nickname = profile.nickname.ifBlank { prefs.myMemberId },
            updatedAt = System.currentTimeMillis(),
            fans = profile.fans,
            grade = TraineeGrade.letter(grade),
            todayDone = review.activityOn(today).doneCount,
            weekStrength = weekStrength,
            weekSleep = weekSleep,
            weekLight = weekLight,
            monthActiveDays = review.activeDays,
            monthStrengthDays = monthStrengthDays,
            monthSleepDays = monthSleepDays,
            activities = activities
        )
    }

    /**
     * Last 14 days of completed ticks, newest first. Caps at [MAX_ACTIVITIES].
     */
    private suspend fun buildRecentActivities(
        today: LocalDate,
        sleepToday: Boolean
    ): List<RoomActivity> {
        val feedStart = today.minusDays(13)
        val endExclusive = today.plusDays(1)
        val startMillis = feedStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()

        val lights = traineeRepository.getCompletedLightInRange(startMillis, endMillis)
        val linkedIds = lights.map { it.id }.toSet()
        val sessions = homeRepository.getCompletedSessionsInRange(startMillis, endMillis)
        val powerDone = traineeRepository.getDonePowerItemsInRange(
            feedStart.toString(),
            endExclusive.toString()
        )

        val out = mutableListOf<RoomActivity>()

        lights.forEach { practice ->
            val track = TraineeRepository.canonicalTrack(practice.track)
            val label = TraineeRepository.trackLabel(track)
            val title = practice.noteTitle.trim().ifBlank { label }
            out += RoomActivity(
                id = "light-${practice.id}",
                at = practice.startedAt,
                title = title,
                kind = when (track) {
                    PracticeTrack.SLEEP -> "sleep"
                    PracticeTrack.MEDITATION -> "meditation"
                    PracticeTrack.STRENGTH -> "strength"
                    else -> "light"
                },
                track = track.name,
                durationSec = practice.durationSeconds.coerceAtLeast(0),
                preciseTime = true
            )
        }

        sessions.forEach { session ->
            out += RoomActivity(
                id = "strength-${session.id}",
                at = session.dateTime,
                title = "力量 · ${session.workoutType.name}",
                kind = "strength",
                track = PracticeTrack.STRENGTH.name,
                durationSec = ((session.updatedAt - session.dateTime) / 1000L)
                    .toInt()
                    .coerceIn(0, 4 * 60 * 60),
                preciseTime = true
            )
        }

        powerDone.forEach { item ->
            if (item.linkedPracticeId > 0L && item.linkedPracticeId in linkedIds) return@forEach
            if (item.status != PowerItemStatus.DONE) return@forEach
            val day = runCatching { LocalDate.parse(item.dayKey) }.getOrNull() ?: return@forEach
            val linkedLight = lights.firstOrNull { it.id == item.linkedPracticeId }
            val preciseAt = when {
                item.completedAt > 0L -> item.completedAt
                linkedLight != null -> linkedLight.startedAt
                else -> 0L
            }
            // Legacy checklist rows: keep day ordering without inventing a fake clock.
            val at = if (preciseAt > 0L) {
                preciseAt
            } else {
                day.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
            }
            val track = item.track?.let(TraineeRepository::canonicalTrack)
            out += RoomActivity(
                id = "power-${item.id}",
                at = at,
                title = item.title.ifBlank { TraineeRepository.trackLabel(track) },
                kind = when (track) {
                    PracticeTrack.SLEEP -> "sleep"
                    PracticeTrack.STRENGTH -> "strength"
                    else -> "checklist"
                },
                track = track?.name.orEmpty(),
                preciseTime = preciseAt > 0L
            )
        }

        if (sleepToday && out.none { it.kind == "sleep" && Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == today }) {
            out += RoomActivity(
                id = "sleep-$today",
                at = System.currentTimeMillis(),
                title = "早睡打卡",
                kind = "sleep",
                track = PracticeTrack.SLEEP.name,
                preciseTime = true
            )
        }

        return out
            .sortedByDescending { it.at }
            .distinctBy { it.id }
            .take(MAX_ACTIVITIES)
    }

    suspend fun syncAndCompare(): Result<RoomCompareSnapshot> {
        if (!prefs.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置里打开练习室并填入 Gitee 令牌"))
        }
        val client = GiteeContentsClient(
            owner = prefs.owner,
            repo = prefs.repo,
            token = prefs.accessToken,
            branch = prefs.branch.ifBlank { "master" }
        )
        val mine = buildMyProgress()
        val myPath = "members/${prefs.myMemberId}.json"
        val peerPath = "members/${prefs.peerMemberId}.json"

        client.putFile(
            path = myPath,
            content = mine.toJson().toString(2),
            message = "sync ${prefs.myMemberId} ${mine.updatedAt}"
        ).getOrElse { return Result.failure(it) }

        val peer = client.getFile(peerPath).fold(
            onSuccess = { runCatching { MemberProgress.fromJson(it.content) }.getOrNull() },
            onFailure = { null }
        )
        val snap = RoomCompareSnapshot(me = mine, peer = peer)
        _lastSnapshot.value = snap
        return Result.success(snap)
    }

    fun mergedFeed(snapshot: RoomCompareSnapshot? = _lastSnapshot.value): List<RoomFeedItem> {
        val snap = snapshot ?: return emptyList()
        val mine = snap.me.activities.map {
            RoomFeedItem(
                memberId = snap.me.memberId,
                nickname = snap.me.nickname,
                isMe = true,
                activity = it
            )
        }
        val peer = snap.peer?.let { p ->
            p.activities.map {
                RoomFeedItem(
                    memberId = p.memberId,
                    nickname = p.nickname,
                    isMe = false,
                    activity = it
                )
            }
        }.orEmpty()
        return (mine + peer).sortedWith(
            compareByDescending<RoomFeedItem> { it.activity.at }
                .thenBy { it.memberId }
        )
    }

    companion object {
        private const val MAX_ACTIVITIES = 40
    }
}
