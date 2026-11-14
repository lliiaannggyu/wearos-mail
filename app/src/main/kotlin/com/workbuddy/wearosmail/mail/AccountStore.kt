package com.workbuddy.wearosmail.mail

import android.content.Context

/** 账号本地记忆。生产环境应换成 EncryptedSharedPreferences / OAuth2，见 README。 */
class AccountStore(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("wear_mail_account", Context.MODE_PRIVATE)

    fun load(): MailAccount? {
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

    fun save(a: MailAccount) {
        sp.edit()
            .putString("email", a.email)
            .putString("password", a.password)
            .putString("imapHost", a.imapHost)
            .putInt("imapPort", a.imapPort)
            .putString("smtpHost", a.smtpHost)
            .putInt("smtpPort", a.smtpPort)
            .apply()
    }

    fun clear() = sp.edit().clear().apply()
}
