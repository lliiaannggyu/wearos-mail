package com.workbuddy.wearosmail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.update.UpdateManager

@Composable
fun AccountScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current.applicationContext
    val updater = remember { UpdateManager(context) }
    val state by updater.state.collectAsState()
    val account by vm.account.collectAsState()

    LaunchedEffect(Unit) {
        if (updater.state.value is UpdateManager.State.Idle) updater.check()
    }

    WearScreen(listState) {
        item { ListHeader { Text("账户与更新") } }
        item { Hint(account?.email ?: "未登录") }
        item { Hint("当前版本 ${UpdateManager.CURRENT_VERSION}") }

        when (val s = state) {
            is UpdateManager.State.Idle -> Unit
            is UpdateManager.State.Checking -> {
                item { CircularProgressIndicator() }
                item { Hint("正在检查更新…") }
            }
            is UpdateManager.State.UpToDate -> item { Hint("已是最新版本") }
            is UpdateManager.State.Available -> {
                item { Hint("发现新版本 ${s.info.version}") }
                item {
                    Chip(
                        label = { Text("下载并安装") },
                        onClick = { updater.download(s.info) },
                        modifier = Modifier.fillParentMaxWidth(),
                    )
                }
            }
            is UpdateManager.State.Downloading -> {
                item { CircularProgressIndicator(s.progress) }
                item { Hint("下载 ${(s.progress * 100).toInt()}%") }
            }
            is UpdateManager.State.Ready -> item { Hint("下载完成，正在拉起安装器") }
            is UpdateManager.State.NeedPermission -> {
                item { Hint("需要「安装未知应用」权限") }
                item {
                    Chip(
                        label = { Text("前往授权") },
                        onClick = { updater.openInstallSettings() },
                        modifier = Modifier.fillParentMaxWidth(),
                    )
                }
                item {
                    CompactChip(
                        onClick = { updater.requestInstall(s.file) },
                        label = { Text("已授权，继续") },
                    )
                }
            }
            is UpdateManager.State.Error -> {
                item { Text(s.message, color = Color(0xFFFFB4AB), fontSize = 12.sp) }
                item {
                    CompactChip(onClick = { updater.check() }, label = { Text("重试") })
                }
            }
        }

        item {
            CompactChip(onClick = { updater.check() }, label = { Text("检查更新") })
        }
        if (account != null) {
            item {
                Chip(
                    label = { Text("退出登录") },
                    onClick = {
                        vm.logout()
                        nav.navigate("login") { popUpTo("inbox") { inclusive = true } }
                    },
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
        }
        item {
            CompactChip(onClick = { nav.popBackStack() }, label = { Text("返回") })
        }
    }
}
