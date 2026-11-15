package com.workbuddy.wearosmail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.mail.MailFilter
import com.workbuddy.wearosmail.mail.filterEmails

/** 三页左右滑动：全部 / 未读 / 星标。 */
private val INBOX_PAGES = listOf(
    MailFilter.ALL to "全部邮件",
    MailFilter.UNREAD to "未读邮件",
    MailFilter.STARRED to "星标邮件",
)

/**
 * 收件箱：Scaffold（TimeText/Vignette）+ 官方 HorizontalPager 三页 +
 * Wear 官方 HorizontalPageIndicator 页面指示点。
 *
 * 这是 Wear OS 官方推荐的「左右滑动切换页面」组合。真手表上 pager 用官方
 * edgeSwipeToDismiss 与 SwipeDismissableNavHost 的右滑返回协调：屏幕边缘的
 * 右滑交给返回手势，其余区域的左右滑交给分页。
 */
@Composable
fun InboxScreen(
    vm: MailViewModel,
    nav: NavHostController,
    swipeToDismissBoxState: SwipeToDismissBoxState? = null,
) {
    val pagerState = rememberPagerState(pageCount = { INBOX_PAGES.size })
    val pageIndicatorState = remember(pagerState) { InboxPageIndicatorState(pagerState) }

    // 三页各自独立的列表状态，切换页面时滚动位置互不干扰
    val listStateAll = rememberScalingLazyListState()
    val listStateUnread = rememberScalingLazyListState()
    val listStateStarred = rememberScalingLazyListState()
    val listStates = listOf(listStateAll, listStateUnread, listStateStarred)

    val allEmails by vm.emails.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val account by vm.account.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val unread by vm.unreadCount.collectAsState()

    WearScreenScaffold(
        positionIndicator = {
            HorizontalPageIndicator(pageIndicatorState = pageIndicatorState)
        },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val baseModifier = Modifier.fillMaxSize()
            val pagerModifier = if (swipeToDismissBoxState != null) {
                baseModifier.edgeSwipeToDismiss(swipeToDismissBoxState)
            } else {
                baseModifier
            }
            HorizontalPager(
                state = pagerState,
                modifier = pagerModifier,
            ) { page ->
                val (filter, title) = INBOX_PAGES[page]
                val pageEmails = filterEmails(allEmails, filter)
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listStates[page],
                ) {
                    inboxPageContent(
                        vm = vm,
                        nav = nav,
                        title = title,
                        pageEmails = pageEmails,
                        total = allEmails.size,
                        unread = unread,
                        accountEmail = account?.email,
                        accountCount = accounts.size,
                        loading = loading,
                        error = error,
                    )
                }
            }
        }
    }
}

/** 浏览器式页面指示器状态：把 foundation PagerState 适配成 Wear PageIndicatorState。 */
private class InboxPageIndicatorState(private val pagerState: PagerState) : PageIndicatorState {
    override val pageCount: Int get() = pagerState.pageCount
    override val selectedPage: Int get() = pagerState.currentPage
    override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
}

/** 单页列表内容：标题 + 账号信息 + 邮件列表 + 统一宽度的操作按钮。 */
private fun ScalingLazyListScope.inboxPageContent(
    vm: MailViewModel,
    nav: NavHostController,
    title: String,
    pageEmails: List<com.workbuddy.wearosmail.mail.Email>,
    total: Int,
    unread: Int,
    accountEmail: String?,
    accountCount: Int,
    loading: Boolean,
    error: String?,
) {
    item {
        ListHeader {
            Text(
                title,
                color = Color(MailPalette.ON_SURFACE),
            )
        }
    }

    if (accountEmail == null) {
        item { Hint("登录后显示真实邮件，无内置示例数据") }
        actionChip(
            label = "登录邮箱",
            onClick = { nav.navigate("login") },
            kind = ChipKind.MAIL,
        )
        return
    }

    item { Hint("$unread 封未读 · 共 $total 封") }
    item { Hint(accountEmail) }

    if (loading) {
        item { CircularProgressIndicator() }
    }

    if (error != null) {
        item { Text(error ?: "", color = Color(0xFFF2B8B5), fontSize = 12.sp) }
        actionChip(
            label = "重试",
            onClick = { vm.clearError(); vm.refresh() },
            kind = ChipKind.DANGER,
        )
    }

    pageEmails.forEach { e ->
        item {
            androidx.wear.compose.material.Chip(
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
                colors = chipColors(ChipKind.MAIL),
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
    }

    if (!loading && error == null && pageEmails.isEmpty()) {
        item { Hint("这里还没有邮件") }
    }

    // 「写邮件 + 搜索」合并成一行两个对半相等的按钮
    item {
        SplitActionRow(
            leftLabel = "写邮件",
            onLeft = { vm.startCompose(); nav.navigate("compose") },
            rightLabel = "搜索",
            onRight = { nav.navigate("search") },
            modifier = Modifier.fillParentMaxWidth(),
        )
    }

    actionChip(
        label = "刷新",
        onClick = { vm.refresh() },
        kind = ChipKind.UTILITY,
    )
    actionChip(
        label = if (accountCount > 1) "账户 · 切换账号（$accountCount）" else "账户 / 更新",
        onClick = { nav.navigate("account") },
        kind = ChipKind.UTILITY,
    )
}
