package com.workbuddy.wearosmail

import com.workbuddy.wearosmail.mail.Email
import com.workbuddy.wearosmail.mail.MailFilter
import com.workbuddy.wearosmail.mail.filterEmails
import org.junit.Assert.assertEquals
import org.junit.Test

class MailFilterTest {

    private fun mail(uid: Long, subject: String, read: Boolean, starred: Boolean) = Email(
        uid = uid,
        subject = subject,
        fromName = "Sender$uid",
        fromAddress = "sender$uid@example.com",
        to = "me@example.com",
        dateMillis = 0,
        isRead = read,
        isStarred = starred,
        body = "body $subject",
    )

    private val inbox = listOf(
        mail(1, "项目周报", read = false, starred = true),
        mail(2, "账单通知", read = true, starred = false),
        mail(3, "会议纪要", read = false, starred = false),
    )

    @Test
    fun `ALL returns everything`() {
        assertEquals(3, filterEmails(inbox, MailFilter.ALL).size)
    }

    @Test
    fun `UNREAD returns only unread`() {
        val r = filterEmails(inbox, MailFilter.UNREAD)
        assertEquals(listOf(1L, 3L), r.map { it.uid })
    }

    @Test
    fun `STARRED returns only starred`() {
        val r = filterEmails(inbox, MailFilter.STARRED)
        assertEquals(listOf(1L), r.map { it.uid })
    }

    @Test
    fun `three pages produce distinct counts`() {
        val counts = MailFilter.values().map { filterEmails(inbox, it).size }
        assertEquals(listOf(3, 2, 1), counts)
    }

    @Test
    fun `query filters on subject case-insensitively`() {
        val r = filterEmails(inbox, MailFilter.ALL, "账单")
        assertEquals(listOf(2L), r.map { it.uid })
        val r2 = filterEmails(inbox, MailFilter.ALL, "WEEK")
        assertEquals(0, r2.size)
    }

    @Test
    fun `query applies on top of filter`() {
        assertEquals(0, filterEmails(inbox, MailFilter.STARRED, "账单").size)
        assertEquals(1, filterEmails(inbox, MailFilter.STARRED, "周报").size)
    }
}
