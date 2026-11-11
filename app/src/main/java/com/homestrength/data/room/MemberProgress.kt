package com.homestrength.data.room

import org.json.JSONArray
import org.json.JSONObject

/**
 * One public activity tick synced via Gitee. No private notes.
 */
data class RoomActivity(
    val id: String,
    val at: Long,
    val title: String,
    val kind: String,
    val track: String = "",
    val durationSec: Int = 0,
    /** False when only the calendar day is known (no real clock). */
    val preciseTime: Boolean = true,
    /** Chip label in feed — e.g. 阅读 instead of 修养. */
    val trackDisplay: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("at", at)
        .put("title", title)
        .put("kind", kind)
        .put("track", track)
        .put("durationSec", durationSec)
        .put("preciseTime", preciseTime)
        .put("trackDisplay", trackDisplay)

    companion object {
        fun fromJson(o: JSONObject): RoomActivity = RoomActivity(
            id = o.optString("id"),
            at = o.optLong("at"),
            title = o.optString("title"),
            kind = o.optString("kind", "life"),
            track = o.optString("track"),
            durationSec = o.optInt("durationSec"),
            trackDisplay = o.optString("trackDisplay", ""),
            // Old feeds that parked checklist at 20:00 are not real clocks.
            preciseTime = when {
                o.has("preciseTime") -> o.optBoolean("preciseTime", true)
                else -> {
                    val at = o.optLong("at")
                    if (at <= 0L) false
                    else {
                        // Heuristic for already-synced fake 20:00 rows.
                        val local = java.time.Instant.ofEpochMilli(at)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalTime()
                        !(local.hour == 20 && local.minute == 0 && local.second == 0)
                    }
                }
            }
        )
    }
}

/**
 * Public progress card synced via Gitee. Keep small — no private notes.
 */
data class MemberProgress(
    val memberId: String,
    val nickname: String,
    val updatedAt: Long = 0L,
    val fans: Int = 0,
    val grade: String = "F",
    val todayDone: Int = 0,
    val weekStrength: Int = 0,
    val weekSleep: Int = 0,
    val weekLight: Int = 0,
    val monthActiveDays: Int = 0,
    val monthStrengthDays: Int = 0,
    val monthSleepDays: Int = 0,
    val activities: List<RoomActivity> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("memberId", memberId)
        .put("nickname", nickname)
        .put("updatedAt", updatedAt)
        .put("fans", fans)
        .put("grade", grade)
        .put("todayDone", todayDone)
        .put("weekStrength", weekStrength)
        .put("weekSleep", weekSleep)
        .put("weekLight", weekLight)
        .put("monthActiveDays", monthActiveDays)
        .put("monthStrengthDays", monthStrengthDays)
        .put("monthSleepDays", monthSleepDays)
        .put(
            "activities",
            JSONArray().also { arr ->
                activities.forEach { arr.put(it.toJson()) }
            }
        )

    companion object {
        fun fromJson(raw: String): MemberProgress {
            val o = JSONObject(raw)
            val acts = mutableListOf<RoomActivity>()
            val arr = o.optJSONArray("activities")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    acts += RoomActivity.fromJson(item)
                }
            }
            return MemberProgress(
                memberId = o.optString("memberId", "unknown"),
                nickname = o.optString("nickname", "好友"),
                updatedAt = o.optLong("updatedAt", 0L),
                fans = o.optInt("fans"),
                grade = o.optString("grade", "F"),
                todayDone = o.optInt("todayDone"),
                weekStrength = o.optInt("weekStrength"),
                weekSleep = o.optInt("weekSleep"),
                weekLight = o.optInt("weekLight"),
                monthActiveDays = o.optInt("monthActiveDays"),
                monthStrengthDays = o.optInt("monthStrengthDays"),
                monthSleepDays = o.optInt("monthSleepDays"),
                activities = acts.sortedByDescending { it.at }
            )
        }
    }
}
