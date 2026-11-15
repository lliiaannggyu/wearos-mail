package com.tengwear.tengemail.ui

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 功能按钮的语义分类：不同功能使用不同种子色，彼此颜色分开。
 *
 * argb 取自 Google 官方色板（Material 3 baseline key colors 与 Google 品牌色），
 * chromaScale 用于压低色度，得到「淡一点、暗一点、不亮眼」的低饱和色。
 * MAIL 的种子色可被用户在设置页自定义覆盖（见 [SeedColors]）。
 */
enum class ChipKind(val argb: Long, val chromaScale: Float) {
    /** 邮件条目 / 回复 / 详情：默认取 Material 3 baseline primary（紫），可自定义 */
    MAIL(0xFF6750A4L, 0.55f),

    /** 搜索：Teal（青） */
    SEARCH(0xFF00897BL, 0.50f),

    /** 写邮件 / 发送：Google Green */
    COMPOSE(0xFF34A853L, 0.48f),

    /** 其他功能（刷新 / 设置 / 更新 / 返回 / 星标 / 登录辅助）：Google Grey 中性 */
    UTILITY(0xFF5F6368L, 0.32f),

    /** 危险操作（退出 / 删除账号）：Google Red */
    DANGER(0xFFEA4335L, 0.50f),
}

/**
 * 设置页「自定义种子色」用的 Google 官方色板预设与工具。
 *
 * 全部为纯 Kotlin（无 Android 依赖），便于 JVM 单测覆盖解析/校验。
 */
object SeedColors {
    data class Preset(val name: String, val argb: Long)

    /** Material 3 baseline primary，未自定义时的默认值。 */
    const val DEFAULT = 0xFF6750A4L

    /** 官方色阶 tone 标尺（用于设置页预览条）。 */
    val TONES = intArrayOf(10, 20, 30, 40, 50, 60, 80, 90, 95)

    /** Google 官方色板 / Material 3 baseline key colors。 */
    val PRESETS = listOf(
        Preset("官方紫 · M3 Baseline", 0xFF6750A4L),
        Preset("Google 蓝", 0xFF4285F4L),
        Preset("青 Teal", 0xFF00897BL),
        Preset("Google 绿", 0xFF34A853L),
        Preset("Google 黄", 0xFFFBBC04L),
        Preset("Google 橙", 0xFFF9AB00L),
        Preset("Google 红", 0xFFEA4335L),
        Preset("莓红", 0xFFE91E63L),
    )

    /** 合法种子色：不透明 ARGB。 */
    fun isValid(argb: Long): Boolean = argb in 0xFF000000L..0xFFFFFFFFL

    /** 解析 #RRGGBB / RRGGBB，失败返回 null。 */
    fun parseHex(input: String): Long? {
        val s = input.trim().removePrefix("#")
        if (s.length != 6) return null
        val rgb = s.toLongOrNull(16) ?: return null
        return 0xFF000000L or rgb
    }

    fun hex(argb: Long): String =
        "#" + (argb and 0xFFFFFFL).toString(16).uppercase().padStart(6, '0')
}

/**
 * Google Material 3 官方「种子色 → Tonal Palette 色阶」配色。
 *
 * 官方色彩系统以「种子色 seed color」为输入，在 HCT 色彩空间按 tone
 * （= CIE L*，0 全黑、100 全白）生成一组色阶
 * （0/10/20/30/40/50/60/70/80/90/95/99/100），供 primary / secondary /
 * tertiary / neutral 等色板取用。本类把这套官方色阶机制用于 Wear OS 深色表盘：
 *
 *  - [seedArgb] 是用户自定义的 primary 种子色，默认取官方 baseline 紫；
 *  - MAIL 功能随自定义种子色变化，其余功能仍用各自官方种子色，保证色相分离；
 *  - 按官方 tone 亮度标尺生成 0..100 连续色阶，压低色度并在两端衰减，
 *    得到低饱和柔和色，替换原先高饱和亮眼的默认 primary；
 *  - 按钮容器取 tone≈30（暗），内容文字取 tone≈92（浅但柔和，非纯白）。
 *
 * tone→sRGB 用 HSV 明度对官方 HCT 反解做近似：种子色只贡献 hue 与受限 chroma，
 * 亮度完全由 tone 决定。这样无需引入 material-color-utilities 依赖，且可用 JVM
 * 单测验证亮度单调、低饱和与不同种子色相分离。
 */
class MailPalette(val seedArgb: Long = SeedColors.DEFAULT) {

    /** 按钮容器色（暗、低饱和）。 */
    fun containerFor(kind: ChipKind): Long = toneFor(kind, 30)

    /** 按钮内容文字色（浅、柔和）。 */
    fun onContainerFor(kind: ChipKind): Long = toneFor(kind, 92)

    /** 生成 [kind] 种子色在指定 tone 上的色阶值；MAIL 使用自定义种子色。 */
    fun toneFor(kind: ChipKind, tone: Int): Long {
        val seed = if (kind == ChipKind.MAIL) seedArgb else kind.argb
        return tonal(seed, kind.chromaScale, tone)
    }

    /** 当前自定义种子色的官方色阶（设置页预览用）。 */
    fun seedTone(tone: Int): Long = tonal(seedArgb, ChipKind.MAIL.chromaScale, tone)

    companion object {
        /** 深色表盘背景 / 表面（官方 neutral 色阶暗端）。 */
        const val BACKGROUND = 0xFF0B0E14L
        const val SURFACE = 0xFF161B26L
        const val FIELD = 0xFF1B2030L
        const val ON_SURFACE = 0xFFE4E7F0L
        const val ON_SURFACE_MUTED = 0xFF9AA3B5L

        /** 默认配色（未自定义时）。 */
        @JvmStatic
        val DEFAULT = MailPalette()

        // 静态便捷入口：供 JVM 单测与不需要动态种子的常量场景使用。
        @JvmStatic
        fun container(kind: ChipKind): Long = DEFAULT.containerFor(kind)

        @JvmStatic
        fun onContainer(kind: ChipKind): Long = DEFAULT.onContainerFor(kind)

        @JvmStatic
        fun tone(kind: ChipKind, tone: Int): Long = DEFAULT.toneFor(kind, tone)

        /** 纯函数：任意种子色按官方 tone 标尺生成色阶值（0xAARRGGBB 的 Long）。 */
        @JvmStatic
        fun tonal(seed: Long, chromaScale: Float, tone: Int): Long {
            val t = tone.coerceIn(0, 100) / 100f
            val hsv = rgbToHsv(seed)
            // 色度向黑白两端衰减：tone=0/100 时为无彩色，中间保留一点柔和色调。
            val chromaFalloff = 1f - abs(2f * t - 1f)
            val chroma = (hsv[1] * chromaScale * chromaFalloff).coerceIn(0f, 1f)
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
}
