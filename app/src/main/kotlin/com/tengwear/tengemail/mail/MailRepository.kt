package com.tengwear.tengemail.mail

import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Date
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.FetchProfile
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage

/**
 * 真实邮件收发：JavaMail Android 版。
 * - IMAP over SSL 993：拉取 ENVELOPE/FLAGS、按需取正文、写回 \SEEN / \Flagged
 * - SMTP 465 SSL 或 587 STARTTLS：UTF-8 发信
 * MIME 编码主题、中文、多部分正文全部交给 JavaMail 解码，不自己实现协议。
 */
class MailRepository {

    private fun auth(a: MailAccount) = object : Authenticator() {
        override fun getPasswordAuthentication() =
            PasswordAuthentication(a.email, a.password)
    }

    private fun imapSession(a: MailAccount): Session =
        Session.getInstance(Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", a.imapHost)
            put("mail.imaps.port", a.imapPort.toString())
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.auth", "true")
            put("mail.imaps.connectiontimeout", 15000)
            put("mail.imaps.timeout", 15000)
            put("mail.imaps.writetimeout", 15000)
        }, auth(a))

    private fun openInbox(a: MailAccount, mode: Int): Pair<javax.mail.Store, IMAPFolder> {
        val store = imapSession(a).getStore("imaps")
        store.connect(a.imapHost, a.imapPort, a.email, a.password)
        val folder = store.getFolder("INBOX") as IMAPFolder
        folder.open(mode)
        return store to folder
    }

    suspend fun fetchInbox(a: MailAccount, limit: Int = 40): List<Email> =
        withContext(Dispatchers.IO) {
            val (store, folder) = openInbox(a, Folder.READ_ONLY)
            try {
                val count = folder.messageCount
                if (count <= 0) return@withContext emptyList()
                val from = maxOf(1, count - limit + 1)
                val msgs: Array<Message> = folder.getMessages(from, count)
                folder.fetch(msgs, FetchProfile().apply {
                    add(FetchProfile.Item.ENVELOPE)
                    add(FetchProfile.Item.FLAGS)
                })
                msgs.reversed().map { m ->
                    val addr = (m.from?.firstOrNull() as? InternetAddress)
                    val to = m.getRecipients(Message.RecipientType.TO)
                        ?.mapNotNull { (it as? InternetAddress)?.address }
                        ?.joinToString(", ") ?: ""
                    val date = runCatching {
                        (m.sentDate ?: m.receivedDate)?.time ?: 0L
                    }.getOrDefault(0L)
                    Email(
                        uid = runCatching { folder.getUID(m) }.getOrDefault(m.messageNumber.toLong()),
                        subject = runCatching { m.subject ?: "(无主题)" }.getOrDefault("(无主题)"),
                        fromName = runCatching { addr?.personal ?: "" }.getOrDefault(""),
                        fromAddress = addr?.address ?: "",
                        to = to,
                        dateMillis = date,
                        isRead = m.isSet(Flags.Flag.SEEN),
                        isStarred = m.isSet(Flags.Flag.FLAGGED),
                    )
                }
            } finally {
                runCatching { folder.close(false) }
                runCatching { store.close() }
            }
        }

    suspend fun fetchBody(a: MailAccount, uid: Long): String = withContext(Dispatchers.IO) {
        val (store, folder) = openInbox(a, Folder.READ_ONLY)
        try {
            val m = folder.getMessageByUID(uid) ?: return@withContext ""
            val fp = FetchProfile().apply { add(FetchProfile.Item.CONTENT_INFO) }
            folder.fetch(arrayOf(m), fp)
            extractText(m).ifBlank { "(此邮件无可显示的纯文本内容)" }
        } finally {
            runCatching { folder.close(false) }
            runCatching { store.close() }
        }
    }

    suspend fun setFlag(a: MailAccount, uid: Long, flag: Flags.Flag, value: Boolean) =
        withContext(Dispatchers.IO) {
            val (store, folder) = openInbox(a, Folder.READ_WRITE)
            try {
                folder.getMessageByUID(uid)?.setFlag(flag, value)
            } finally {
                runCatching { folder.close(false) }
                runCatching { store.close() }
            }
        }

    suspend fun send(a: MailAccount, to: String, subject: String, body: String) =
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.host", a.smtpHost)
                put("mail.smtp.port", a.smtpPort.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.connectiontimeout", 15000)
                put("mail.smtp.timeout", 15000)
                if (a.smtpPort == 587) {
                    put("mail.smtp.starttls.enable", "true")
                } else {
                    put("mail.smtp.socketFactory.port", a.smtpPort.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                }
            }
            val session = Session.getInstance(props, auth(a))
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(a.email))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject, "UTF-8")
                setText(body, "UTF-8")
                sentDate = Date()
                saveChanges()
            }
            val transport: Transport = session.getTransport("smtp")
            try {
                transport.connect(a.smtpHost, a.smtpPort, a.email, a.password)
                transport.sendMessage(msg, msg.allRecipients ?: return@withContext)
            } finally {
                transport.close()
            }
        }

    private fun extractText(part: Part): String = try {
        when (val c = part.content) {
            is String -> toPlain(c)
            is Multipart -> buildString {
                for (i in 0 until c.count) {
                    val bp = c.getBodyPart(i)
                    if ((bp as? MimeBodyPart)?.disposition
                            ?.equals(Part.ATTACHMENT, true) == true
                    ) continue
                    val t = extractText(bp)
                    if (t.isNotBlank()) appendLine(t)
                }
            }
            is InputStream -> c.bufferedReader(Charsets.UTF_8).use { toPlain(it.readText()) }
            else -> ""
        }.trim()
    } catch (_: Exception) {
        ""
    }

    private fun toPlain(s: String): String = if (
        s.contains("<html", true) || s.contains("<body", true) || s.contains("<div", true)
    ) {
        s.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ").replace("&#160;", " ")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            .replace(Regex("\\s+"), " ").trim()
    } else {
        s.replace(Regex("\\r\\n"), "\n")
    }
}
