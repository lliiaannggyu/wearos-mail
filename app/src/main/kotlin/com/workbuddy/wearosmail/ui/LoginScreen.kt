package com.workbuddy.wearosmail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.mail.MailAccount
import com.workbuddy.wearosmail.mail.MailPresets

@Composable
fun LoginScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var presetIndex by remember { mutableStateOf(0) }
    var showAdvanced by remember { mutableStateOf(false) }
    var imapHost by remember { mutableStateOf("") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }
    var status by remember { mutableStateOf<String?>(null) }

    WearScreen(listState) {
        item { ListHeader { Text("登录邮箱") } }
        item { Hint("使用服务商「授权码」，不是登录密码") }
        item { RoundTextField(email, { email = it }, "邮箱地址") }
        item { RoundTextField(password, { password = it }, "授权码", password = true) }

        MailPresets.all.forEachIndexed { i, p ->
            item {
                CompactChip(
                    onClick = {
                        presetIndex = i
                        val (hi, hs) = MailPresets.hostsFor(email.trim())
                        imapHost = p.imapHost.ifBlank { hi }
                        smtpHost = p.smtpHost.ifBlank { hs }
                        smtpPort = p.smtpPort.toString()
                    },
                    label = { Text((if (i == presetIndex) "✓ " else "") + p.label) },
                )
            }
        }

        item {
            CompactChip(
                onClick = { showAdvanced = !showAdvanced },
                label = { Text(if (showAdvanced) "收起服务器设置" else "服务器设置") },
            )
        }
        if (showAdvanced) {
            item { RoundTextField(imapHost, { imapHost = it }, "IMAP 主机") }
            item { RoundTextField(smtpHost, { smtpHost = it }, "SMTP 主机") }
            item { RoundTextField(smtpPort, { smtpPort = it }, "SMTP 端口 465/587") }
        }

        if (status != null) {
            item { Text(status ?: "", color = Color(0xFFFFB4AB), fontSize = 12.sp) }
        }

        item {
            Chip(
                label = { Text("登录并同步") },
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
                        nav.navigate("inbox") { popUpTo("login") { inclusive = true } }
                    }
                },
                modifier = Modifier.fillParentMaxWidth(),
            )
        }
        item { Hint("QQ/163/Gmail 需先在网页端开启 IMAP/SMTP") }
    }
}
