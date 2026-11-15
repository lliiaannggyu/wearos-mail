package com.workbuddy.wearosmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workbuddy.wearosmail.mail.AccountSet
import com.workbuddy.wearosmail.mail.AccountStore
import com.workbuddy.wearosmail.mail.Email
import com.workbuddy.wearosmail.mail.MailAccount
import com.workbuddy.wearosmail.mail.MailFilter
import com.workbuddy.wearosmail.mail.MailRepository
import com.workbuddy.wearosmail.mail.filterEmails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.mail.Flags

class MailViewModel(private val accountStore: AccountStore) : ViewModel() {
    private val repo = MailRepository()

    // ---- 多账号：账号集合是唯一数据源，activeEmail 决定「当前在用」 ----
    private val _accountSet = MutableStateFlow(AccountSet.EMPTY)
    val accountSet: StateFlow<AccountSet> = _accountSet.asStateFlow()

    val accounts: StateFlow<List<MailAccount>> = _accountSet
        .map { it.accounts }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 当前激活账号；未登录或全部账号被移除时为 null。 */
    val account: StateFlow<MailAccount?> = _accountSet
        .map { it.active }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _emails = MutableStateFlow<List<Email>>(emptyList())
    val emails: StateFlow<List<Email>> = _emails.asStateFlow()

    private val _filter = MutableStateFlow(MailFilter.ALL)
    val filter: StateFlow<MailFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _current = MutableStateFlow<Email?>(null)
    val current: StateFlow<Email?> = _current.asStateFlow()

    private val _draftTo = MutableStateFlow("")
    val draftTo: StateFlow<String> = _draftTo.asStateFlow()

    private val _draftSubject = MutableStateFlow("")
    val draftSubject: StateFlow<String> = _draftSubject.asStateFlow()

    /** 每个账号的收件箱内存缓存：切换账号时先秒显缓存，再后台刷新。 */
    private val mailCache = mutableMapOf<String, List<Email>>()

    /** 搜索页结果（不受三页分页 filter 影响，始终 ALL + 关键字）。 */
    val searchResults: StateFlow<List<Email>> =
        combine(_emails, _query) { list, q -> filterEmails(list, MailFilter.ALL, q) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 单页/兼容用的可见邮件（保留给仍按 filter 消费的地方）。 */
    val visibleEmails: StateFlow<List<Email>> =
        combine(_emails, _filter, _query) { list, f, q -> filterEmails(list, f, q) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = _emails
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        val saved = accountStore.load()
        _accountSet.value = saved
        if (saved.active != null) refresh()
    }

    // ---- 账号管理 ----

    /** 登录：新增/更新账号并激活，随后拉取该账号收件箱。 */
    fun login(a: MailAccount) = commit(_accountSet.value.addOrUpdate(a), refresh = true)

    /** 切换到已保存账号，不重新登录；已有缓存则秒显，无缓存才正常拉取。 */
    fun switchAccount(email: String) {
        val cur = _accountSet.value
        val next = cur.switchTo(email)
        if (next.activeEmail == cur.activeEmail) return
        val key = next.active?.email ?: return
        val cached = mailCache[key]
        _emails.value = cached ?: emptyList()
        _current.value = null
        _error.value = null
        _query.value = ""
        _filter.value = MailFilter.ALL
        // 先持久化新集合（不触发带 loading 的全量刷新），再决定刷新方式
        _accountSet.value = next
        accountStore.save(next)
        if (cached == null) refresh() else refreshSilently()
    }

    /** 移除单个账号；若移除的是当前账号，自动切到剩余第一个。 */
    fun removeAccount(email: String) {
        val wasActive = _accountSet.value.isActive(email)
        val next = _accountSet.value.remove(email)
        if (next === _accountSet.value) return
        mailCache.remove(email)
        if (next.active == null) {
            _emails.value = emptyList()
            _current.value = null
        }
        commit(next, refresh = wasActive && next.active != null)
    }

    /** 退出全部账号并清空缓存。 */
    fun logout() {
        mailCache.clear()
        _emails.value = emptyList()
        _current.value = null
        _error.value = null
        _query.value = ""
        accountStore.clear()
        _accountSet.value = AccountSet.EMPTY
    }

    private fun commit(set: AccountSet, refresh: Boolean) {
        _accountSet.value = set
        accountStore.save(set)
        if (refresh) refresh()
    }

    // ---- 邮件操作 ----

    fun refresh() {
        val a = _accountSet.value.active ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val list = repo.fetchInbox(a, 40)
                mailCache[a.email] = list
                _emails.value = list
            } catch (e: Exception) {
                _error.value = friendly(e)
            } finally {
                _loading.value = false
            }
        }
    }

    /** 有缓存时的静默刷新，不打断 UI（不弹 loading/error）。 */
    private fun refreshSilently() {
        val a = _accountSet.value.active ?: return
        viewModelScope.launch {
            runCatching { repo.fetchInbox(a, 40) }
                .onSuccess { list ->
                    mailCache[a.email] = list
                    _emails.value = list
                }
        }
    }

    fun clearError() { _error.value = null }
    fun consumeMessage() { _message.value = null }
    fun setFilter(f: MailFilter) { _filter.value = f }
    fun setQuery(q: String) { _query.value = q }

    fun startCompose() {
        _draftTo.value = ""
        _draftSubject.value = ""
    }

    fun startReply(e: Email) {
        _draftTo.value = e.fromAddress
        _draftSubject.value = "Re: " + e.subject.removePrefix("Re: ")
    }

    fun open(uid: Long) {
        val local = _emails.value.firstOrNull { it.uid == uid } ?: return
        _current.value = local
        val a = _accountSet.value.active ?: return
        if (!local.isRead) {
            _emails.value = _emails.value.map {
                if (it.uid == uid) it.copy(isRead = true) else it
            }
            viewModelScope.launch { runCatching { repo.setFlag(a, uid, Flags.Flag.SEEN, true) } }
        }
        viewModelScope.launch {
            val body = runCatching { repo.fetchBody(a, uid) }
                .getOrDefault("(正文读取失败)")
            _current.value?.let { cur ->
                if (cur.uid == uid) _current.value = cur.copy(body = body)
            }
        }
    }

    fun toggleStar(uid: Long) {
        val e = _emails.value.firstOrNull { it.uid == uid } ?: return
        val next = !e.isStarred
        _emails.value = _emails.value.map {
            if (it.uid == uid) it.copy(isStarred = next) else it
        }
        _current.value?.let { cur ->
            if (cur.uid == uid) _current.value = cur.copy(isStarred = next)
        }
        val a = _accountSet.value.active ?: return
        viewModelScope.launch {
            if (runCatching { repo.setFlag(a, uid, Flags.Flag.FLAGGED, next) }.isFailure) {
                _emails.value = _emails.value.map {
                    if (it.uid == uid) it.copy(isStarred = !next) else it
                }
                _current.value?.let { cur ->
                    if (cur.uid == uid) _current.value = cur.copy(isStarred = !next)
                }
                _message.value = "星标同步失败"
            }
        }
    }

    fun send(to: String, subject: String, body: String, onDone: (Boolean, String) -> Unit) {
        val a = _accountSet.value.active
        if (a == null) { onDone(false, "尚未登录"); return }
        if (to.isBlank()) { onDone(false, "请填写收件人"); return }
        viewModelScope.launch {
            _sending.value = true
            val ok = runCatching { repo.send(a, to.trim(), subject.ifBlank { "(无主题)" }, body) }
            _sending.value = false
            ok.fold(
                onSuccess = { onDone(true, "") },
                onFailure = { onDone(false, friendly(it)) },
            )
        }
    }

    private fun friendly(e: Throwable): String {
        val m = e.message ?: ""
        return when {
            m.contains("AuthenticationFailed", true) ||
                m.contains("Authentication failed", true) ||
                m.contains("Invalid credentials", true) -> "认证失败：请使用邮箱「授权码」，并确认 IMAP/SMTP 已开启"
            m.contains("timed out", true) || m.contains("timeout", true) -> "连接超时，请检查网络"
            m.contains("UnknownHost", true) || m.contains("ConnectException", true) -> "服务器不可达，检查主机地址"
            m.contains("SSL", true) || m.contains("certificate", true) -> "TLS 连接失败，检查端口（IMAP 993 / SMTP 465 或 587）"
            m.contains("MessagingException", true) -> "邮件服务器拒绝：${m.take(60)}"
            else -> "${e.javaClass.simpleName}: ${m.take(70)}"
        }
    }
}
