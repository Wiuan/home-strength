package com.homestrength.data.feishu

import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.data.room.RoomSyncRepository
import com.homestrength.data.track.LightTrackCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FeishuSyncResult(
    val applied: Int,
    val revoked: Int,
    val skipped: Int,
    val skippedNoFlag: Int,
    val roomPushed: Boolean,
    val message: String
)

/**
 * Feishu「同步到App」= 已完成并计分 + 进练习室动态（经 Gitee 推送）。
 * 取消勾选 = 扣分并删本地完成记录；下次练习室同步即下墙。
 */
class FeishuSyncRepository(
    private val prefs: FeishuPrefs,
    private val traineeRepository: TraineeRepository,
    private val roomSyncRepository: RoomSyncRepository? = null,
    private val lightTrackCatalog: LightTrackCatalog? = null
) {
    /**
     * @param pushRoom when true and there are apply/revoke changes, push Gitee after.
     * Callers that sync room themselves can pass false to avoid a double push.
     */
    suspend fun sync(pushRoom: Boolean = true): FeishuSyncResult = withContext(Dispatchers.IO) {
        if (!prefs.isConfigured()) {
            return@withContext FeishuSyncResult(
                0, 0, 0, 0, false, "请先填写飞书 App ID / Secret / Wiki 或 base 链接"
            )
        }
        val client = FeishuApiClient(prefs.appId, prefs.appSecret)
        val bitableToken = client.resolveBitableAppToken(extractToken(prefs.appToken))
        val manualIds = prefs.tableIds().map { extractTableId(it).ifBlank { it } }.distinct()
        val tableIds = if (manualIds.isNotEmpty()) {
            manualIds
        } else {
            val listed = client.listTables(bitableToken)
            if (listed.isEmpty()) {
                return@withContext FeishuSyncResult(
                    0, 0, 0, 0, false, "该 Wiki/base 下没有数据表"
                )
            }
            listed.map { it.tableId }
        }

        var applied = 0
        var revoked = 0
        var skipped = 0
        var skippedNoFlag = 0
        // Keys: "tableId/recordId" (and bare recordId for legacy marks).
        val seenKeys = mutableSetOf<String>()

        for (tableId in tableIds) {
            val records = client.searchRecords(bitableToken, tableId)
            for (rec in records) {
                val key = prefs.syncKey(tableId, rec.recordId)
                seenKeys += key
                seenKeys += rec.recordId
                val checked = syncFlag(rec.fields)
                val already = prefs.isRecordSynced(tableId, rec.recordId)
                if (checked) {
                    if (already && traineeRepository.hasFeishuCompletion(rec.recordId)) {
                        skipped++
                        continue
                    }
                    val rawTitle = textField(rec.fields, "训练名称").ifBlank { "飞书训练" }
                    val minutes = numberField(rec.fields, "分钟数").coerceAtLeast(0)
                    val atMillis = dateTimeMillis(rec.fields, "训练时间", "锻炼时间")
                        ?: dateTimeMillis(rec.fields, "创建时间", "创建于")
                        ?: rec.createdTime
                        ?: rec.lastModifiedTime
                    val trainDay = atMillis?.let { ms ->
                        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    } ?: dateField(rec.fields, "训练时间", "锻炼时间")
                        ?: traineeRepository.todayKey()
                    val lightLabel = textField(rec.fields, "轻轨", "轨道")
                    val fromLight = lightLabel.takeIf { it.isNotBlank() }
                        ?.let { lightTrackCatalog?.findByLabel(it) }
                    val track = fromLight?.backing
                        ?: mapTrack(textField(rec.fields, "训练类型"))
                    val seconds = if (minutes > 0) minutes * 60 else 0
                    val ok = traineeRepository.applyFeishuCompletion(
                        recordId = rec.recordId,
                        dayKey = trainDay,
                        title = rawTitle,
                        track = track,
                        targetDurationSeconds = seconds,
                        atMillis = atMillis,
                        // Keep Feishu cell text (e.g. 运动) even if not in catalog.
                        lightTrackLabel = lightLabel.takeIf { it.isNotBlank() }
                            ?: fromLight?.label
                    )
                    prefs.markRecordSynced(tableId, rec.recordId)
                    if (ok) applied++ else skipped++
                } else if (already) {
                    traineeRepository.revokeFeishuCompletion(rec.recordId)
                    prefs.unmarkRecordSynced(tableId, rec.recordId)
                    revoked++
                } else {
                    skippedNoFlag++
                }
            }
        }

        // Deleted from Feishu (or removed from configured tables) but still marked → revoke.
        for (stored in prefs.syncedRecordIds()) {
            if (stored in seenKeys) continue
            val slash = stored.indexOf('/')
            val (tablePart, recordId) = if (slash > 0) {
                stored.substring(0, slash) to stored.substring(slash + 1)
            } else {
                "" to stored
            }
            if (tablePart.isNotBlank() && tablePart !in tableIds) {
                // Table removed from config — leave mark until user revokes all, or treat as orphan.
            }
            traineeRepository.revokeFeishuCompletion(recordId)
            if (tablePart.isNotBlank()) {
                prefs.unmarkRecordSynced(tablePart, recordId)
            } else {
                // Legacy bare id: clear against every configured table key + bare.
                tableIds.forEach { prefs.unmarkRecordSynced(it, recordId) }
            }
            revoked++
        }

        var roomPushed = false
        var roomNote = ""
        if (pushRoom && (applied > 0 || revoked > 0)) {
            if (roomSyncRepository?.isReady() == true) {
                roomSyncRepository.syncAndCompare()
                    .onSuccess { roomPushed = true }
                    .onFailure { roomNote = " · 练习室推送失败：${it.message ?: "未知"}" }
            } else {
                roomNote = " · 未推练习室（先开练习室并填令牌）"
            }
        }

        val msg = buildString {
            append(
                if (manualIds.isEmpty()) "自动 ${tableIds.size} 表"
                else "限定 ${tableIds.size} 表"
            )
            append(" · 上墙 $applied · 下墙 $revoked")
            if (skipped > 0) append(" · 已同步 $skipped")
            if (skippedNoFlag > 0) append(" · 未勾选 $skippedNoFlag")
            if (roomPushed) append(" · 已推练习室")
            append(roomNote)
        }
        FeishuSyncResult(applied, revoked, skipped, skippedNoFlag, roomPushed, msg)
    }

    /** Revoke all Feishu completions + clear marks (settings reset). */
    suspend fun revokeAllLocal(): Int = withContext(Dispatchers.IO) {
        val n = traineeRepository.revokeAllFeishuCompletions()
        prefs.clearSyncedRecords()
        if (n > 0 && roomSyncRepository?.isReady() == true) {
            roomSyncRepository.syncAndCompare()
        }
        n
    }

    companion object {
        fun extractToken(raw: String): String {
            val t = raw.trim()
            val wiki = Regex("""/wiki/([A-Za-z0-9]+)""").find(t)
            if (wiki != null) return wiki.groupValues[1]
            val base = Regex("""/base/([A-Za-z0-9]+)""").find(t)
            if (base != null) return base.groupValues[1]
            return t
        }

        fun extractTableId(raw: String): String {
            val t = raw.trim()
            val m = Regex("""(?:[?&]table=|table/)([A-Za-z0-9]+)""").find(t)
            return m?.groupValues?.get(1) ?: t
        }

        /**
         * Feishu field names are exact; friends often use 同步到APP vs 同步到App.
         * Match ignoring ASCII case and spaces.
         */
        private fun fieldRaw(fields: Map<String, Any?>, vararg names: String): Any? {
            for (n in names) {
                fields[n]?.let { return it }
            }
            fun norm(s: String) = s.replace(" ", "").replace("　", "").lowercase()
            val wanted = names.map(::norm).toSet()
            for ((k, v) in fields) {
                if (norm(k) in wanted) return v
            }
            return null
        }

        private fun syncFlag(fields: Map<String, Any?>): Boolean {
            val v = fieldRaw(fields, "同步到App", "同步到APP", "同步到app") ?: return false
            return when (v) {
                is Boolean -> v
                is Number -> v.toInt() != 0
                is String -> v.equals("true", true) || v == "1" || v == "是"
                else -> false
            }
        }

        private fun textField(fields: Map<String, Any?>, vararg keys: String): String {
            val v = fieldRaw(fields, *keys) ?: return ""
            return when (v) {
                is String -> v.trim()
                is Number -> v.toString()
                is JSONArray -> {
                    buildString {
                        for (i in 0 until v.length()) {
                            val item = v.opt(i)
                            if (item is JSONObject) {
                                val t = item.optString("text", item.optString("name", ""))
                                if (t.isNotBlank()) {
                                    if (isNotEmpty()) append(" ")
                                    append(t)
                                }
                            } else if (item != null) {
                                if (isNotEmpty()) append(" ")
                                append(item.toString())
                            }
                        }
                    }.trim()
                }
                is JSONObject -> v.optString("text", v.optString("name", "")).trim()
                else -> v.toString().trim()
            }
        }

        private fun numberField(fields: Map<String, Any?>, vararg keys: String): Int {
            val v = fieldRaw(fields, *keys) ?: return 0
            return when (v) {
                is Number -> v.toInt()
                is String -> v.toDoubleOrNull()?.toInt() ?: 0
                else -> 0
            }
        }

        private fun dateField(fields: Map<String, Any?>, vararg keys: String): String? {
            val ms = dateTimeMillis(fields, *keys) ?: return null
            return Instant.ofEpochMilli(ms)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }

        /** Unix millis from a Feishu date / created-time cell (preserves HH:mm when set). */
        private fun dateTimeMillis(fields: Map<String, Any?>, vararg keys: String): Long? {
            val v = fieldRaw(fields, *keys) ?: return null
            val zone = ZoneId.systemDefault()
            return when (v) {
                is Number -> v.toLong().takeIf { it > 0L }
                is String -> {
                    val s = v.trim()
                    runCatching {
                        when {
                            s.all { it.isDigit() } -> s.toLong().takeIf { it > 0L }
                            // "2026-11-18 14:30" / "2026/11/18 14:30"
                            s.length >= 16 && (s[4] == '-' || s[4] == '/') -> {
                                val norm = s.replace('/', '-').take(16)
                                java.time.LocalDateTime.parse(
                                    norm,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                ).atZone(zone).toInstant().toEpochMilli()
                            }
                            s.length >= 10 && (s[4] == '-' || s[4] == '/') -> {
                                LocalDate.parse(s.take(10).replace('/', '-'))
                                    .atStartOfDay(zone).toInstant().toEpochMilli()
                            }
                            else -> null
                        }
                    }.getOrNull()
                }
                else -> null
            }
        }

        /** Fallback when「轻轨」列为空：仍读「训练类型」。 */
        private fun mapTrack(type: String): PracticeTrack {
            val t = type.trim()
            return when {
                t.contains("算法") -> PracticeTrack.ALGORITHM
                t.contains("音乐") || t.contains("声乐") -> PracticeTrack.VOCAL
                t.contains("有氧") || t.contains("运动") || t.contains("修养") ||
                    t.contains("阅读") || t.contains("书法") -> PracticeTrack.CULTIVATION
                t.contains("生活") -> PracticeTrack.LIFE
                t.contains("力量") -> PracticeTrack.STRENGTH
                else -> PracticeTrack.CULTIVATION
            }
        }
    }
}
