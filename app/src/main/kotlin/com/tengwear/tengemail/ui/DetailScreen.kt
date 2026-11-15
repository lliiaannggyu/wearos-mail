package com.tengwear.tengemail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.tengwear.tengemail.MailViewModel

@Composable
fun DetailScreen(vm: MailViewModel, nav: NavHostController, uid: Long) {
    val listState = rememberScalingLazyListState()
    val email by vm.current.collectAsState()

    LaunchedEffect(uid) { vm.open(uid) }

    WearScreen(listState) {
        item { ListHeader { Text("邮件详情") } }

        val e = email
        if (e == null || e.uid != uid) {
            item { CircularProgressIndicator() }
            return@WearScreen
        }

        item {
            Text(
                e.subject,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(MailPalette.ON_SURFACE),
            )
        }
        item { Text("来自 ${e.displayFrom}", fontSize = 12.sp, color = Color(0xFFC8CFDD)) }
        item { Text(e.fromAddress, fontSize = 11.sp, color = Color(MailPalette.ON_SURFACE_MUTED)) }
        if (e.to.isNotBlank()) {
            item { Text("收件人 ${e.to}", fontSize = 11.sp, color = Color(MailPalette.ON_SURFACE_MUTED)) }
        }
        if (e.dateMillis > 0) {
            item { Hint(formatMailDate(e.dateMillis)) }
        }
        item {
            Text(
                e.body.ifBlank { "正在读取正文…" },
                fontSize = 13.sp,
                color = Color(MailPalette.ON_SURFACE),
            )
        }

        actionChip(
            label = if (e.isStarred) "★ 取消星标" else "☆ 加星标",
            kind = ChipKind.MAIL,
            onClick = { vm.toggleStar(e.uid) },
        )
        actionChip(
            label = "回复",
            kind = ChipKind.COMPOSE,
            onClick = { vm.startReply(e); nav.navigate("compose") },
        )
        actionChip(
            label = "返回",
            kind = ChipKind.UTILITY,
            onClick = { nav.popBackStack() },
        )
    }
}
