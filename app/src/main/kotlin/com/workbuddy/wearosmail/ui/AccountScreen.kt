package com.workbuddy.wearosmail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.update.UpdateManager

@Composable
fun AccountScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current.applicationContext
    val updater = remember { UpdateManager(context) }
    val state by updater.state.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val accountSet by vm.accountSet.collectAsState()
    var confirmRemove by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (updater.state.value is UpdateManager.State.Idle) updater.check()
    }

    WearScreen(listState) {
        item { ListHeader { Text("账户与更新") } }

        // ---- 多账号列表：点击即切换，不用重新登录 ----
        item { ListHeader { Text("账号（${accounts.size}）· 点击切换") } }
        if (accounts.isEmpty()) {
            item { Hint("未登录") }
        } else {
            accounts.forEach { a ->
                val active = accountSet.isActive(a.email)
                actionChip(
                    label = (if (active) "● " else "○ ") + a.email,
                    secondaryLabel = if (active) "当前使用中" else "点击切换到此账号",
                    kind = if (active) ChipKind.MAIL else ChipKind.UTILITY,
                    onClick = {
                        if (!active) vm.switchAccount(a.email)
                    },
                )
                if (confirmRemove == a.email) {
                    actionChip(
                        label = "确认移除 ${a.email}",
                        kind = ChipKind.DANGER,
                        onClick = {
                            vm.removeAccount(a.email)
                            confirmRemove = null
                        },
                    )
                    actionChip(
                        label = "取消移除",
                        kind = ChipKind.UTILITY,
                        onClick = { confirmRemove = null },
                    )
                }
            }
        }

        actionChip(
            label = "添加账号",
            kind = ChipKind.COMPOSE,
            onClick = { nav.navigate("login") },
        )
        if (accounts.size > 1) {
            actionChip(
                label = "移除一个账号",
                kind = ChipKind.UTILITY,
                onClick = {
                    confirmRemove = if (confirmRemove == null) accountSet.active?.email else null
                },
            )
        }

        // ---- 应用内更新 ----
        item { ListHeader { Text("当前版本 ${UpdateManager.CURRENT_VERSION}") } }
        when (val s = state) {
            is UpdateManager.State.Idle -> Unit
            is UpdateManager.State.Checking -> {
                item { CircularProgressIndicator() }
                item { Hint("正在检查更新…") }
            }
            is UpdateManager.State.UpToDate -> item { Hint("已是最新版本") }
            is UpdateManager.State.Available -> {
                item { Hint("发现新版本 ${s.info.version}") }
                actionChip(
                    label = "下载并安装",
                    kind = ChipKind.COMPOSE,
                    onClick = { updater.download(s.info) },
                )
            }
            is UpdateManager.State.Downloading -> {
                item { CircularProgressIndicator(s.progress) }
                item { Hint("下载 ${(s.progress * 100).toInt()}%") }
            }
            is UpdateManager.State.Ready -> item { Hint("下载完成，正在拉起安装器") }
            is UpdateManager.State.NeedPermission -> {
                item { Hint("需要「安装未知应用」权限") }
                actionChip(
                    label = "前往授权",
                    kind = ChipKind.UTILITY,
                    onClick = { updater.openInstallSettings() },
                )
                actionChip(
                    label = "已授权，继续",
                    kind = ChipKind.UTILITY,
                    onClick = { updater.requestInstall(s.file) },
                )
            }
            is UpdateManager.State.Error -> {
                item { Text(s.message, color = Color(0xFFF2B8B5), fontSize = 12.sp) }
                actionChip(
                    label = "重试",
                    kind = ChipKind.UTILITY,
                    onClick = { updater.check() },
                )
            }
        }

        actionChip(
            label = "检查更新",
            kind = ChipKind.UTILITY,
            onClick = { updater.check() },
        )
        if (accounts.isNotEmpty()) {
            actionChip(
                label = "退出全部账号",
                kind = ChipKind.DANGER,
                onClick = {
                    vm.logout()
                    nav.navigate("login") { popUpTo(0) { inclusive = true } }
                },
            )
        }
        actionChip(
            label = "返回",
            kind = ChipKind.UTILITY,
            onClick = { nav.popBackStack() },
        )
    }
}
