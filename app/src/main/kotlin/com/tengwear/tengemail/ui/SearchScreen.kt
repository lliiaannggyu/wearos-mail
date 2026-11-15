package com.tengwear.tengemail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.tengwear.tengemail.MailViewModel
import com.tengwear.tengemail.mail.MailFilter

@Composable
fun SearchScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val query by vm.query.collectAsState()
    // 搜索用独立的 searchResults（不受三页分页 filter 影响）
    val results by vm.searchResults.collectAsState()

    LaunchedEffect(Unit) { vm.setFilter(MailFilter.ALL) }

    WearScreen(listState) {
        item { ListHeader { Text("搜索") } }
        item { RoundTextField(query, { vm.setQuery(it) }, "发件人 / 主题 / 正文") }

        if (query.isBlank()) {
            item { Hint("在已拉取的邮件中查找（本地过滤）") }
        } else if (results.isEmpty()) {
            item { Hint("没有匹配的邮件") }
        } else {
            results.forEach { e ->
                item {
                    Chip(
                        label = { Text(e.displayFrom, maxLines = 1) },
                        secondaryLabel = { Text(e.subject.ifBlank { "(无主题)" }, maxLines = 1) },
                        onClick = {
                            vm.open(e.uid)
                            nav.navigate("detail/${e.uid}")
                        },
                        colors = chipColors(ChipKind.SEARCH),
                        modifier = Modifier.fillParentMaxWidth(),
                    )
                }
            }
        }

        actionChip(
            label = "返回",
            kind = ChipKind.UTILITY,
            onClick = { vm.setQuery(""); nav.popBackStack() },
        )
    }
}
