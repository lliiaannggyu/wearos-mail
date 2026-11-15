package com.tengwear.tengemail.mail

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 多账号本地记忆（SharedPreferences + JSON）。
 *
 * v1.1.0 起支持「同时保存多个账号并即时切换」，无需重新登录：
 * 所有账号一次性常驻内存（ViewModel 的 StateFlow），切换只改 activeEmail。
 *
 * 兼容 v1.0.x：旧版以平铺字段只存了单个账号，首次加载时自动迁移成
 * 「含一个账号的多账号集合」，用户不会丢登录状态。
 *
 * 生产环境应换成 EncryptedSharedPreferences / OAuth2，见 README。
 */
class AccountStore(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("wear_mail_account", Context.MODE_PRIVATE)

    fun load(): AccountSet {
        val raw = sp.getString(KEY_ACCOUNTS, null)
        if (!raw.isNullOrBlank()) {
            return runCatching { decode(raw) }.getOrDefault(AccountSet.EMPTY)
        }
        // 旧版单账号迁移
        val legacy = readLegacy()
        val migrated = AccountSet.fromLegacy(legacy)
        if (legacy != null) save(migrated)
        return migrated
    }

    fun save(set: AccountSet) {
        sp.edit()
            .putString(KEY_ACCOUNTS, encode(set.accounts))
            .putString(KEY_ACTIVE, set.activeEmail ?: "")
            .apply()
    }

    fun clear() = sp.edit().remove(KEY_ACCOUNTS).remove(KEY_ACTIVE).apply()

    /** 读取 v1.0.x 的平铺单账号字段。 */
    private fun readLegacy(): MailAccount? {
        val email = sp.getString("email", null)?.takeIf { it.isNotBlank() } ?: return null
        return MailAccount(
            email = email,
            password = sp.getString("password", "") ?: "",
            imapHost = sp.getString("imapHost", "") ?: "",
            imapPort = sp.getInt("imapPort", 993),
            smtpHost = sp.getString("smtpHost", "") ?: "",
            smtpPort = sp.getInt("smtpPort", 465),
        )
    }

    private fun encode(accounts: List<MailAccount>): String {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(JSONObject().apply {
                put("email", a.email)
                put("password", a.password)
                put("imapHost", a.imapHost)
                put("imapPort", a.imapPort)
                put("smtpHost", a.smtpHost)
                put("smtpPort", a.smtpPort)
            })
        }
        return arr.toString()
    }

    private fun decode(raw: String): AccountSet {
        val arr = JSONArray(raw)
        val list = mutableListOf<MailAccount>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val email = o.optString("email").trim()
            if (email.isEmpty()) continue
            list += MailAccount(
                email = email,
                password = o.optString("password", ""),
                imapHost = o.optString("imapHost", ""),
                imapPort = o.optInt("imapPort", 993),
                smtpHost = o.optString("smtpHost", ""),
                smtpPort = o.optInt("smtpPort", 465),
            )
        }
        // 去重（旧数据可能含重复邮箱）
        val distinct = list.distinctBy { it.email.lowercase() }
        val active = sp.getString(KEY_ACTIVE, null)?.takeIf { it.isNotBlank() }
        return AccountSet(distinct, active)
    }

    private companion object {
        const val KEY_ACCOUNTS = "accounts_json"
        const val KEY_ACTIVE = "active_email"
    }
}
