package com.tengwear.tengemail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.tengwear.tengemail.MailViewModel
import com.tengwear.tengemail.update.UpdateManager

/**
 * 设置页：把「关于 / 应用内更新 / 自定义种子色 / 账号切换」统一收口。
 *
 * 收件箱每页底部只有一个「设置」入口，原来的账户页不再单独暴露。
 */
@Composable
fun SettingsScreen(vm: MailViewModel, nav: NavHostController) {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current.applicationContext
    val updater = remember { UpdateManager(context) }
    val state by updater.state.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val accountSet by vm.accountSet.collectAsState()
    val seed by vm.seedColor.collectAsState()
    var confirmRemove by remember { mutableStateOf<String?>(null) }
    var hexInput by remember { mutableStateOf("") }
    var hexError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (updater.state.value is UpdateManager.State.Idle) updater.check()
    }

    WearScreen(listState) {
        item { ListHeader { Text("设置") } }

        // ---- 关于：软件名 / 版本 / 平台 / 协议 ----
        item { ListHeader { Text("关于") } }
        item {
            Text("TENG邮箱", color = Color(MailPalette.ON_SURFACE), fontSize = 14.sp)
        }
        item { Hint("软件版本 v${UpdateManager.CURRENT_VERSION}") }
        item { Hint("包名 com.tengwear.tengemail") }
        item { Hint("Wear OS / 普通安卓 · Android 8.0+ (API 25)") }
        item { Hint("IMAP / SMTP 直连真实邮箱，无内置示例数据") }
        item { Hint("开源：github.com/lliiaannggyu/wearos-mail") }

        // ---- 应用内更新 ----
        item { ListHeader { Text("应用内更新 · 当前 v${UpdateManager.CURRENT_VERSION}") } }
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

        // ---- 谷歌官方色阶：自定义种子色 ----
        item { ListHeader { Text("主题 · 官方色阶种子色") } }
        item { Hint("Material 3 Tonal Palette · 当前 ${SeedColors.hex(seed)}") }
        item { TonalStrip(seed) }
        item { Hint("tone 10 → 95（官方亮度标尺）") }

        SeedColors.PRESETS.forEach { preset ->
            val selected = preset.argb == seed
            actionChip(
                label = (if (selected) "● " else "○ ") + preset.name,
                secondaryLabel = SeedColors.hex(preset.argb),
                kind = if (selected) ChipKind.MAIL else ChipKind.UTILITY,
                onClick = { vm.setSeedColor(preset.argb); hexError = null },
            )
        }

        item { Hint("自定义种子色 HEX（#RRGGBB）") }
        item {
            RoundTextField(
                value = hexInput,
                onValueChange = { hexInput = it; hexError = null },
                placeholder = "#RRGGBB",
            )
        }
        actionChip(
            label = "应用自定义色",
            kind = ChipKind.COMPOSE,
            onClick = {
                val parsed = SeedColors.parseHex(hexInput)
                if (parsed == null) {
                    hexError = "格式错误，请输入 #RRGGBB（如 #4285F4）"
                } else {
                    vm.setSeedColor(parsed)
                    hexError = null
                }
            },
        )
        hexError?.let { msg ->
            item { Text(msg, color = Color(0xFFF2B8B5), fontSize = 12.sp) }
        }
        actionChip(
            label = "恢复官方默认紫",
            kind = ChipKind.UTILITY,
            onClick = { vm.resetSeedColor(); hexInput = ""; hexError = null },
        )

        // ---- 账号切换（与设置页合并，不再单独开账户页）----
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
                    onClick = { if (!active) vm.switchAccount(a.email) },
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

/** 当前种子色按官方 tone 标尺生成的色阶预览条。 */
@Composable
private fun TonalStrip(seed: Long) {
    val palette = remember(seed) { MailPalette(seed) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SeedColors.TONES.forEach { tone ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(palette.seedTone(tone))),
            )
        }
    }
}
