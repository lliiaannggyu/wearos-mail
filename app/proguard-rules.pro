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
