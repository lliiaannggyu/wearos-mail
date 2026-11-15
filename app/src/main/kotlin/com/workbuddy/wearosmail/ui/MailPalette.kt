package com.workbuddy.wearosmail.ui

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 功能按钮的语义分类：不同功能使用不同种子色，彼此颜色分开。
 *
 * argb 取自 Google 官方色板（Material 3 baseline key colors 与 Google 品牌色），
 * chromaScale 用于压低色度，得到「淡一点、暗一点、不亮眼」的低饱和色。
 */
enum class ChipKind(val argb: Long, val chromaScale: Float) {
    /** 邮件条目 / 回复 / 详情：Material 3 baseline primary（紫） */
    MAIL(0xFF6750A4L, 0.55f),

    /** 搜索：Teal（青） */
    SEARCH(0xFF00897BL, 0.50f),

    /** 写邮件 / 发送：Google Green */
    COMPOSE(0xFF34A853L, 0.48f),

    /** 其他功能（刷新 / 账户 / 更新 / 返回 / 星标 / 登录辅助）：Google Grey 中性 */
    UTILITY(0xFF5F6368L, 0.32f),

    /** 危险操作（退出 / 删除账号）：Google Red */
    DANGER(0xFFEA4335L, 0.50f),
}

/**
 * Google Material 3 官方「种子色 → Tonal Palette 色阶」配色。
 *
 * 官方色彩系统（m3.material.io/styles/color）以「种子色 seed color」为输入，在 HCT
 * 色彩空间按 tone（= CIE L*，0 全黑、100 全白）生成一组色阶
 * （0/10/20/30/40/50/60/70/80/90/95/99/100），供 primary / secondary / tertiary /
 * neutral 等色板取用。本类把这套官方色阶机制用于 Wear OS 深色表盘：
 *
 *  - 种子色取自 Google 官方色板（见 [ChipKind]），共 5 组，构成「多种颜色色阶」；
 *  - 按官方 tone 亮度标尺生成 0..100 的连续色阶，色相随种子、亮度随 tone 单调变化；
 *  - 生成时压低色度（chromaScale），且色度向 tone 两端衰减，得到低饱和、柔和的颜色，
 *    替换原先高饱和、亮眼的默认 primary；
 *  - 按钮容器取 tone≈30（暗），内容文字取 tone≈92（浅但柔和，非纯白），保证对比度。
 *
 * tone→sRGB 用 HSV 明度对官方 HCT 反解做近似：种子色只贡献 hue 与受限 chroma，
 * 亮度完全由 tone 决定。这样无需引入 material-color-utilities 依赖，且可用 JVM 单测
 * 验证亮度单调、低饱和与不同种子色相分离。
 */
object MailPalette {

    /** 深色表盘背景 / 表面（官方 neutral 色阶暗端）。 */
    const val BACKGROUND = 0xFF0B0E14L
    const val SURFACE = 0xFF161B26L
    const val FIELD = 0xFF1B2030L
    const val ON_SURFACE = 0xFFE4E7F0L
    const val ON_SURFACE_MUTED = 0xFF9AA3B5L

    /** 按钮容器色（暗、低饱和）。 */
    @JvmStatic
    fun container(kind: ChipKind): Long = tone(kind, 30)

    /** 按钮内容文字色（浅、柔和）。 */
    @JvmStatic
    fun onContainer(kind: ChipKind): Long = tone(kind, 92)

    /** 生成 [kind] 种子色在指定 tone 上的色阶值（0xAARRGGBB 布局的 Long）。 */
    @JvmStatic
    fun tone(kind: ChipKind, tone: Int): Long {
        val t = tone.coerceIn(0, 100) / 100f
        val hsv = rgbToHsv(kind.argb)
        // 色度向黑白两端衰减：tone=0/100 时为无彩色，中间保留一点柔和色调。
        val chromaFalloff = 1f - abs(2f * t - 1f)
        val chroma = (hsv[1] * kind.chromaScale * chromaFalloff).coerceIn(0f, 1f)
        return hsvToArgb(hsv[0], chroma, t)
    }

    private fun rgbToHsv(argb: Long): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        var h = 0f
        if (d > 0f) {
            h = when (max) {
                r -> ((g - b) / d) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            h = (h * 60f + 360f) % 360f
        }
        val s = if (max == 0f) 0f else d / max
        return floatArrayOf(h, s, max)
    }

    private fun hsvToArgb(h: Float, s: Float, v: Float): Long {
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
