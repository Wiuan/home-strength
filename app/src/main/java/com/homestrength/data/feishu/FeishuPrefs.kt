package com.homestrength.data.feishu

import android.content.Context

/**
 * Local-only Feishu open-platform credentials for training-table sync.
 * Never commit real secrets.
 *
 * [tableId] may hold multiple ids (comma / newline / semicolon), same [appToken] base.
 */
class FeishuPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var appId: String
        get() = prefs.getString(KEY_APP_ID, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_APP_ID, value.trim()).apply()

    var appSecret: String
        get() = prefs.getString(KEY_APP_SECRET, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_APP_SECRET, value.trim()).apply()

    /** Bitable app_token (or wiki node token — resolved on sync). */
    var appToken: String
        get() = prefs.getString(KEY_APP_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_APP_TOKEN, value.trim()).apply()

    /** One or more table ids / URLs, separated by comma, newline, or semicolon. */
    var tableId: String
        get() = prefs.getString(KEY_TABLE_ID, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TABLE_ID, value.trim()).apply()

    fun tableIds(): List<String> = parseTableIds(tableId)

    fun isConfigured(): Boolean =
        appId.isNotBlank() &&
            appSecret.isNotBlank() &&
            appToken.isNotBlank() &&
            tableIds().isNotEmpty()

    fun syncedRecordIds(): Set<String> =
        prefs.getStringSet(KEY_SYNCED, emptySet()).orEmpty().toSet()

    fun syncKey(tableId: String, recordId: String): String = "$tableId/$recordId"

    fun isRecordSynced(tableId: String, recordId: String): Boolean {
        val keys = syncedRecordIds()
        return syncKey(tableId, recordId) in keys || recordId in keys
    }

    fun markRecordSynced(tableId: String, recordId: String) {
        val next = syncedRecordIds().toMutableSet()
        next.remove(recordId) // drop legacy bare id if present
        next.add(syncKey(tableId, recordId))
        prefs.edit().putStringSet(KEY_SYNCED, next).apply()
    }

    fun unmarkRecordSynced(tableId: String, recordId: String) {
        val next = syncedRecordIds().toMutableSet()
        var changed = next.remove(syncKey(tableId, recordId))
        changed = next.remove(recordId) || changed
        if (!changed) return
        prefs.edit().putStringSet(KEY_SYNCED, next).apply()
    }

    /** Clears local Feishu sync marks only (scores/items unchanged). */
    fun clearSyncedRecords() {
        prefs.edit().remove(KEY_SYNCED).apply()
    }

    companion object {
        private const val PREFS = "feishu_training_prefs"
        private const val KEY_APP_ID = "app_id"
        private const val KEY_APP_SECRET = "app_secret"
        private const val KEY_APP_TOKEN = "app_token"
        private const val KEY_TABLE_ID = "table_id"
        private const val KEY_SYNCED = "synced_record_ids"

        fun parseTableIds(raw: String): List<String> =
            raw.split(',', '\n', ';', '，', '；')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
    }
}
