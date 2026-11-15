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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.mail.MailAccount
import com.workbuddy.wearosmail.mail.MailPresets

/**
 * 登录 / 添加账号。
 * 已有账号时以「添加账号」模式工作：保存后直接激活新账号并返回，
 * 不会清空其他账号，也不需要重新登录。
 */
@Composable
fun LoginScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val existing by vm.accounts.collectAsState()
    val isAdding = existing.isNotEmpty()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var presetIndex by remember { mutableStateOf(0) }
    var showAdvanced by remember { mutableStateOf(false) }
    var imapHost by remember { mutableStateOf("") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }
    var status by remember { mutableStateOf<String?>(null) }

    WearScreen(listState) {
        item { ListHeader { Text(if (isAdding) "添加账号" else "登录邮箱") } }
        item { Hint("使用服务商「授权码」，不是登录密码") }
        item { RoundTextField(email, { email = it }, "邮箱地址") }
        item { RoundTextField(password, { password = it }, "授权码", password = true) }

        MailPresets.all.forEachIndexed { i, p ->
            actionChip(
                label = (if (i == presetIndex) "✓ " else "") + p.label,
                onClick = {
                    presetIndex = i
                    val (hi, hs) = MailPresets.hostsFor(email.trim())
                    imapHost = p.imapHost.ifBlank { hi }
                    smtpHost = p.smtpHost.ifBlank { hs }
                    smtpPort = p.smtpPort.toString()
                },
                kind = ChipKind.UTILITY,
            )
        }

        actionChip(
            label = if (showAdvanced) "收起服务器设置" else "服务器设置",
            onClick = { showAdvanced = !showAdvanced },
            kind = ChipKind.UTILITY,
        )
        if (showAdvanced) {
            item { RoundTextField(imapHost, { imapHost = it }, "IMAP 主机") }
            item { RoundTextField(smtpHost, { smtpHost = it }, "SMTP 主机") }
            item { RoundTextField(smtpPort, { smtpPort = it }, "SMTP 端口 465/587") }
        }

        if (status != null) {
            item { Text(status ?: "", color = Color(0xFFF2B8B5), fontSize = 12.sp) }
        }

        actionChip(
            label = if (isAdding) "添加并切换" else "登录并同步",
            kind = ChipKind.MAIL,
            onClick = {
                val mail = email.trim()
                if (!mail.contains('@') || password.isBlank()) {
                    status = "请填写邮箱与授权码"
                } else {
                    val p = MailPresets.all[presetIndex]
                    val (hi, hs) = MailPresets.hostsFor(mail)
                    val account = MailAccount(
                        email = mail,
                        password = password,
                        imapHost = if (presetIndex == 4) imapHost.ifBlank { hi } else p.imapHost,
                        imapPort = p.imapPort,
                        smtpHost = if (presetIndex == 4) smtpHost.ifBlank { hs } else p.smtpHost,
                        smtpPort = if (presetIndex == 4) {
                            smtpPort.toIntOrNull() ?: 465
                        } else {
                            p.smtpPort
                        },
                    )
                    status = null
                    vm.login(account)
                    if (isAdding) {
                        nav.popBackStack()
                    } else {
                        nav.navigate("inbox") { popUpTo("login") { inclusive = true } }
                    }
                }
            },
        )
        item { Hint("QQ/163/Gmail 需先在网页端开启 IMAP/SMTP") }
        if (isAdding) {
            actionChip(
                label = "取消",
                onClick = { nav.popBackStack() },
                kind = ChipKind.UTILITY,
            )
        }
    }
}
