package com.tengwear.tengemail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.tengwear.tengemail.EulaGate
import com.tengwear.tengemail.EulaText

/**
 * 启动首屏：使用协议。
 *
 * 协议按节铺在 Wear 官方 ScalingLazyColumn 里；用列表布局信息配合
 * derivedStateOf 判断「最后一项是否可见」，只有真正滑到最底部时
 * 「同意并开始使用」按钮才会启用。未同意前不会进入邮箱任何页面。
 */
@Composable
fun EulaScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    // 列表布局信息随滚动变化，derivedStateOf 只在「是否到底」翻转时触发重组。
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            EulaGate.canAgree(
                lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index,
                totalItems = info.totalItemsCount,
            )
        }
    }

    WearScreen(listState) {
        item {
            ListHeader {
                Text(EulaText.TITLE, color = Color(MailPalette.ON_SURFACE))
            }
        }
        item { Hint(EulaText.SUMMARY) }

        EulaText.SECTIONS.forEach { section ->
            item {
                Text(
                    section.title,
                    color = Color(MailPalette.ON_SURFACE),
                    fontSize = 13.sp,
                )
            }
            item { Hint(section.body) }
        }

        item {
            Hint(
                if (atBottom) "已读完全文，可以点选同意" else "请继续向下滑动阅读全文…"
            )
        }

        actionChip(
            label = "同意并开始使用",
            kind = ChipKind.COMPOSE,
            enabled = atBottom,
            onClick = onAccept,
        )
        actionChip(
            label = "不同意并退出",
            kind = ChipKind.DANGER,
            onClick = onDecline,
        )
    }
}
