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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.SwipeDismissableNavHostState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import com.workbuddy.wearosmail.DeviceSupport
import com.workbuddy.wearosmail.MailViewModel
import com.workbuddy.wearosmail.mail.MailAccount
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WearMailApp(ambient: Boolean, vm: MailViewModel) {
    // 全局低饱和暗色主题：替换 Wear 默认的高饱和亮紫 primary
    MaterialTheme(colors = WearMailColors()) {
        if (ambient) {
            AmbientScreen(vm)
            return@MaterialTheme
        }

        // 真 Wear OS 设备用官方 SwipeDismissableNavHost（原生右滑返回）；
        // 普通安卓设备改用标准 NavHost，避免 Wear 专有滑动容器依赖缺失的
        // 系统能力。两种容器共用同一份导航图。
        val isWatch = DeviceSupport.isWatch(LocalContext.current)
        val account by vm.account.collectAsState()
        val startDestination = if (account == null) "login" else "inbox"

        if (isWatch) {
            val nav: NavHostController = rememberSwipeDismissableNavController()
            // 把同一个 swipe state 交给 NavHost 与收件箱分页，让边缘右滑返回
            // 与分页左右滑互不干扰（Wear 官方 edgeSwipeToDismiss 协调方式）。
            val swipeState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState()
            val navHostState: SwipeDismissableNavHostState =
                rememberSwipeDismissableNavHostState(swipeState)
            AccountWatcher(account, nav)
            SwipeDismissableNavHost(
                navController = nav,
                startDestination = startDestination,
                state = navHostState,
            ) {
                MailNavGraph(vm, nav, swipeState)
            }
        } else {
            val nav: NavHostController = rememberNavController()
            AccountWatcher(account, nav)
            NavHost(
                navController = nav,
                startDestination = startDestination,
            ) {
                MailNavGraph(vm, nav, null)
            }
        }
    }
}

/** 登出或被踢回登录时清掉返回栈，避免右滑/返回键回到旧账号的收件箱。 */
@Composable
private fun AccountWatcher(account: MailAccount?, nav: NavHostController) {
    LaunchedEffect(account) {
        if (account == null && nav.currentDestination?.route != "login") {
            nav.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

/** 两种导航容器共用的路由表，保证手表与手机行为一致。 */
private fun NavGraphBuilder.MailNavGraph(
    vm: MailViewModel,
    nav: NavHostController,
    swipeState: SwipeToDismissBoxState?,
) {
    composable("login") { LoginScreen(vm, nav) }
    composable("inbox") { InboxScreen(vm, nav, swipeState) }
    composable("detail/{uid}") { entry ->
        val uid = entry.arguments?.getString("uid")?.toLongOrNull() ?: 0L
        DetailScreen(vm, nav, uid)
    }
    composable("compose") { ComposeScreen(vm, nav) }
    composable("search") { SearchScreen(vm, nav) }
    composable("account") { AccountScreen(vm, nav) }
}

/**
 * 微光（Ambient / always-on）界面：纯黑底、白色时钟、无动画、低刷新，
 * 符合 Wear OS 微光模式规范。仅在具备 Wear 共享库的手表上会出现。
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
