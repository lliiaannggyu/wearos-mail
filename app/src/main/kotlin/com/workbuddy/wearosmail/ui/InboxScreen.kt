package com.workbuddy.wearosmail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.mail.MailFilter

private fun label(f: MailFilter) = when (f) {
    MailFilter.ALL -> "全部邮件"
    MailFilter.UNREAD -> "仅未读"
    MailFilter.STARRED -> "仅星标"
}

private fun next(f: MailFilter) = when (f) {
    MailFilter.ALL -> MailFilter.UNREAD
    MailFilter.UNREAD -> MailFilter.STARRED
    MailFilter.STARRED -> MailFilter.ALL
}

@Composable
fun InboxScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val emails by vm.visibleEmails.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val account by vm.account.collectAsState()
    val filter by vm.filter.collectAsState()
    val unread by vm.unreadCount.collectAsState()

    WearScreen(listState) {
        item { ListHeader { Text("收件箱") } }

        if (account == null) {
            item { Hint("登录后显示真实邮件，无内置示例数据") }
            item {
                Chip(
                    label = { Text("登录邮箱") },
                    onClick = { nav.navigate("login") },
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
            return@WearScreen
        }

        item { Hint("$unread 封未读 · 共 ${emails.size} 封") }
        item {
            Chip(
                label = { Text(label(filter)) },
                secondaryLabel = { Text("点击切换筛选") },
                onClick = { vm.setFilter(next(filter)) },
                modifier = Modifier.fillParentMaxWidth(),
            )
        }

        if (loading) {
            item { CircularProgressIndicator() }
        }

        if (error != null) {
            item { Text(error ?: "", color = Color(0xFFFFB4AB), fontSize = 12.sp) }
            item {
                CompactChip(
                    onClick = { vm.clearError(); vm.refresh() },
                    label = { Text("重试") },
                )
            }
        }

        emails.forEach { e ->
            item {
                Chip(
                    label = {
                        Text(
                            (if (!e.isRead) "● " else "") +
                                (if (e.isStarred) "★ " else "") + e.displayFrom,
                            maxLines = 1,
                        )
                    },
                    secondaryLabel = {
                        Text(
                            "${e.subject.ifBlank { "(无主题)" }}  ·  ${formatMailDate(e.dateMillis)}",
                            maxLines = 1,
                        )
                    },
                    onClick = {
                        vm.open(e.uid)
                        nav.navigate("detail/${e.uid}")
                    },
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
        }

        if (!loading && error == null && emails.isEmpty()) {
            item { Hint("收件箱为空，或当前筛选无结果") }
        }

        item {
            Chip(
                label = { Text("写邮件") },
                onClick = { vm.startCompose(); nav.navigate("compose") },
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item {
            CompactChip(onClick = { nav.navigate("search") }, label = { Text("搜索") })
        }
        item {
            CompactChip(onClick = { vm.refresh() }, label = { Text("刷新") })
        }
        item {
            CompactChip(onClick = { nav.navigate("account") }, label = { Text("账户 / 更新") })
        }
    }
}
