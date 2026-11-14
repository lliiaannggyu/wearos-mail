package com.workbuddy.wearosmail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.workbuddy.wearosmail.MailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WearMailApp(ambient: Boolean, vm: MailViewModel) {
    MaterialTheme {
        if (ambient) {
            AmbientScreen(vm)
            return@MaterialTheme
        }

        val nav: NavHostController = rememberSwipeDismissableNavController()
        val account by vm.account.collectAsState()

        // 登出或被踢回登录时，清掉返回栈，避免右滑回到旧账号的收件箱
        LaunchedEffect(account) {
            if (account == null) {
                nav.navigate("login") {
                    popUpTo("inbox") { inclusive = true }
                }
            }
        }

        // SwipeDismissableNavHost 是 Wear OS 官方导航：所有二级页面原生右滑返回
        SwipeDismissableNavHost(
            navController = nav,
            startDestination = if (account == null) "login" else "inbox",
        ) {
            composable("login") { LoginScreen(vm, nav) }
            composable("inbox") { InboxScreen(vm, nav) }
            composable("detail/{uid}") { entry ->
                val uid = entry.arguments?.getString("uid")?.toLongOrNull() ?: 0L
                DetailScreen(vm, nav, uid)
            }
            composable("compose") { ComposeScreen(vm, nav) }
            composable("search") { SearchScreen(vm, nav) }
            composable("account") { AccountScreen(vm, nav) }
        }
    }
}

/**
 * 微光（Ambient / always-on）界面。
 * 由 FragmentActivity + AmbientModeSupport 在进入低功耗状态时切换过来：
 * 纯黑底、白色时钟、无动画、低刷新，符合 Wear OS 微光模式规范。
 */
@Composable
private fun AmbientScreen(vm: MailViewModel) {
    val unread by vm.unreadCount.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(6.dp))
            Text("WEAR 邮箱", color = Color(0xFFB0B0B0), fontSize = 12.sp)
            if (unread > 0) {
                Text("$unread 封未读", color = Color(0xFFB0B0B0), fontSize = 12.sp)
            }
        }
    }
}
