package com.tengwear.tengemail

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * 设备能力检测。
 *
 * 本工程主体是原生 Wear OS 应用，但同一个 APK 也需要能在普通安卓设备
 * （手机 / 平板 / 不带 Wearable 共享库的安卓手表）上安装并正常启动。
 *
 * androidx.wear 的 AmbientLifecycleObserver 在 ON_CREATE 回调里会强依赖
 * com.google.android.wearable.compat.WearableActivityController：共享库缺失时
 * SharedLibraryVersion.verifySharedLibraryPresent() 直接抛 IllegalStateException，
 * 表现为「点开即闪退」。因此微光模式必须按设备能力条件挂载，不能无条件创建。
 */
object DeviceSupport {

    private const val WEARABLE_CONTROLLER =
        "com.google.android.wearable.compat.WearableActivityController"

    /** 系统是否声明手表硬件特性。 */
    fun isWatch(context: Context): Boolean = runCatching {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }.getOrDefault(false)

    /** 能否加载 Wear OS 微光模式所需的系统共享库类。 */
    fun hasWearableAmbientLibrary(): Boolean = runCatching {
        Class.forName(WEARABLE_CONTROLLER)
        true
    }.getOrDefault(false)

    /**
     * 纯逻辑判断，抽出来便于 JVM 单元测试覆盖：
     * 只有「手表设备」且「共享库可用」时才启用微光模式。
     */
    @JvmStatic
    fun shouldEnableAmbient(isWatch: Boolean, hasLibrary: Boolean): Boolean =
        isWatch && hasLibrary

    fun ambientAvailable(context: Context): Boolean =
        shouldEnableAmbient(isWatch(context), hasWearableAmbientLibrary())

    /** 屏幕是否圆形。方形大屏（普通安卓）时列表需要限制宽度避免拉伸。 */
    fun isRoundScreen(context: Context): Boolean =
        (context.resources.configuration.screenLayout and
            Configuration.SCREENLAYOUT_ROUND_MASK) == Configuration.SCREENLAYOUT_ROUND_YES
}
