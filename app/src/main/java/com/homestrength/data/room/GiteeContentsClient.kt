package com.homestrength.data.room

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class GiteeFile(
    val path: String,
    val content: String,
    val sha: String
)

/**
 * Minimal Gitee Contents API client (no third-party HTTP deps).
 * Docs: https://gitee.com/api/v5/swagger
 */
class GiteeContentsClient(
    private val owner: String,
    private val repo: String,
    private val token: String,
    private val branch: String = "master"
) {
    suspend fun getFile(path: String): Result<GiteeFile> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedPath = path.split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8")
            }
            val url =
                "https://gitee.com/api/v5/repos/$owner/$repo/contents/$encodedPath" +
                    "?access_token=${URLEncoder.encode(token, "UTF-8")}" +
                    "&ref=${URLEncoder.encode(branch, "UTF-8")}"
            val body = http("GET", url)
            val json = JSONObject(body)
            val b64 = json.getString("content").replace("\n", "")
            val decoded = String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
            GiteeFile(
                path = json.optString("path", path),
                content = decoded,
                sha = json.getString("sha")
            )
        }
    }

    /**
     * Create (POST) or update (PUT) a file. Gitee requires PUT + [sha] only when the path already exists.
     */
    suspend fun putFile(
        path: String,
        content: String,
        message: String,
        sha: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedPath = path.split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8")
            }
            val url = "https://gitee.com/api/v5/repos/$owner/$repo/contents/$encodedPath"
            val existingSha = when {
                !sha.isNullOrBlank() -> sha
                else -> getFile(path).getOrNull()?.sha
            }
            val payload = JSONObject()
                .put("access_token", token)
                .put("content", Base64.encodeToString(content.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP))
                .put("message", message)
                .put("branch", branch)
            val method = if (!existingSha.isNullOrBlank()) {
                payload.put("sha", existingSha)
                "PUT"
            } else {
                "POST"
            }
            try {
                http(method, url, payload.toString())
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (method == "PUT" && msg.contains("sha", ignoreCase = true)) {
                    val freshSha = getFile(path).getOrNull()?.sha
                    if (!freshSha.isNullOrBlank()) {
                        payload.put("sha", freshSha)
                        http("PUT", url, payload.toString())
                    } else {
                        http("POST", url, payload.toString())
                    }
                } else if (method == "POST" && msg.contains("400")) {
                    val freshSha = getFile(path).getOrNull()?.sha
                    if (!freshSha.isNullOrBlank()) {
                        payload.put("sha", freshSha)
                        http("PUT", url, payload.toString())
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }
            Unit
        }
    }

    private fun http(method: String, url: String, body: String? = null): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) {
                error("Gitee HTTP $code: ${text.take(240)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
