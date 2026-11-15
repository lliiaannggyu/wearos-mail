package com.workbuddy.wearosmail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.workbuddy.wearosmail.MailViewModel

@Composable
fun ComposeScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val sending by vm.sending.collectAsState()

    var to by remember { mutableStateOf(vm.draftTo.value) }
    var subject by remember { mutableStateOf(vm.draftSubject.value) }
    var body by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    WearScreen(listState) {
        item { ListHeader { Text("写邮件") } }
        item { RoundTextField(to, { to = it }, "收件人") }
        item { RoundTextField(subject, { subject = it }, "主题") }
        item { RoundTextField(body, { body = it }, "正文", singleLine = false) }

        if (status != null) {
            item { Text(status ?: "", color = Color(0xFFF2B8B5), fontSize = 12.sp) }
        }
        if (sending) {
            item { CircularProgressIndicator() }
        }

        actionChip(
            label = if (sending) "发送中…" else "发送",
            kind = ChipKind.COMPOSE,
            enabled = !sending,
            onClick = {
                if (!sending) {
                    status = null
                    vm.send(to, subject, body) { ok, msg ->
                        if (ok) nav.popBackStack() else status = msg
                    }
                }
            },
        )
        actionChip(
            label = "取消",
            kind = ChipKind.UTILITY,
            onClick = { nav.popBackStack() },
        )
    }
}
