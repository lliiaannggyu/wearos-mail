package com.workbuddy.wearosmail

import com.workbuddy.wearosmail.mail.AccountSet
import com.workbuddy.wearosmail.mail.MailAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSetTest {

    private fun acct(email: String) = MailAccount(
        email = email,
        password = "pw-$email",
        imapHost = "imap.$email",
        smtpHost = "smtp.$email",
    )

    @Test
    fun `empty set has no active account`() {
        assertNull(AccountSet.EMPTY.active)
        assertEquals(0, AccountSet.EMPTY.accounts.size)
    }

    @Test
    fun `legacy single account migrates and stays active`() {
        val set = AccountSet.fromLegacy(acct("a@x.com"))
        assertEquals(1, set.accounts.size)
        assertEquals("a@x.com", set.active?.email)
        assertTrue(set.isActive("a@x.com"))
    }

    @Test
    fun `add multiple accounts and switch without re-login`() {
        var set = AccountSet.EMPTY
        set = set.addOrUpdate(acct("a@x.com"))
        set = set.addOrUpdate(acct("b@x.com"))
        set = set.addOrUpdate(acct("c@x.com"))

        assertEquals(3, set.accounts.size)
        assertEquals("c@x.com", set.active?.email) // 新增即激活

        set = set.switchTo("a@x.com")
        assertEquals("a@x.com", set.active?.email)
        assertEquals(3, set.accounts.size) // 切换不丢账号
        assertTrue(set.isActive("a@x.com"))
    }

    @Test
    fun `switch to unknown account is a no-op`() {
        val set = AccountSet.fromLegacy(acct("a@x.com"))
        val next = set.switchTo("ghost@x.com")
        assertEquals("a@x.com", next.active?.email)
    }

    @Test
    fun `addOrUpdate replaces same-email account by case-insensitive key`() {
        var set = AccountSet.fromLegacy(acct("A@x.com"))
        val updated = acct("a@X.COM").copy(password = "new")
        set = set.addOrUpdate(updated)
        assertEquals(1, set.accounts.size)
        assertEquals("new", set.active?.password)
    }

    @Test
    fun `remove active account falls back to first remaining`() {
        var set = AccountSet.EMPTY
        set = set.addOrUpdate(acct("a@x.com"))
        set = set.addOrUpdate(acct("b@x.com"))
        set = set.addOrUpdate(acct("c@x.com"))
        assertEquals("c@x.com", set.active?.email)

        set = set.remove("c@x.com")
        assertEquals(2, set.accounts.size)
        assertEquals("a@x.com", set.active?.email) // 自动切到第一个
    }

    @Test
    fun `remove non-active keeps active`() {
        var set = AccountSet.EMPTY
        set = set.addOrUpdate(acct("a@x.com"))
        set = set.addOrUpdate(acct("b@x.com"))
        set = set.remove("a@x.com")
        assertEquals("b@x.com", set.active?.email)
    }

    @Test
    fun `remove last account yields empty set`() {
        val set = AccountSet.fromLegacy(acct("a@x.com")).remove("a@x.com")
        assertNull(set.active)
        assertTrue(set.accounts.isEmpty())
    }
}
