package com.workbuddy.wearosmail.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新：GitHub Release 主源 + 仓库静态 version.json 备用源。
 * GitHub API 匿名请求同 IP 每小时仅 60 次，手表共用 WiFi 时容易撞限流，
 * 因此主源失败/限流/无 APK 资产时自动回退 raw 域名（无 API 限流）。
 * 所有请求都带 User-Agent（GitHub API 强制要求）。
 */
class UpdateManager(private val context: Context) {
    companion object {
        const val CURRENT_VERSION = "1.1.0"
        private const val REPO_API =
            "https://api.github.com/repos/lliiaannggyu/wearos-mail/releases/latest"
        private const val MANIFEST =
            "https://raw.githubusercontent.com/lliiaannggyu/wearos-mail/main/version.json"
        private const val UA = "WearMail-WearOS/$CURRENT_VERSION"

        fun isNewer(latest: String, current: String): Boolean {
            val a = latest.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
            val b = current.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
            if (a.isEmpty()) return false
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }

    data class Info(val version: String, val apkUrl: String, val size: Long)

    sealed class State {
        object Idle : State()
        object Checking : State()
        object UpToDate : State()
        data class Available(val info: Info) : State()
        data class Downloading(val progress: Float) : State()
        data class Ready(val file: File) : State()
        data class NeedPermission(val file: File) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun check() {
        if (_state.value is State.Checking || _state.value is State.Downloading) return
        _state.value = State.Checking
        scope.launch {
            val info = runCatching { fromApi() }.getOrNull()
                ?: runCatching { fromManifest() }.getOrNull()
            _state.value = when {
                info == null -> State.Error("无法获取版本信息")
                info.apkUrl.isBlank() -> State.Error("更新清单缺少 APK 地址")
                isNewer(info.version, CURRENT_VERSION) -> State.Available(info)
                else -> State.UpToDate
            }
        }
    }

    fun download(info: Info) {
        if (_state.value is State.Downloading) return
        scope.launch {
            _state.value = State.Downloading(0f)
            try {
                val dir = File(context.cacheDir, "apk").apply { mkdirs() }
                val file = File(dir, "wear-mail-update.apk")
                if (file.exists()) file.delete()
                val conn = URL(info.apkUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", UA)
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = true
                val total: Long
                try {
                    if (conn.responseCode !in 200..299) {
                        error("下载失败 HTTP ${conn.responseCode}")
                    }
                    total = info.size.takeIf { it > 0 } ?: conn.contentLength.toLong()
                    conn.inputStream.use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(16 * 1024)
                            var done = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total > 0) {
                                    _state.value = State.Downloading(
                                        (done.toFloat() / total).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                if (file.length() < 10_000) error("下载文件异常过小，可能拿到错误页面")
                _state.value = State.Ready(file)
                requestInstall(file)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "下载失败")
            }
        }
    }

    fun requestInstall(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            _state.value = State.NeedPermission(file)
            return
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { _state.value = State.Error("无法启动安装器：${it.message}") }
    }

    fun openInstallSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun fetchJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", UA)
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 12000
        conn.readTimeout = 12000
        return try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun fromApi(): Info {
        val o = fetchJson(REPO_API)
        val version = o.optString("tag_name").removePrefix("v")
        val assets = o.optJSONArray("assets") ?: error("无 assets")
        var url: String? = null
        var size = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name").lowercase()
            if (name.endsWith(".apk")) {
                url = a.optString("browser_download_url")
                size = a.optLong("size")
                break
            }
        }
        return Info(version, url ?: error("Release 中没有 APK"), size)
    }

    private fun fromManifest(): Info {
        val o = fetchJson(MANIFEST)
        return Info(
            o.optString("version").removePrefix("v"),
            o.optString("apk"),
            o.optLong("size"),
        )
    }
}
