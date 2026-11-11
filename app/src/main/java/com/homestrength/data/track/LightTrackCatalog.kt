package com.homestrength.data.track

import android.content.Context
import com.homestrength.data.local.entity.PracticeTrack
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-editable light / checklist tracks. Peers of 算法/声乐/修养 on home.
 * [backing] picks timer vs checklist-only; [rewardFans]/[rewardCoins] override profile defaults when >= 0.
 */
data class LightTrackDef(
    val id: String,
    val label: String,
    val backing: PracticeTrack,
    val defaultMinutes: Int = 10,
    val showOnHome: Boolean = true,
    val showOnChecklist: Boolean = true,
    val locked: Boolean = false,
    /** -1 = use profile light/checklist defaults */
    val rewardFans: Int = -1,
    val rewardCoins: Int = -1
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("backing", backing.name)
        .put("defaultMinutes", defaultMinutes)
        .put("showOnHome", showOnHome)
        .put("showOnChecklist", showOnChecklist)
        .put("locked", locked)
        .put("rewardFans", rewardFans)
        .put("rewardCoins", rewardCoins)

    fun defaultTitle(minutes: Int = defaultMinutes): String {
        val m = minutes.coerceAtLeast(0).let { if (it == 0) defaultMinutes else it }
        return when (backing) {
            PracticeTrack.LIFE -> label
            else -> if (label.contains("分钟")) label else "$label ${m}分钟"
        }
    }

    fun isTimed(): Boolean = backing != PracticeTrack.LIFE

    companion object {
        fun fromJson(o: JSONObject): LightTrackDef? {
            val backing = runCatching {
                PracticeTrack.valueOf(o.getString("backing"))
            }.getOrNull() ?: return null
            return LightTrackDef(
                id = o.getString("id"),
                label = o.optString("label", ""),
                backing = backing,
                defaultMinutes = o.optInt("defaultMinutes", 10),
                showOnHome = o.optBoolean("showOnHome", true),
                showOnChecklist = o.optBoolean("showOnChecklist", true),
                locked = o.optBoolean("locked", false),
                rewardFans = o.optInt("rewardFans", -1),
                rewardCoins = o.optInt("rewardCoins", -1)
            )
        }

        fun defaults(): List<LightTrackDef> = listOf(
            LightTrackDef("algorithm", "算法", PracticeTrack.ALGORITHM, defaultMinutes = 10, locked = true),
            LightTrackDef("vocal", "声乐", PracticeTrack.VOCAL, defaultMinutes = 10, locked = true),
            LightTrackDef("cultivation", "修养", PracticeTrack.CULTIVATION, defaultMinutes = 10, locked = true),
            LightTrackDef("reading", "阅读", PracticeTrack.CULTIVATION, defaultMinutes = 10),
            LightTrackDef("calligraphy", "书法", PracticeTrack.CULTIVATION, defaultMinutes = 10),
            LightTrackDef(
                "life",
                "生活",
                PracticeTrack.LIFE,
                defaultMinutes = 0,
                showOnHome = false,
                locked = true
            )
        )
    }
}

class LightTrackCatalog(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<LightTrackDef> {
        val raw = prefs.getString(KEY, null) ?: return LightTrackDef.defaults()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    LightTrackDef.fromJson(item)?.let { add(it) }
                }
            }.ifEmpty { LightTrackDef.defaults() }
        }.getOrDefault(LightTrackDef.defaults())
    }

    fun save(list: List<LightTrackDef>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun homeTracks(): List<LightTrackDef> =
        load().filter { it.showOnHome && it.backing != PracticeTrack.LIFE }

    fun checklistTracks(): List<LightTrackDef> =
        load().filter { it.showOnChecklist }

    fun findById(id: String): LightTrackDef? = load().firstOrNull { it.id == id }

    /**
     * Match checklist / practice title to a track def (longest label wins).
     */
    fun findForTitle(title: String, track: PracticeTrack? = null): LightTrackDef? {
        val t = title.trim()
        if (t.isEmpty()) {
            return track?.let { tr ->
                load().firstOrNull { it.locked && it.backing == tr }
            }
        }
        val byLabel = load()
            .filter { def ->
                t == def.label ||
                    t.startsWith(def.label) ||
                    def.label.isNotBlank() && t.contains(def.label)
            }
            .maxByOrNull { it.label.length }
        if (byLabel != null) return byLabel
        val canonical = when (track) {
            PracticeTrack.READING, PracticeTrack.CALLIGRAPHY -> PracticeTrack.CULTIVATION
            else -> track
        } ?: return null
        return load().firstOrNull { it.locked && it.backing == canonical }
    }

    companion object {
        private const val PREFS = "light_track_catalog"
        private const val KEY = "tracks"
    }
}
