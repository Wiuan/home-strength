package com.homestrength.domain.trainee

import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.repository.TraineeRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DayEntry(
    val title: String,
    val note: String = "",
    val track: PracticeTrack? = null
)

data class DayActivity(
    val day: LocalDate,
    val doneCount: Int,
    val tracks: Set<PracticeTrack>,
    val entries: List<DayEntry>
) {
    val titles: List<String> get() = entries.map { it.title }
}

data class TrackShare(
    val track: PracticeTrack,
    val seconds: Int,
    val count: Int
) {
    val label: String get() = TraineeRepository.trackLabel(track)
}

data class MonthReview(
    val month: YearMonth,
    val days: List<DayActivity>,
    val activeDays: Int,
    val totalDone: Int,
    /** Timed track shares for the pie chart. */
    val trackShares: List<TrackShare> = emptyList()
) {
    fun activityOn(day: LocalDate): DayActivity =
        days.firstOrNull { it.day == day } ?: DayActivity(day, 0, emptySet(), emptyList())

    val totalShareSeconds: Int get() = trackShares.sumOf { it.seconds }
}

object MonthReviewBuilder {
    /** Fallback when session span is missing or nonsense. */
    private const val STRENGTH_FALLBACK_SECONDS = 40 * 60
    private const val STRENGTH_MIN_SECONDS = 60
    private const val STRENGTH_MAX_SECONDS = 4 * 60 * 60
    /** Default length for a completed life checklist tick without a timed practice. */
    private const val LIFE_CHECKLIST_SECONDS = 5 * 60

    fun strengthDurationSeconds(session: WorkoutSessionEntity): Int {
        val raw = ((session.updatedAt - session.dateTime) / 1000L).toInt()
        return if (raw in STRENGTH_MIN_SECONDS..STRENGTH_MAX_SECONDS) {
            raw
        } else {
            STRENGTH_FALLBACK_SECONDS
        }
    }

    fun build(
        month: YearMonth,
        powerDone: List<PowerListItemEntity>,
        lightDone: List<LightPracticeEntity>,
        strengthDone: List<WorkoutSessionEntity>,
        zone: ZoneId = ZoneId.systemDefault()
    ): MonthReview {
        val byDay = linkedMapOf<LocalDate, MutableList<DayEntry>>()

        fun add(day: LocalDate, entry: DayEntry) {
            if (day.year != month.year || day.month != month.month) return
            val list = byDay.getOrPut(day) { mutableListOf() }
            val dup = list.any {
                it.track == entry.track && it.title == entry.title && it.note == entry.note
            }
            if (!dup) list.add(entry)
        }

        powerDone.forEach { item ->
            val day = runCatching { LocalDate.parse(item.dayKey) }.getOrNull() ?: return@forEach
            add(
                day,
                DayEntry(
                    title = item.title,
                    note = item.note.trim(),
                    track = item.track?.let(TraineeRepository::canonicalTrack)
                )
            )
        }

        lightDone.forEach { practice ->
            val day = Instant.ofEpochMilli(practice.startedAt).atZone(zone).toLocalDate()
            if (day.year != month.year || day.month != month.month) return@forEach
            val track = TraineeRepository.canonicalTrack(practice.track)
            val label = TraineeRepository.trackLabel(track)
            val note = practice.note.trim()
            val noteTitle = practice.noteTitle.trim()
            val list = byDay.getOrPut(day) { mutableListOf() }
            val matchIndex = list.indexOfFirst { entry ->
                entry.track == track && (
                    (noteTitle.isNotBlank() && entry.title == noteTitle) ||
                        entry.title == label ||
                        (noteTitle.isBlank() && entry.note.isBlank())
                    )
            }
            if (matchIndex >= 0) {
                val existing = list[matchIndex]
                val mergedNote = when {
                    note.isNotBlank() -> note
                    existing.note.isNotBlank() -> existing.note
                    noteTitle.isNotBlank() && noteTitle != existing.title -> noteTitle
                    else -> existing.note
                }
                list[matchIndex] = existing.copy(note = mergedNote)
            } else {
                add(
                    day,
                    DayEntry(
                        title = noteTitle.ifBlank { label },
                        note = note,
                        track = track
                    )
                )
            }
        }

        strengthDone.forEach { session ->
            val day = Instant.ofEpochMilli(session.dateTime).atZone(zone).toLocalDate()
            val list = byDay[day].orEmpty()
            val already = list.any { it.track == PracticeTrack.STRENGTH }
            if (!already) {
                add(
                    day,
                    DayEntry(
                        title = "力量 · ${session.workoutType.name}",
                        track = PracticeTrack.STRENGTH
                    )
                )
            }
        }

        val days = (1..month.lengthOfMonth()).map { d ->
            val day = month.atDay(d)
            val entries = byDay[day].orEmpty()
            DayActivity(
                day = day,
                doneCount = entries.size,
                tracks = entries.mapNotNull { it.track }.toSet(),
                entries = entries
            )
        }

        val seconds = linkedMapOf<PracticeTrack, Int>()
        val counts = linkedMapOf<PracticeTrack, Int>()

        fun bump(track: PracticeTrack, sec: Int, n: Int = 1) {
            if (sec <= 0 && n <= 0) return
            seconds[track] = (seconds[track] ?: 0) + sec.coerceAtLeast(0)
            counts[track] = (counts[track] ?: 0) + n
        }

        lightDone.forEach { practice ->
            val track = TraineeRepository.canonicalTrack(practice.track)
            if (track == PracticeTrack.SLEEP || track == PracticeTrack.MEDITATION) return@forEach
            bump(track, practice.durationSeconds.coerceAtLeast(0))
        }
        strengthDone.forEach { session ->
            bump(PracticeTrack.STRENGTH, strengthDurationSeconds(session))
        }
        // Life checklist ticks that weren't also timed light practices
        val lightLifeDays = lightDone
            .filter { TraineeRepository.canonicalTrack(it.track) == PracticeTrack.LIFE }
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSet()
        powerDone
            .filter { it.track == PracticeTrack.LIFE }
            .forEach { item ->
                val day = runCatching { LocalDate.parse(item.dayKey) }.getOrNull() ?: return@forEach
                if (day !in lightLifeDays) {
                    bump(PracticeTrack.LIFE, LIFE_CHECKLIST_SECONDS)
                }
            }

        val shares = seconds.entries
            .filter { it.value > 0 }
            .map { (track, sec) -> TrackShare(track, sec, counts[track] ?: 0) }
            .sortedByDescending { it.seconds }

        return MonthReview(
            month = month,
            days = days,
            activeDays = days.count { it.doneCount > 0 },
            totalDone = days.sumOf { it.doneCount },
            trackShares = shares
        )
    }
}
