package com.tengwear.tengemail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import com.tengwear.tengemail.DeviceSupport

/**
 * 当前主题色阶：由设置页的「自定义种子色」驱动。
 *
 * 用 staticCompositionLocalOf 是为了让种子色变化时整个界面（TimeText / 进度环 /
 * 所有功能按钮）一起重组换色，而不需要在每个调用点手动传参。
 */
val LocalMailPalette = staticCompositionLocalOf { MailPalette.DEFAULT }

/**
 * 全局低饱和暗色主题。
 *
 * 用 [MailPalette] 的官方色阶（tone≈30 暗容器 / tone≈92 柔和浅字）替换 Wear
 * MaterialTheme 默认的高饱和亮色 primary，让所有系统组件（TimeText / 进度环 /
 * 页面指示点 / 文本）都不再是刺眼的亮紫色。primary 系列跟随用户自定义种子色。
 */
@Composable
fun WearMailColors(): Colors {
    val palette = LocalMailPalette.current
    return Colors(
        primary = Color(palette.containerFor(ChipKind.MAIL)),
        primaryVariant = Color(palette.toneFor(ChipKind.MAIL, 20)),
        secondary = Color(palette.containerFor(ChipKind.SEARCH)),
        secondaryVariant = Color(palette.toneFor(ChipKind.SEARCH, 20)),
        background = Color(MailPalette.BACKGROUND),
        surface = Color(MailPalette.SURFACE),
        error = Color(palette.containerFor(ChipKind.DANGER)),
        onPrimary = Color(palette.onContainerFor(ChipKind.MAIL)),
        onSecondary = Color(palette.onContainerFor(ChipKind.SEARCH)),
        onBackground = Color(MailPalette.ON_SURFACE),
        onSurface = Color(MailPalette.ON_SURFACE),
        onSurfaceVariant = Color(MailPalette.ON_SURFACE_MUTED),
        onError = Color(palette.onContainerFor(ChipKind.DANGER)),
    )
}

/** 按功能分类生成按钮配色：暗、淡、低饱和，且不同功能色相分离。 */
@Composable
fun chipColors(kind: ChipKind): ChipColors {
    val palette = LocalMailPalette.current
    val content = Color(palette.onContainerFor(kind))
    return ChipDefaults.primaryChipColors(
        backgroundColor = Color(palette.containerFor(kind)),
        contentColor = content,
        secondaryContentColor = content.copy(alpha = 0.72f),
        iconColor = content,
    )
}

/** 统一按钮：所有页面共用这一个组件，宽度由调用方在列表 item 里统一给满宽。 */
@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit,
    kind: ChipKind = ChipKind.UTILITY,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
) {
    Chip(
        label = { Text(label, maxLines = 1) },
        secondaryLabel = secondaryLabel?.let { txt -> { Text(txt, maxLines = 1) } },
        onClick = onClick,
        enabled = enabled,
        colors = chipColors(kind),
        modifier = modifier,
    )
}

/** 列表项按钮：统一宽度（fillParentMaxWidth），按功能着色。 */
fun ScalingLazyListScope.actionChip(
    label: String,
    onClick: () -> Unit,
    kind: ChipKind = ChipKind.UTILITY,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
) {
    item {
        ActionChip(
            label = label,
            onClick = onClick,
            kind = kind,
            secondaryLabel = secondaryLabel,
            enabled = enabled,
            modifier = Modifier.fillParentMaxWidth(),
        )
    }
}

/**
 * 「写邮件 + 搜索」合并行：两个对半相等、等宽等高的按钮，文字各自水平居中。
 * 写邮件用 COMPOSE 色、搜索用 SEARCH 色，功能颜色彼此分开。
 */
@Composable
fun SplitActionRow(
    leftLabel: String,
    onLeft: () -> Unit,
    rightLabel: String,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(
            label = {
                Text(
                    leftLabel,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            onClick = onLeft,
            colors = chipColors(ChipKind.COMPOSE),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
        )
        Chip(
            label = {
                Text(
                    rightLabel,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            onClick = onRight,
            colors = chipColors(ChipKind.SEARCH),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 通用 Scaffold：TimeText / Vignette / positionIndicator 全部是 androidx.wear.compose
 * 官方实现，圆屏安全区与弧形装饰由系统处理。方形屏把内容约束到手表宽度并居中。
 * positionIndicator 由调用方提供（普通页给 PositionIndicator，分页页给页面指示点）。
 */
@Composable
fun WearScreenScaffold(
    positionIndicator: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val round = DeviceSupport.isRoundScreen(LocalContext.current)
    Scaffold(
        timeText = { if (round) TimeText() },
        vignette = { if (round) Vignette(VignettePosition.TopAndBottom) },
        positionIndicator = { positionIndicator?.invoke() },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = if (round) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.widthIn(max = 360.dp).fillMaxSize()
                },
            ) {
                content()
            }
        }
    }
}

/** 普通单列表页容器：绑定 PositionIndicator（仅圆屏）。 */
@Composable
fun WearScreen(
    listState: ScalingLazyListState,
    content: ScalingLazyListScope.() -> Unit,
) {
    val round = DeviceSupport.isRoundScreen(LocalContext.current)
    WearScreenScaffold(
        positionIndicator = if (round) {
            { PositionIndicator(listState) }
        } else {
            null
        },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            content = content,
        )
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
        textStyle = TextStyle(color = Color(MailPalette.ON_SURFACE), fontSize = 13.sp),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(MailPalette.FIELD))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Color(MailPalette.ON_SURFACE_MUTED),
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
        color = Color(MailPalette.ON_SURFACE_MUTED),
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
