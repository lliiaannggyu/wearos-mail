package com.workbuddy.wearosmail.mail

/**
 * 多账号集合的纯逻辑（无 Android 依赖，便于 JVM 单元测试）。
 *
 * 账号以邮箱地址为唯一键；activeEmail 指向当前正在使用的账号。
 * 所有操作返回新的不可变快照，ViewModel 用 StateFlow 持有它。
 */
data class AccountSet(
    val accounts: List<MailAccount> = emptyList(),
    val activeEmail: String? = null,
) {
    /** 当前激活账号；若 activeEmail 失效则回退到第一个账号。 */
    val active: MailAccount?
        get() = accounts.firstOrNull { it.email == activeEmail }
            ?: accounts.firstOrNull()

    fun isActive(email: String): Boolean {
        val current = active ?: return false
        return current.email.equals(email, ignoreCase = true)
    }

    /** 新增或覆盖账号（同邮箱视为更新授权码/服务器），并激活它。 */
    fun addOrUpdate(account: MailAccount): AccountSet {
        val key = account.email.trim()
        val kept = accounts.filterNot { it.email.equals(key, ignoreCase = true) }
        return AccountSet(kept + account.copy(email = key), key)
    }

    /** 切换到指定邮箱；账号不存在时原样返回。 */
    fun switchTo(email: String): AccountSet = if (
        accounts.any { it.email.equals(email, ignoreCase = true) }
    ) {
        copy(activeEmail = accounts.first { it.email.equals(email, ignoreCase = true) }.email)
    } else {
        this
    }

    /** 移除账号；若移除的是当前账号则自动切到剩余第一个。 */
    fun remove(email: String): AccountSet {
        val left = accounts.filterNot { it.email.equals(email, ignoreCase = true) }
        if (left.size == accounts.size) return this
        val nextActive = if (
            activeEmail != null && !left.any { it.email.equals(activeEmail, ignoreCase = true) }
        ) {
            left.firstOrNull()?.email
        } else {
            activeEmail
        }
        return AccountSet(left, nextActive)
    }

    companion object {
        val EMPTY = AccountSet()

        /** 旧版单账号数据迁移：构造只含一个账号的集合。 */
        fun fromLegacy(account: MailAccount?): AccountSet =
            if (account == null) EMPTY else AccountSet(listOf(account), account.email)
    }
}
