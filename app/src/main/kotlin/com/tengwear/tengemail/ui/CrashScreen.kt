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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

/**
 * 启动异常兜底页：用最基础的 foundation 组件（不依赖 Wear Compose / MaterialTheme），
 * 保证在 Wear 组件链本身出问题时这个页面依然能显示出来。
 *
 * 提供「复制崩溃报告」：把完整报告写入系统剪贴板（Wear OS 手表同样支持），
 * 便于粘贴给开发者精确排查；再配合 Toast 反馈确认复制成功。
 */
@Composable
fun CrashScreen(report: String, onRestart: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

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
            if (copied) "已复制，请把报告粘贴发给开发者。" else "可点下方按钮复制报告发给开发者，或点重试。",
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
                .background(if (copied) Color(0xFF1E3A2F) else Color(0xFF2B3247))
                .clickable(enabled = !copied) {
                    runCatching {
                        clipboard.setText(AnnotatedString(report))
                        copied = true
                        Toast.makeText(context, "崩溃报告已复制", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                if (copied) "已复制 ✓" else "复制崩溃报告",
                style = TextStyle(
                    color = if (copied) Color(0xFF7FD8A0) else Color.White,
                    fontSize = 13.sp,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
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
