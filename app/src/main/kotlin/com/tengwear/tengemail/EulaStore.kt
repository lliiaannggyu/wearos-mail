package com.tengwear.tengemail

import android.content.Context

/**
 * 使用协议同意状态持久化。只记录「已同意的协议版本号」，不记录任何账号或邮件数据。
 *
 * 版本化存储让条款修订后可以强制用户重新阅读并同意，见 [EulaText.VERSION]。
 */
class EulaStore(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("teng_mail_eula", Context.MODE_PRIVATE)

    fun acceptedVersion(): Int = sp.getInt(KEY_VERSION, 0)

    fun isAccepted(): Boolean = EulaGate.isAccepted(acceptedVersion(), EulaText.VERSION)

    fun accept() {
        sp.edit().putInt(KEY_VERSION, EulaText.VERSION).apply()
    }

    /** 设置页/调试用：撤销同意，下次启动重新展示协议。 */
    fun revoke() = sp.edit().remove(KEY_VERSION).apply()

    private companion object {
        const val KEY_VERSION = "accepted_version"
    }
}
