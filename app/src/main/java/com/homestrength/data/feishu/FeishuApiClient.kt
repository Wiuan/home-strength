package com.homestrength.data.feishu

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class FeishuRecord(
    val recordId: String,
    val fields: Map<String, Any?>,
    /** System created_time millis when automatic_fields is enabled. */
    val createdTime: Long? = null,
    val lastModifiedTime: Long? = null
)

data class FeishuTable(
    val tableId: String,
    val name: String
)

/**
 * Minimal Feishu OpenAPI client (tenant_access_token + bitable search + wiki node).
 */
class FeishuApiClient(
    private val appId: String,
    private val appSecret: String
) {
    private var cachedToken: String = ""
    private var tokenExpireAtMs: Long = 0L

    suspend fun tenantAccessToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken.isNotBlank() && now < tokenExpireAtMs - 60_000L) {
            return@withContext cachedToken
        }
        val body = JSONObject()
            .put("app_id", appId)
            .put("app_secret", appSecret)
        val json = httpJson(
            method = "POST",
            url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal",
            body = body,
            bearer = null
        )
        if (json.optInt("code", -1) != 0 && !json.has("tenant_access_token")) {
            error(json.optString("msg", "获取飞书 token 失败"))
        }
        val token = json.getString("tenant_access_token")
        val expire = json.optInt("expire", 7200)
        cachedToken = token
        tokenExpireAtMs = now + expire * 1000L
        token
    }

    /**
     * If [token] is a wiki node token, resolve bitable [obj_token]; otherwise return as-is.
     */
    suspend fun resolveBitableAppToken(token: String): String = withContext(Dispatchers.IO) {
        val raw = token.trim()
        if (raw.isBlank()) error("缺少 app_token")
        // Already looks like a base token from /base/ URL or known obj_token — try wiki resolve first if wiki-like.
        runCatching {
            val bearer = tenantAccessToken()
            val url =
                "https://open.feishu.cn/open-apis/wiki/v2/spaces/get_node?token=" +
                    java.net.URLEncoder.encode(raw, "UTF-8") +
                    "&obj_type=wiki"
            val json = httpJson("GET", url, body = null, bearer = bearer)
            if (json.optInt("code", -1) != 0) {
                error(json.optString("msg", "解析知识库节点失败"))
            }
            val node = json.getJSONObject("data").getJSONObject("node")
            val objType = node.optString("obj_type", "")
            val objToken = node.optString("obj_token", "")
            if (objType == "bitable" && objToken.isNotBlank()) {
                return@withContext objToken
            }
        }
        raw
    }

    /** List all data tables under a bitable [appToken]. */
    suspend fun listTables(appToken: String): List<FeishuTable> = withContext(Dispatchers.IO) {
        val bearer = tenantAccessToken()
        val out = mutableListOf<FeishuTable>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("https://open.feishu.cn/open-apis/bitable/v1/apps/")
                append(appToken)
                append("/tables?page_size=100")
                if (!pageToken.isNullOrBlank()) {
                    append("&page_token=")
                    append(java.net.URLEncoder.encode(pageToken, "UTF-8"))
                }
            }
            val json = httpJson("GET", url, body = null, bearer = bearer)
            if (json.optInt("code", -1) != 0) {
                error(json.optString("msg", "列出飞书数据表失败"))
            }
            val data = json.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("table_id", "")
                if (id.isBlank()) continue
                out += FeishuTable(id, item.optString("name", id))
            }
            pageToken = if (data.optBoolean("has_more", false)) {
                data.optString("page_token", "").ifBlank { null }
            } else {
                null
            }
        } while (pageToken != null)
        out
    }

    suspend fun searchRecords(
        appToken: String,
        tableId: String,
        pageSize: Int = 100
    ): List<FeishuRecord> = withContext(Dispatchers.IO) {
        val bearer = tenantAccessToken()
        val out = mutableListOf<FeishuRecord>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("https://open.feishu.cn/open-apis/bitable/v1/apps/")
                append(appToken)
                append("/tables/")
                append(tableId)
                append("/records/search?page_size=")
                append(pageSize.coerceIn(1, 500))
                if (!pageToken.isNullOrBlank()) {
                    append("&page_token=")
                    append(java.net.URLEncoder.encode(pageToken, "UTF-8"))
                }
            }
            val json = httpJson(
                method = "POST",
                url = url,
                body = JSONObject().put("automatic_fields", true),
                bearer = bearer
            )
            if (json.optInt("code", -1) != 0) {
                error(json.optString("msg", "查询飞书记录失败"))
            }
            val data = json.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("record_id", "")
                if (id.isBlank()) continue
                val fieldsJson = item.optJSONObject("fields") ?: JSONObject()
                val fields = mutableMapOf<String, Any?>()
                val keys = fieldsJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    fields[k] = fieldsJson.opt(k)
                }
                val created = item.optLong("created_time", 0L).takeIf { it > 0L }
                    ?: item.optLong("created_time_ms", 0L).takeIf { it > 0L }
                val modified = item.optLong("last_modified_time", 0L).takeIf { it > 0L }
                    ?: item.optLong("last_modified_time_ms", 0L).takeIf { it > 0L }
                out += FeishuRecord(id, fields, created, modified)
            }
            pageToken = if (data.optBoolean("has_more", false)) {
                data.optString("page_token", "").ifBlank { null }
            } else {
                null
            }
        } while (pageToken != null)
        out
    }

    private fun httpJson(
        method: String,
        url: String,
        body: JSONObject?,
        bearer: String?
    ): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!bearer.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearer")
            }
            doInput = true
            if (body != null && method != "GET") {
                doOutput = true
            }
        }
        try {
            if (body != null && method != "GET") {
                OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                    it.write(body.toString())
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (text.isBlank()) error("飞书 HTTP $code 空响应")
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
