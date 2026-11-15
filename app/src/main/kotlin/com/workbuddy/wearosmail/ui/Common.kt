package com.workbuddy.wearosmail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workbuddy.wearosmail.DeviceSupport
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

/**
 * 所有列表页的统一容器。
 * Scaffold / TimeText / Vignette / PositionIndicator / ScalingLazyColumn
 * 全部是 androidx.wear.compose 官方实现：圆屏安全区、弧形缩放、
 * 顶部弧形时钟、边缘渐晕与侧边滚动指示都由系统组件处理，应用层不计算几何。
 */
@Composable
fun WearScreen(
    listState: ScalingLazyListState,
    content: ScalingLazyListScope.() -> Unit,
) {
    val round = DeviceSupport.isRoundScreen(LocalContext.current)
    Scaffold(
        timeText = { if (round) TimeText() },
        vignette = { if (round) Vignette(VignettePosition.TopAndBottom) },
        positionIndicator = { if (round) PositionIndicator(listState) },
    ) {
        // 圆屏：完全交给 Wear Scaffold 的官方安全区与弧形缩放。
        // 方形屏（普通安卓手机/平板）：把列表约束到手表宽度并居中，
        // 避免 Chip 被拉伸成整屏宽、TimeText 的弧形布局在直屏上错位。
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ScalingLazyColumn(
                modifier = if (round) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.widthIn(max = 360.dp).fillMaxSize()
                },
                state = listState,
                content = content,
            )
        }
    }
}

/** 圆形输入框（Compose foundation 的 BasicTextField，配 Wear 圆角容器）。 */
@Composable
fun RoundTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false,
    singleLine: Boolean = true,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1B2030))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Color(0xFF8A93A6),
                        fontSize = 13.sp,
                    )
                }
                inner()
            }
        },
    )
}

fun formatMailDate(millis: Long): String = if (millis <= 0) "" else {
    val d = java.util.Date(millis)
    val today = java.util.Calendar.getInstance()
    val c = java.util.Calendar.getInstance().apply { time = d }
    if (today.get(java.util.Calendar.YEAR) == c.get(java.util.Calendar.YEAR) &&
        today.get(java.util.Calendar.DAY_OF_YEAR) == c.get(java.util.Calendar.DAY_OF_YEAR)
    ) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(d)
    } else {
        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(d)
    }
}

@Composable
fun Hint(text: String) {
    Text(
        text,
        color = Color(0xFF9AA3B5),
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
