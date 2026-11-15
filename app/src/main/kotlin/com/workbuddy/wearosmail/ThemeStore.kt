package com.workbuddy.wearosmail

import android.content.Context
import com.workbuddy.wearosmail.ui.SeedColors

/**
 * 主题偏好持久化：用户自定义的 Google 官方色阶「种子色」。
 *
 * 只存一个 ARGB Long（SharedPreferences），读取时校验合法性，脏数据自动回退
 * 到官方 baseline 紫，避免异常值导致配色异常。纯存储逻辑，颜色计算在
 * [com.workbuddy.wearosmail.ui.MailPalette]。
 */
class ThemeStore(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("wear_mail_theme", Context.MODE_PRIVATE)

    fun load(): Long {
        val raw = sp.getString(KEY_SEED, null) ?: return SeedColors.DEFAULT
        val parsed = raw.toLongOrNull() ?: return SeedColors.DEFAULT
        return if (SeedColors.isValid(parsed)) parsed else SeedColors.DEFAULT
    }

    fun save(seed: Long) {
        if (!SeedColors.isValid(seed)) return
        sp.edit().putString(KEY_SEED, seed.toString()).apply()
    }

    fun reset() = sp.edit().remove(KEY_SEED).apply()

    private companion object {
        const val KEY_SEED = "seed_argb"
    }
}
