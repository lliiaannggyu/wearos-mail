package com.tengwear.tengemail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 启动异常兜底页：用最基础的 foundation 组件（不依赖 Wear Compose / MaterialTheme），
 * 保证在 Wear 组件链本身出问题时这个页面依然能显示出来。
 */
@Composable
fun CrashScreen(report: String, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        BasicText(
            "启动异常",
            style = TextStyle(color = Color(0xFFFFB4AB), fontSize = 18.sp),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            "已记录崩溃原因，可截图反馈。点下方按钮重试。",
            style = TextStyle(color = Color(0xFF9AA3B5), fontSize = 12.sp),
        )
        Spacer(Modifier.height(12.dp))
        BasicText(
            report,
            style = TextStyle(color = Color(0xFFDADCE6), fontSize = 10.sp, lineHeight = 14.sp),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2B3247))
                .clickable(onClick = onRestart)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("清除记录并重试", style = TextStyle(color = Color.White, fontSize = 13.sp))
        }
    }
}
