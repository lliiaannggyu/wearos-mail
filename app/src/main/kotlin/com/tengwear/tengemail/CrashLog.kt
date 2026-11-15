package com.tengwear.tengemail

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import java.io.File

/**
 * 兜底：把任何未捕获异常落到本地文件，下次启动时用可见的错误页展示原因，
 * 而不是让用户只看到「闪退」却拿不到任何线索。
 */
object CrashLog {

    private const val FILE_NAME = "last_crash.txt"

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    fun read(context: Context): String? = runCatching {
        val f = file(context)
        if (f.exists() && f.length() > 0) f.readText() else null
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    /** 安装全局异常兜底。原 handler 保留并转交，不改变系统默认行为。 */
    @SuppressLint("UnsafeOptInUsageError")
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is CrashCatcher) return
        Thread.setDefaultUncaughtExceptionHandler(CrashCatcher(app, previous))
    }

    private class CrashCatcher(
        private val app: Context,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            runCatching {
                val text = buildString {
                    append("包名: ").append(app.packageName).append('\n')
                    append("设备: ").append(Build.MANUFACTURER).append(' ')
                        .append(Build.MODEL).append('\n')
                    append("系统: Android ").append(Build.VERSION.RELEASE)
                        .append(" / API ").append(Build.VERSION.SDK_INT).append('\n')
                    append("手表设备: ").append(DeviceSupport.isWatch(app)).append('\n')
                    append("Wear 共享库: ")
                        .append(DeviceSupport.hasWearableAmbientLibrary()).append('\n')
                    append("圆屏: ").append(DeviceSupport.isRoundScreen(app)).append('\n')
                    append("线程: ").append(t.name).append("\n\n")
                    append(e.javaClass.name).append(": ").append(e.message ?: "").append('\n')
                    append(e.stackTraceToString())
                }
                File(app.filesDir, FILE_NAME).writeText(text.take(8000))
            }
            previous?.uncaughtException(t, e)
        }
    }
}
