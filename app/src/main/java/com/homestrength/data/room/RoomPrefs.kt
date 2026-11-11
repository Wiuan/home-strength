package com.homestrength.data.room

import android.content.Context

/**
 * Local-only Gitee room credentials. Never commit these values to source control.
 */
class RoomPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var owner: String
        get() = prefs.getString(KEY_OWNER, "wyx5555").orEmpty()
        set(value) = prefs.edit().putString(KEY_OWNER, value.trim()).apply()

    var repo: String
        get() = prefs.getString(KEY_REPO, "trainee-room").orEmpty()
        set(value) = prefs.edit().putString(KEY_REPO, value.trim()).apply()

    var branch: String
        get() = prefs.getString(KEY_BRANCH, "master").orEmpty()
        set(value) = prefs.edit().putString(KEY_BRANCH, value.trim().ifBlank { "master" }).apply()

    var accessToken: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    var myMemberId: String
        get() = prefs.getString(KEY_MY_ID, "wyx").orEmpty()
        set(value) = prefs.edit().putString(KEY_MY_ID, value.trim()).apply()

    var peerMemberId: String
        get() = prefs.getString(KEY_PEER_ID, "friend").orEmpty()
        set(value) = prefs.edit().putString(KEY_PEER_ID, value.trim()).apply()

    fun isConfigured(): Boolean =
        enabled &&
            owner.isNotBlank() &&
            repo.isNotBlank() &&
            accessToken.isNotBlank() &&
            myMemberId.isNotBlank() &&
            peerMemberId.isNotBlank()

    companion object {
        private const val PREFS = "trainee_room_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_OWNER = "owner"
        private const val KEY_REPO = "repo"
        private const val KEY_BRANCH = "branch"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_MY_ID = "my_member_id"
        private const val KEY_PEER_ID = "peer_member_id"
    }
}
