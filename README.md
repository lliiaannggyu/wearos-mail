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
| 微光 / 常亮模式（Ambient） | `AmbientLifecycleObserver`（`androidx.wear:wear` 当前推荐 API，挂到 Activity Lifecycle），进入微光后切纯黑时钟界面。**按 `DeviceSupport.ambientAvailable()` 条件挂载**，避免无共享库设备启动闪退 |
| 手表专属控件 | `Chip` / `CompactChip` / `Button` / `CircularProgressIndicator`（Wear Material） |
| 手表启动器识别 | `uses-feature android.hardware.type.watch`（`required=false`，便于兼容运行）+ `com.google.android.wearable.standalone=true` |
| 圆形启动图标 | `mipmap-*/ic_launcher.png` 圆形透明底，`android:roundIcon` |

`AndroidManifest.xml` 里同时声明了 `WAKE_LOCK`（微光常亮）、`REQUEST_INSTALL_PACKAGES`
（应用内更新）与 `FileProvider`。这些是普通 Android 应用不需要、Wear OS 应用才需要的东西。

## 功能

- **真实邮箱**：JavaMail Android 版（`com.sun.mail:android-mail`）走 IMAP over SSL 993
  拉取 INBOX、`\Flagged` 星标与 `\SEEN` 已读真实写回服务器；SMTP（465 SSL / 587 STARTTLS）发信。
  中文主题、MIME 编码、多部分正文由 JavaMail 解码，不自己造轮子。
- **服务商预设**：QQ / 163 / Gmail / Outlook / 自定义，使用**授权码**登录（不是邮箱密码）。
- **零虚假数据**：没有任何内置示例邮件。未登录就是空态 + 登录引导；「演示模式」已彻底移除。
- **多账号**：可同时保存多个账号，设置页点一下即切换，不用重新登录；每个账号的收件箱有独立内存缓存，切换秒显再后台刷新。旧版单账号数据自动迁移。
- **官方色阶配色**：以 Google Material 3 官方「种子色 → Tonal Palette 色阶」为基础，按官方 tone 亮度标尺生成低饱和暗色，替换 Wear 默认高饱和亮紫 primary。不同功能按钮用不同种子色分开：邮件（可自定义）、搜索（青）、写/发邮件（绿）、其他（中性灰）、危险操作（红）。所有按钮统一宽度。
- **自定义种子色**：设置页可从 8 组 Google 官方色板预设里选，或直接输入 `#RRGGBB` 自定义；主色按官方 tone 色阶实时重算、全局生效并跨启动保存，另有 tone 10→95 色阶预览条。
- **三页左右滑动**：收件箱用官方 `HorizontalPager` + `HorizontalPageIndicator` 做「全部 / 未读 / 星标」三页左右滑动切换，去掉了原来的点击切换按钮；手表上用官方 `edgeSwipeToDismiss` 与右滑返回协调手势。写邮件与搜索合并为一行两个对半相等的按钮，**只在「全部邮件」页顶部出现且文字居中**，其余两页不显示。
- **设置页**：每页列表底部统一一个「设置」入口，收口软件名/版本等关于信息、应用内更新、自定义种子色与账号切换（原账户页已并入）。
- 页面：收件箱（三页）、邮件详情（按需取正文）、写邮件 / 回复、本地搜索、设置。
- **应用内更新**：启动静默检查 GitHub Release，设置页手动检查 → 下载（Wear 圆环 + 百分比）
  → `FileProvider` 拉起系统安装器。GitHub API 匿名限流（60 次/小时/IP）时自动回退
  仓库静态 `version.json`（raw 域名无限流），所有请求带 `User-Agent`。

## 构建（国内镜像）

```bash
export ANDROID_HOME=/opt/android-sdk      # 需 platform-35 + build-tools 34
./gradlew :app:assembleRelease
./gradlew :app:testReleaseUnitTest   # JVM 单测（色阶 / 种子色解析 / 多账号 / 过滤 / 版本 / 设备能力）
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
├── MainActivity.kt            # ComponentActivity + 条件挂载 Ambient + setContent
├── DeviceSupport.kt           # 设备能力检测（手表 / Wear 共享库 / 圆屏）
├── CrashLog.kt                # 未捕获异常落盘，下次启动可见
├── MailViewModel.kt           # UI 状态（StateFlow）+ 多账号切换/缓存 + 自定义种子色
├── ThemeStore.kt              # SharedPreferences 保存自定义种子色
├── mail/
│   ├── Models.kt              # 账号 / 邮件 / 服务商预设 / 纯过滤逻辑
│   ├── AccountSet.kt          # 多账号集合纯逻辑（可 JVM 单测）
│   ├── AccountStore.kt        # SharedPreferences 多账号 JSON + 旧版迁移
│   └── MailRepository.kt      # JavaMail：IMAP 拉取/标记、正文、SMTP 发信
├── update/UpdateManager.kt    # GitHub Release 检查 / 下载 / 安装 + version.json 回退
└── ui/
    ├── WearApp.kt             # CompositionLocal 主题 + 双导航容器 + 微光界面
    ├── MailPalette.kt         # Material 3 官方色阶 / SeedColors 官方预设与 HEX 解析
    ├── CrashScreen.kt         # 纯 foundation 的启动异常兜底页
    ├── Common.kt              # 统一宽度按钮 / 居中合并按钮行 / 主题色 / 通用 Scaffold
    ├── InboxScreen.kt         # HorizontalPager 三页 + 顶部操作行 + 底部设置入口
    ├── SettingsScreen.kt      # 关于 / 应用内更新 / 官方色阶自定义种子色 / 账号切换
    └── DetailScreen.kt / ComposeScreen.kt / LoginScreen.kt / SearchScreen.kt
```

## v1.2.0：设置页收口 / 顶部按钮 / 自定义种子色

| 诉求 | 实现 |
| --- | --- |
| 写邮件与搜索放顶部 | `InboxScreen` 把 `SplitActionRow` 移到 `ListHeader` 之后，作为列表最上方的操作行 |
| 只在「全部邮件」页保留 | `showComposeSearch = filter == MailFilter.ALL`，未读 / 星标两页不渲染；未登录时也不显示 |
| 两个按钮文字居中 | `SplitActionRow` 的 label `Text` 加 `textAlign = TextAlign.Center` + `fillMaxWidth()` |
| 列表底部设置入口 | 三页共用 `settingsEntry()`，一个「设置」Chip 进 `settings` 路由；原「账户 / 更新」按钮移除 |
| 设置页 · 关于 | 软件名 Wear 邮箱、版本、包名、平台（Wear OS / 普通安卓 API 25+）、IMAP/SMTP 说明、开源地址 |
| 设置页 · 应用内更新 | 检查 / 下载 / 安装状态机整体搬入设置页，`UpdateManager` 不变 |
| 设置页 · 账号切换 | 原 `AccountScreen` 的多账号列表 / 添加 / 移除 / 退出全部并入，删除旧文件与路由 |
| 设置页 · 官方色阶自定义种子色 | `SeedColors`（8 组 Google 官方色板预设 + `#RRGGBB` 解析）+ `ThemeStore` 持久化 + `MailPalette(seed)` 按官方 tone 标尺重算主色；`LocalMailPalette` 让全局即时换色，附 tone 10→95 色阶预览条 |

配色说明：自定义种子色只改变 **MAIL 主色**（primary / 邮件按钮 / TimeText 等），
搜索 / 写邮件 / 其他 / 危险四类仍用各自官方种子色，保证功能色相分离不被破坏。

单测新增 `SeedColorsTest`：HEX 解析与非法输入、官方预设去重与合法性、自定义种子色
只影响 MAIL、色阶亮度单调与两端黑白、纯函数确定性。

## v1.1.0：官方色阶配色 / 多账号 / 三页滑动 / 合并按钮

| 诉求 | 实现 |
| --- | --- |
| 按钮统一宽度 | `ActionChip` 组件 + `ScalingLazyListScope.actionChip` 扩展，所有按钮统一 `fillParentMaxWidth()`；原 `CompactChip` 全部换成统一 Chip |
| 多账号切换 | `AccountSet`（纯逻辑）+ `AccountStore`（多账号 JSON 存储 + 旧版单账号迁移）+ ViewModel 账号列表/切换/移除；账户页点击即切换，每账号独立内存缓存，不用重新登录 |
| 谷歌官方色阶种子色 | `MailPalette` 以 Material 3 官方「种子色 → Tonal Palette」为模型，按 tone 亮度标尺生成 0–100 色阶；5 组官方种子色对应 5 类功能 |
| 颜色改淡改暗 | 按钮容器取 tone≈30（暗），文字取 tone≈92（柔和浅色），色度向黑白两端衰减；全局 `Colors` 替换高饱和 primary |
| 功能分色 | 邮件 / 搜索 / 写发 / 其他 / 危险 各自种子色，色相分离、互不串色 |
| 写邮件+搜索合并行 | `SplitActionRow`：一行两个 `weight(1f)` 等宽按钮，左写邮件（绿）、右搜索（青） |
| 去掉筛选切换，改三页滑动 | `HorizontalPager` + Wear `HorizontalPageIndicator`，全部/未读/星标三页左右滑动；`edgeSwipeToDismiss` 与右滑返回协调 |

单元测试覆盖：官方色阶亮度单调/两端黑白/低饱和/对比度/功能色相分离、多账号增删切换与旧版迁移、三页过滤与关键字叠加。

## 已知边界

- 搜索是对已拉取邮件的本地过滤，不是 IMAP `SEARCH`（手表小屏够用，避免全量下载）。
- 只做 INBOX 单文件夹；归档/删除未做（避免用「本地隐藏」冒充服务器同步）。
- 账号凭据存于 `SharedPreferences` 明文（多账号以 JSON 数组保存），仅适用于个人设备；
  生产环境应改用 `EncryptedSharedPreferences` 或 OAuth2。
- 色阶 tone→sRGB 用 HSV 明度对官方 HCT 反解做**近似**（未引入
  `material-color-utilities`）：视觉上遵循官方 tone 亮度标尺与低饱和方向，
  但不是 Google HCT 算法的精确实现。自定义种子色只影响 MAIL 主色，其余功能色固定。
- 应用内更新需下一个版本发布后才会真正提示；当前 `version.json` 与 Release 已就绪。

## v1.0.1：修复普通安卓设备启动闪退

v1.0.0 在**不带 `com.google.android.wearable` 系统共享库**的设备上（普通安卓手机 /
平板、部分国产手表、部分模拟器）点开即闪退。根因不是布局或邮件逻辑，而是
`AmbientLifecycleObserver` 被无条件挂载：它的 `ON_CREATE` 回调会调用
`SharedLibraryVersion.verifySharedLibraryPresent()`，`Class.forName` 找不到
`com.google.android.wearable.compat.WearableActivityController` 时直接抛
`IllegalStateException`，异常发生在 `lifecycle.addObserver()` 的首帧分发里。

修复内容：

| 改动 | 作用 |
| --- | --- |
| `DeviceSupport.ambientAvailable()` | 仅当「手表特性」且「共享库类可加载」时才创建 Ambient observer；其余设备跳过微光模式 |
| `uses-feature ... android:required="false"` | 普通安卓设备可正常安装 |
| 非手表设备用标准 `NavHost` | 避免 `SwipeDismissableNavHost` 依赖 Wear 系统能力；手表仍用官方右滑返回，两者共用同一份路由表 |
| 方形屏居中限宽 + 关闭弧形装饰 | 普通安卓直屏上 Chip 不被拉伸、`TimeText`/`Vignette`/`PositionIndicator` 不错位；圆屏行为完全不变 |
| `CrashLog` + `CrashScreen` | 任何未捕获异常落盘并在下次启动显示堆栈与设备信息，替代「无声闪退」；点按钮清除并重启 |
| `addObserver` 外层 `runCatching` | 共享库存在但版本不匹配（回调签名变化）时也不崩 |

单元测试 `DeviceSupportTest` 覆盖了四种设备能力组合，确保降级逻辑不被改坏。

> 注意：这仍是 Wear OS 原生应用，兼容运行只是让它在非手表设备上也能启动与预览；
> 弧形缩放、微光模式、右滑返回等手表特性需要在真正的 Wear OS 设备上才会启用。

## 与旧版的关系

`m3e-mail-wearos` 仓库里的 Flutter + M3E 版本是**方向错误的历史产物**：它是一个普通
Flutter Android 壳，靠手写弧形列表和边距模拟手表，没有 AmbientMode、没有任何
`androidx.wear` 依赖。本项目是独立重建的原生工程，不复用其任何代码。
