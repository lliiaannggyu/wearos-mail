import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 正式签名可选：没有 keystore.properties 时自动退回 debug 签名，
// 保证任何人 clone 后都能直接构建。
val keystorePropsFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropsFile.exists()

android {
    namespace = "com.tengwear.tengemail"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.tengwear.tengemail"
        minSdk = 25
        // Wear OS 5（Android 14 / API 34）官方推荐 targetSdk 34。
        // targetSdk 35 时读取 Settings.Global.reduce_motion 会抛 SecurityException，
        // 导致 Wear Compose 的 LocalReduceMotion 在组合首页时崩溃。
        targetSdk = 34
        versionCode = 8
        versionName = "1.3.3"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                val p = Properties()
                p.load(FileInputStream(keystorePropsFile))
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources {
            // android-mail 与 android-activation 各带一份 NOTICE/LICENSE，合并 Java 资源时冲突
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/NOTICE.md",
                "/META-INF/NOTICE",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.md",
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    // Wear Compose 1.4.0 传递依赖的是 navigation-compose 2.6.0（对应 Compose 1.4），
    // 显式覆盖到与 Compose 1.7 匹配的版本，避免 API 冲突。
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ---- Wear OS 官方工具包：弧形列表 / 圆屏 Scaffold / 右滑返回 / 微光模式 ----
    // 1.4.1 修复 LocalReduceMotion 读取 Settings.Global.reduce_motion 抛
    // SecurityException 的启动崩溃（targetSdk >= 35 + Android 14 手表上必现）。
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")
    implementation("androidx.wear.compose:compose-navigation:1.4.1")
    implementation("androidx.wear:wear:1.3.0")

    // 邮件协议（JavaMail Android 版：IMAP over SSL / SMTP，MIME 解码由库完成）
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    testImplementation("junit:junit:4.13.2")
}
