# Wear Mail — Wear OS 原生邮箱客户端

一个**从工程地基上就是 Wear OS 应用**的邮箱客户端：Kotlin + Compose for Wear OS +
`androidx.wear`，不是「普通 Android 应用改 UI」。`minSdk 25` / `targetSdk 35`，
纯 Kotlin 工程，不含 Flutter、不含任何自绘的圆屏模拟层。

## 为什么这次是原生 Wear OS 工程

| Wear OS 能力 | 本项目用的官方实现 |
| --- | --- |
| 圆屏弧形缩放列表 | `ScalingLazyColumn` + `rememberScalingLazyListState`（`androidx.wear.compose.foundation.lazy`，官方实现自带弦长收窄与中心放大） |
| 圆屏安全区 / 边缘渐晕 | `Scaffold` + `Vignette` + `TimeText` |
| 侧边滚动指示 | `PositionIndicator`（绑定 `ScalingLazyListState`） |
| 右滑返回 | `SwipeDismissableNavHost` + `rememberSwipeDismissableNavController` |
| 微光 / 常亮模式（Ambient） | `AmbientLifecycleObserver`（`androidx.wear:wear` 当前推荐 API，挂到 Activity Lifecycle），进入微光后切纯黑时钟界面 |
| 手表专属控件 | `Chip` / `CompactChip` / `Button` / `CircularProgressIndicator`（Wear Material） |
| 手表启动器识别 | `uses-feature android.hardware.type.watch` + `com.google.android.wearable.standalone=true` |
| 圆形启动图标 | `mipmap-*/ic_launcher.png` 圆形透明底，`android:roundIcon` |

`AndroidManifest.xml` 里同时声明了 `WAKE_LOCK`（微光常亮）、`REQUEST_INSTALL_PACKAGES`
（应用内更新）与 `FileProvider`。这些是普通 Android 应用不需要、Wear OS 应用才需要的东西。

## 功能

- **真实邮箱**：JavaMail Android 版（`com.sun.mail:android-mail`）走 IMAP over SSL 993
  拉取 INBOX、`\Flagged` 星标与 `\SEEN` 已读真实写回服务器；SMTP（465 SSL / 587 STARTTLS）发信。
  中文主题、MIME 编码、多部分正文由 JavaMail 解码，不自己造轮子。
- **服务商预设**：QQ / 163 / Gmail / Outlook / 自定义，使用**授权码**登录（不是邮箱密码）。
- **零虚假数据**：没有任何内置示例邮件。未登录就是空态 + 登录引导；「演示模式」已彻底移除。
- 页面：收件箱、邮件详情（按需取正文）、写邮件 / 回复、本地搜索、账户与更新。
- **应用内更新**：启动静默检查 GitHub Release，账户页手动检查 → 下载（Wear 圆环 + 百分比）
  → `FileProvider` 拉起系统安装器。GitHub API 匿名限流（60 次/小时/IP）时自动回退
  仓库静态 `version.json`（raw 域名无限流），所有请求带 `User-Agent`。

## 构建（国内镜像）

```bash
export ANDROID_HOME=/opt/android-sdk      # 需 platform-35 + build-tools 34
./gradlew :app:assembleRelease
./gradlew :app:testReleaseUnitTest   # 版本比较单测
```

- Gradle 发行版：`mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip`
- Maven 依赖：`maven.aliyun.com/repository/google`（AGP / AndroidX / Wear Compose）
  + `repository/public`（Kotlin / JavaMail / 协程）
- Release 开启 R8 代码压缩混淆 + 资源收缩；`proguard-rules.pro` 保留 JavaMail 的
  反射 provider。存在 `keystore.properties` 时用正式签名，否则自动退回 debug 签名。

产物：`app/build/outputs/apk/release/app-release.apk`（单包全架构，手表直接安装）。

## 目录

```
app/src/main/kotlin/com/workbuddy/wearosmail/
├── MainActivity.kt            # ComponentActivity + AmbientLifecycleObserver + setContent
├── MailViewModel.kt           # UI 状态（StateFlow）
├── mail/
│   ├── Models.kt              # 账号 / 邮件 / 服务商预设
│   ├── AccountStore.kt        # SharedPreferences 账号记忆
│   └── MailRepository.kt      # JavaMail：IMAP 拉取/标记、正文、SMTP 发信
├── update/UpdateManager.kt    # GitHub Release 检查 / 下载 / 安装 + version.json 回退
└── ui/
    ├── WearApp.kt             # WearMaterialTheme + SwipeDismissableNavHost + 微光界面
    ├── Common.kt              # Wear Chip 封装 + 圆形输入框
    ├── InboxScreen.kt         # Scaffold + ScalingLazyColumn + PositionIndicator
    ├── DetailScreen.kt / ComposeScreen.kt / LoginScreen.kt
    └── SearchScreen.kt / AccountScreen.kt
```

## 已知边界

- 搜索是对已拉取邮件的本地过滤，不是 IMAP `SEARCH`（手表小屏够用，避免全量下载）。
- 只做 INBOX 单文件夹；归档/删除未做（避免用「本地隐藏」冒充服务器同步）。
- 账号凭据存于 `SharedPreferences` 明文，仅适用于个人设备；生产环境应改用
  `EncryptedSharedPreferences` 或 OAuth2。
- 应用内更新需下一个版本发布后才会真正提示；当前 `version.json` 与 Release 已就绪。

## 与旧版的关系

`m3e-mail-wearos` 仓库里的 Flutter + M3E 版本是**方向错误的历史产物**：它是一个普通
Flutter Android 壳，靠手写弧形列表和边距模拟手表，没有 AmbientMode、没有任何
`androidx.wear` 依赖。本项目是独立重建的原生工程，不复用其任何代码。
