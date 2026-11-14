package com.workbuddy.wearosmail.mail

/** 邮箱账号。password 是服务商的「授权码」，不是登录密码。 */
data class MailAccount(
    val email: String,
    val password: String,
    val imapHost: String,
    val imapPort: Int = 993,
    val smtpHost: String,
    val smtpPort: Int = 465,
)

/** 一封邮件。uid 是 IMAP UID，用于按需取正文与写回 flag。 */
data class Email(
    val uid: Long,
    val subject: String,
    val fromName: String,
    val fromAddress: String,
    val to: String,
    val dateMillis: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val body: String = "",
) {
    val displayFrom: String get() = fromName.ifBlank { fromAddress }.ifBlank { "未知发件人" }
}

enum class MailFilter { ALL, UNREAD, STARRED }

object MailPresets {
    data class Preset(
        val label: String,
        val imapHost: String,
        val smtpHost: String,
        val smtpPort: Int,
        val imapPort: Int = 993,
    )

    val all = listOf(
        Preset("QQ 邮箱", "imap.qq.com", "smtp.qq.com", 465),
        Preset("163 邮箱", "imap.163.com", "smtp.163.com", 465),
        Preset("Gmail", "imap.gmail.com", "smtp.gmail.com", 465),
        Preset("Outlook", "outlook.office365.com", "smtp.office365.com", 587),
        Preset("自定义服务器", "", "", 465),
    )

    fun guess(email: String): Preset = when {
        email.endsWith("@qq.com", true) || email.endsWith("@foxmail.com", true) -> all[0]
        email.endsWith("@163.com", true) || email.endsWith("@126.com", true) -> all[1]
        email.endsWith("@gmail.com", true) -> all[2]
        email.endsWith("@outlook.com", true) || email.endsWith("@hotmail.com", true) -> all[3]
        else -> all[0]
    }

    /** 自定义服务器时按邮箱域名给出合理默认值。 */
    fun hostsFor(email: String): Pair<String, String> {
        val domain = email.substringAfter('@', "").trim().lowercase()
        return if (domain.isEmpty()) "" to "" else "imap.$domain" to "smtp.$domain"
    }
}
