package com.homestrength.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.core.content.FileProvider
import com.homestrength.data.room.RoomPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class RemoteAppVersion(
    val versionCode: Int,
    val versionName: String,
    val notes: String = "",
    val apkPath: String = "update/home-strength.apk",
    val apkUrl: String = ""
)

sealed class UpdateCheckResult {
    data class UpToDate(val localName: String, val localCode: Int) : UpdateCheckResult()
    data class Available(val remote: RemoteAppVersion, val localName: String, val localCode: Int) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

/**
 * Checks [update/version.json] on the same Gitee room repo and installs APK via FileProvider.
 */
class AppUpdateRepository(
    private val context: Context,
    private val prefs: RoomPrefs
) {
    fun localVersionName(): String = try {
        val p = context.packageManager.getPackageInfo(context.packageName, 0)
        p.versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }

    fun localVersionCode(): Int = try {
        val p = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode.toInt() else @Suppress("DEPRECATION") p.versionCode
    } catch (_: Exception) {
        0
    }

    fun canCheck(): Boolean =
        prefs.accessToken.isNotBlank() &&
            prefs.owner.isNotBlank() &&
            prefs.repo.isNotBlank()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!canCheck()) {
            return@withContext UpdateCheckResult.Failed("请先在练习室填写 Gitee 令牌")
        }
        runCatching {
            val remote = fetchRemoteVersion()
            val localCode = localVersionCode()
            val localName = localVersionName()
            if (remote.versionCode > localCode) {
                UpdateCheckResult.Available(remote, localName, localCode)
            } else {
                UpdateCheckResult.UpToDate(localName, localCode)
            }
        }.getOrElse {
            UpdateCheckResult.Failed(it.message ?: "检查更新失败")
        }
    }

    suspend fun downloadApk(
        remote: RemoteAppVersion,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "apk").also { it.mkdirs() }
            val outFile = File(dir, "home-strength-${remote.versionName}.apk")
            val path = remote.apkPath.trim().trimStart('/')
            if (remote.apkUrl.isBlank()) {
                writeApkFromGiteeContents(path, outFile, onProgress)
            } else {
                downloadApkFromUrl(resolveApkUrl(remote), outFile, onProgress)
            }
            if (outFile.length() < 10_000L) {
                outFile.delete()
                error("下载文件过小，请确认 Gitee 上已上传 APK")
            }
            onProgress(1f)
            outFile
        }
    }

    /**
     * Private repos often reject [raw] URLs (HTTP 403). Contents API + download_url works with the same token.
     */
    private fun writeApkFromGiteeContents(
        path: String,
        outFile: File,
        onProgress: (Float) -> Unit
    ) {
        val encodedPath = path.split("/").joinToString("/") {
            URLEncoder.encode(it, "UTF-8")
        }
        val branch = prefs.branch.ifBlank { "master" }
        val metaUrl =
            "https://gitee.com/api/v5/repos/${prefs.owner}/${prefs.repo}/contents/$encodedPath" +
                "?access_token=${URLEncoder.encode(prefs.accessToken, "UTF-8")}" +
                "&ref=${URLEncoder.encode(branch, "UTF-8")}"
        val body = httpGet(metaUrl)
        val json = JSONObject(body)
        val b64 = json.optString("content", "").replace("\n", "")
        if (b64.isNotBlank()) {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            FileOutputStream(outFile).use { it.write(bytes) }
            onProgress(1f)
            return
        }
        val downloadUrl = json.optString("download_url", "")
        if (downloadUrl.isBlank()) {
            error("Gitee 未返回 APK 下载地址，请确认 update/home-strength.apk 已上传")
        }
        downloadApkFromUrl(withAccessToken(downloadUrl), outFile, onProgress)
    }

    private fun downloadApkFromUrl(
        url: String,
        outFile: File,
        onProgress: (Float) -> Unit
    ) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 300_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText()?.take(200).orEmpty()
                error("下载失败 HTTP $code ${err.take(120)}")
            }
            val total = conn.contentLengthLong.coerceAtLeast(-1L)
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(outFile).use { output ->
                    val buf = ByteArray(8192)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        if (total > 0L) {
                            onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun withAccessToken(url: String): String {
        val raw = url.trim()
        return if (raw.contains("access_token=")) {
            raw
        } else {
            val sep = if (raw.contains("?")) "&" else "?"
            raw + sep + "access_token=" + URLEncoder.encode(prefs.accessToken, "UTF-8")
        }
    }

    fun canInstallPackages(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun intentOpenUnknownAppSettings(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installApk(file: File): Result<Unit> = runCatching {
        if (!file.exists()) error("安装包不存在")
        if (!canInstallPackages()) {
            error("NEED_INSTALL_PERMISSION")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun fetchRemoteVersion(): RemoteAppVersion {
        val path = "update/version.json"
        val encodedPath = path.split("/").joinToString("/") {
            URLEncoder.encode(it, "UTF-8")
        }
        val branch = prefs.branch.ifBlank { "master" }
        val url =
            "https://gitee.com/api/v5/repos/${prefs.owner}/${prefs.repo}/contents/$encodedPath" +
                "?access_token=${URLEncoder.encode(prefs.accessToken, "UTF-8")}" +
                "&ref=${URLEncoder.encode(branch, "UTF-8")}"
        val body = httpGet(url)
        val json = JSONObject(body)
        val b64 = json.getString("content").replace("\n", "")
        val decoded = String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
        val o = JSONObject(decoded)
        return RemoteAppVersion(
            versionCode = o.getInt("versionCode"),
            versionName = o.optString("versionName", o.getInt("versionCode").toString()),
            notes = o.optString("notes", ""),
            apkPath = o.optString("apkPath", "update/home-strength.apk"),
            apkUrl = o.optString("apkUrl", "")
        )
    }

    private fun resolveApkUrl(remote: RemoteAppVersion): String {
        if (remote.apkUrl.isNotBlank()) {
            val raw = remote.apkUrl.trim()
            return if (raw.contains("access_token=")) {
                raw
            } else {
                val sep = if (raw.contains("?")) "&" else "?"
                raw + sep + "access_token=" + URLEncoder.encode(prefs.accessToken, "UTF-8")
            }
        }
        val branch = prefs.branch.ifBlank { "master" }
        val path = remote.apkPath.trim().trimStart('/')
        return "https://gitee.com/${prefs.owner}/${prefs.repo}/raw/$branch/$path" +
            "?access_token=" + URLEncoder.encode(prefs.accessToken, "UTF-8")
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (code !in 200..299) error("Gitee HTTP $code: ${text.take(200)}")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
