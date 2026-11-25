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
    val entries: List<DayEntry>,
    /** Timed track shares for this calendar day (feeds day-scoped pie). */
    val trackShares: List<TrackShare> = emptyList()
) {
    val titles: List<String> get() = entries.map { it.title }
}

data class TrackShare(
    val track: PracticeTrack,
    val seconds: Int,
    val count: Int,
    /** Display name — may be Feishu/custom 轻轨 (运动) instead of backing 修养. */
    val label: String = TraineeRepository.trackLabel(track)
)

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
        zone: ZoneId = ZoneId.systemDefault(),
        /**
         * Resolve a nicer slice label (e.g. 运动 / 阅读) from title + backing track + notes.
         * Default keeps enum labels (修养 for CULTIVATION).
         */
        displayLabel: (title: String, track: PracticeTrack, note: String) -> String =
            { _, track, note ->
                TraineeRepository.lightLabelFromNote(note)
                    ?: TraineeRepository.trackLabel(track)
            }
    ): MonthReview {
        val byDay = linkedMapOf<LocalDate, MutableList<DayEntry>>()
        val powerByPracticeId = powerDone
            .filter { it.linkedPracticeId > 0L }
            .associateBy { it.linkedPracticeId }

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

        // Aggregate by display label so 运动 / 阅读 don't collapse into 修养.
        val monthSeconds = linkedMapOf<String, Int>()
        val monthCounts = linkedMapOf<String, Int>()
        val monthTrack = linkedMapOf<String, PracticeTrack>()
        val daySeconds = linkedMapOf<LocalDate, MutableMap<String, Int>>()
        val dayCounts = linkedMapOf<LocalDate, MutableMap<String, Int>>()
        val dayTrack = linkedMapOf<LocalDate, MutableMap<String, PracticeTrack>>()

        fun bump(day: LocalDate, track: PracticeTrack, sec: Int, label: String, n: Int = 1) {
            if (sec <= 0 && n <= 0) return
            if (day.year != month.year || day.month != month.month) return
            val s = sec.coerceAtLeast(0)
            val key = label.ifBlank { TraineeRepository.trackLabel(track) }
            monthSeconds[key] = (monthSeconds[key] ?: 0) + s
            monthCounts[key] = (monthCounts[key] ?: 0) + n
            monthTrack.putIfAbsent(key, track)
            val secMap = daySeconds.getOrPut(day) { mutableMapOf() }
            val cntMap = dayCounts.getOrPut(day) { mutableMapOf() }
            val trMap = dayTrack.getOrPut(day) { mutableMapOf() }
            secMap[key] = (secMap[key] ?: 0) + s
            cntMap[key] = (cntMap[key] ?: 0) + n
            trMap.putIfAbsent(key, track)
        }

        fun sharesOf(
            seconds: Map<String, Int>,
            counts: Map<String, Int>,
            tracks: Map<String, PracticeTrack>
        ): List<TrackShare> = seconds.entries
            .filter { it.value > 0 }
            .map { (label, sec) ->
                val track = tracks[label] ?: PracticeTrack.CULTIVATION
                TrackShare(track, sec, counts[label] ?: 0, label)
            }
            .sortedByDescending { it.seconds }

        fun labelForLight(practice: LightPracticeEntity, track: PracticeTrack): String {
            val linked = powerByPracticeId[practice.id]
            val fromLinked = linked?.note?.let(TraineeRepository::lightLabelFromNote)
            val fromPractice = TraineeRepository.lightLabelFromNote(practice.note)
            val title = practice.noteTitle.trim().ifBlank { linked?.title.orEmpty() }
            val note = listOf(practice.note, linked?.note.orEmpty()).firstOrNull { it.isNotBlank() }.orEmpty()
            return fromLinked
                ?: fromPractice
                ?: displayLabel(title, track, note)
        }

        lightDone.forEach { practice ->
            val track = TraineeRepository.canonicalTrack(practice.track)
            if (track == PracticeTrack.SLEEP || track == PracticeTrack.MEDITATION) return@forEach
            val day = Instant.ofEpochMilli(practice.startedAt).atZone(zone).toLocalDate()
            bump(day, track, practice.durationSeconds.coerceAtLeast(0), labelForLight(practice, track))
        }
        strengthDone.forEach { session ->
            val day = Instant.ofEpochMilli(session.dateTime).atZone(zone).toLocalDate()
            bump(
                day,
                PracticeTrack.STRENGTH,
                strengthDurationSeconds(session),
                TraineeRepository.trackLabel(PracticeTrack.STRENGTH)
            )
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
                    val label = TraineeRepository.lightLabelFromNote(item.note)
                        ?: displayLabel(item.title, PracticeTrack.LIFE, item.note)
                    bump(day, PracticeTrack.LIFE, LIFE_CHECKLIST_SECONDS, label)
                }
            }

        val days = (1..month.lengthOfMonth()).map { d ->
            val day = month.atDay(d)
            val entries = byDay[day].orEmpty()
            DayActivity(
                day = day,
                doneCount = entries.size,
                tracks = entries.mapNotNull { it.track }.toSet(),
                entries = entries,
                trackShares = sharesOf(
                    daySeconds[day].orEmpty(),
                    dayCounts[day].orEmpty(),
                    dayTrack[day].orEmpty()
                )
            )
        }

        return MonthReview(
            month = month,
            days = days,
            activeDays = days.count { it.doneCount > 0 },
            totalDone = days.sumOf { it.doneCount },
            trackShares = sharesOf(monthSeconds, monthCounts, monthTrack)
        )
    }
}
