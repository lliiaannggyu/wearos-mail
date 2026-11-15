# JavaMail 通过 META-INF/javamail.providers 反射加载 provider，不能混淆
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.activation.** { *; }
-dontwarn java.awt.**
-dontwarn javax.naming.**
-dontwarn javax.security.**
-dontwarn com.sun.mail.**

# Kotlin 协程
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# 应用内更新（版本号从 BuildConfig 之外的常量读取，StateFlow 状态类保留）
-keep class com.workbuddy.wearosmail.update.** { *; }

# Wear OS 微光模式：AmbientLifecycleObserver 由 androidx.wear 通过反射回调，
# 且共享库类在普通安卓设备上不存在，必须允许缺失并保留自身兼容类。
-keep class androidx.wear.ambient.** { *; }
-dontwarn com.google.android.wearable.**
-dontwarn androidx.wear.ambient.**
-keep class com.workbuddy.wearosmail.DeviceSupport { *; }
-keep class com.workbuddy.wearosmail.CrashLog { *; }
-keep class com.workbuddy.wearosmail.CrashLog$CrashCatcher { *; }
-keep class com.workbuddy.wearosmail.ui.CrashScreenKt { *; }
