package com.workbuddy.wearosmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workbuddy.wearosmail.mail.AccountStore
import com.workbuddy.wearosmail.mail.Email
import com.workbuddy.wearosmail.mail.MailAccount
import com.workbuddy.wearosmail.mail.MailFilter
import com.workbuddy.wearosmail.mail.MailRepository
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

    private val _account = MutableStateFlow<MailAccount?>(null)
    val account: StateFlow<MailAccount?> = _account.asStateFlow()

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

    val visibleEmails: StateFlow<List<Email>> =
        combine(_emails, _filter, _query) { list, f, q ->
            list.filter { e ->
                when (f) {
                    MailFilter.ALL -> true
                    MailFilter.UNREAD -> !e.isRead
                    MailFilter.STARRED -> e.isStarred
                }
            }.filter { e ->
                q.isBlank() || e.subject.contains(q, true) ||
                    e.displayFrom.contains(q, true) ||
                    e.fromAddress.contains(q, true) || e.body.contains(q, true)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = _emails
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        val saved = accountStore.load()
        _account.value = saved
        if (saved != null) refresh()
    }

    fun login(a: MailAccount) {
        accountStore.save(a)
        _account.value = a
        refresh()
    }

    fun logout() {
        accountStore.clear()
        _account.value = null
        _emails.value = emptyList()
        _current.value = null
        _error.value = null
    }

    fun refresh() {
        val a = _account.value ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _emails.value = repo.fetchInbox(a, 40)
            } catch (e: Exception) {
                _error.value = friendly(e)
            } finally {
                _loading.value = false
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
        val a = _account.value ?: return
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
        val a = _account.value ?: return
        viewModelScope.launch {
            if (runCatching { repo.setFlag(a, uid, Flags.Flag.FLAGGED, next) }.isFailure) {
                // 服务器写回失败时回滚本地状态，避免假装同步成功
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
        val a = _account.value
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
