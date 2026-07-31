<div align="center">

# 柠檬音乐（SPICa Music）

**把本地音乐与多个云端音乐库整合到同一个 Android 播放器**

[![Release](https://img.shields.io/github/v/release/GGBond-xxg/SPICaMusic_Android)](https://github.com/GGBond-xxg/SPICaMusic_Android/releases)
[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[下载最新版](https://github.com/GGBond-xxg/SPICaMusic_Android/releases) ·
[提交问题](https://github.com/GGBond-xxg/SPICaMusic_Android/issues) ·
[源项目](https://github.com/yangSpica27/SPICaMusic_Android)

</div>

## 项目说明

本仓库基于
[yangSpica27/SPICaMusic_Android](https://github.com/yangSpica27/SPICaMusic_Android)
继续开发，但功能、界面和交互已经有较大变化，不再是源项目的原样镜像。因此 README
不再展示已经不能代表当前版本的旧界面截图，以下内容以当前代码和 Release 为准。

当前版本在保留本地音乐扫描、播放、歌词、均衡器和频谱等基础能力的同时，加入了完整的
云端音乐入口。Telegram、Emby、Jellyfin、Subsonic、网易云音乐和 QQ 音乐中的歌曲可以
与本地歌曲一起出现在“全部歌曲”页面，并按来源筛选和播放。

项目由维护者与 **Codex、ChatGPT** 协作开发；所有 AI 生成或修改的代码仍需经过人工
确认、编译、测试和实机验证。

## 相比源项目修改了什么

| 方向 | 本仓库的主要改动 |
| --- | --- |
| 统一音乐库 | 将本地歌曲和云端歌曲合并到“全部歌曲”，支持全部、本地、Telegram、Emby、Jellyfin、Subsonic、网易云音乐、QQ 音乐来源筛选 |
| Telegram | 集成 TDLib 登录状态机，支持 API 配置、手机号、验证码、两步验证密码、频道选择、分页歌曲列表、封面和音频串流 |
| 媒体服务器 | 支持 Emby 与 Jellyfin 账号连接、媒体库读取、封面加载和远程播放 |
| Subsonic | 支持 Navidrome 等 Subsonic 兼容服务器的认证、曲库、搜索、封面和串流 |
| 在线音乐 | 增加网易云音乐与 QQ 音乐的网页登录、账号曲库和播放入口 |
| 主题交互 | 封面动态取色；点击歌曲、上一首或下一首时，从触点执行窗口级圆形揭示主题动画 |
| 明暗模式 | 设置页使用“跟随系统 / 浅色 / 深色”行内选项；浅色与深色切换使用对应的圆形收缩或展开效果 |
| 封面体验 | 补充远程封面回退地址、稳定保留上一张封面并交叉淡入，减少切歌时闪烁和占位图跳变 |
| 返回交互 | 适配 Android 预测性返回，修复部分云端页面连续返回时的导航卡死问题 |
| 大列表性能 | Telegram 和统一曲库使用 Paging、稳定键和受控缓存，降低大曲库滚动与封面加载压力 |
| 播放稳定性 | 改善远程歌曲队列、上一首/下一首继续播放、主题取色和冷启动主题首帧 |
| 发布安全 | 提供不含 Telegram API 与内置加密 API 两种 APK；本地凭据使用 Android Keystore 保存 |

## 云端来源

| 来源 | 接入方式 | 当前能力 |
| --- | --- | --- |
| Telegram | TDLib 用户账号登录 | 选择频道、分页读取音频、封面、播放与统一曲库 |
| Emby | 服务器地址、用户名和密码 | 登录、曲库、搜索、封面和串流 |
| Jellyfin | 服务器地址、用户名和密码 | 登录、曲库、搜索、封面和串流 |
| Subsonic | 服务器地址、用户名和密码 | 兼容 Navidrome 等服务，支持曲库、搜索、封面和串流 |
| 网易云音乐 | 应用内网页登录 | 读取账号曲库、搜索和播放 |
| QQ 音乐 | 应用内网页登录 | 读取账号曲库、搜索和播放 |

在线服务的接口、地区限制和账号策略可能随服务方变化。请仅使用自己有权访问的账号、
频道、服务器和媒体内容。

## Telegram API 配置

Telegram 用户账号登录依赖 TDLib，因此应用必须提供 Telegram `API ID` 和 `API Hash`。
可在 [my.telegram.org](https://my.telegram.org) 创建凭据。

复制示例文件：

```text
config/telegram-api.properties.example
```

保存为：

```text
config/telegram-api.properties
```

然后在本地填写：

```properties
TELEGRAM_API_ID=123456
TELEGRAM_API_HASH=your_api_hash
```

`config/telegram-api.properties` 已被 Git 忽略，不应提交到公开仓库。

项目提供两种正式构建：

| Gradle 任务 | 输出 | Telegram API |
| --- | --- | --- |
| `:app:assembleRelease` | `SPICaMusic-版本号-no-telegram-api.apk` | 不内置，用户首次使用时在应用中填写 |
| `:app:assembleWithApi` | `SPICaMusic-版本号-with-telegram-api.apk` | 构建时读取本地配置并写入绑定签名证书的 AES-GCM 密文 |

应用内填写的 API 凭据和云端账号令牌使用 Android Keystore 加密后保存在设备本地。
内置加密可以避免 APK 中直接出现 API 明文，但客户端中的凭据无法成为绝对不可提取的
秘密；公开分发前请自行评估风险。

## 主要功能

- 本地 MediaStore 与指定文件夹音乐扫描。
- 本地及云端歌曲统一浏览和来源筛选。
- 歌曲、专辑、艺术家、收藏、歌单、最近播放与常听统计。
- 后台播放、MediaSession、媒体通知和播放队列。
- 在线歌词搜索、同步歌词和歌词页。
- EQ 均衡器、音效、FFT 频谱及多种进度条样式。
- FLAC、ALAC、Opus、Vorbis、MP3、AAC、WAV、AC3、EAC3、DTS、MLP、
  TrueHD 等格式支持。
- 封面动态取色、流体背景、浅色/深色模式和圆形揭示转场。
- Android 预测性返回与边到边界面。

## 技术与模块

主要技术栈：

- Kotlin、Jetpack Compose、Material 3
- AndroidX Media3、ExoPlayer、MediaSession
- Koin、Room、DataStore、Paging 3
- OkHttp、Retrofit、Moshi
- TDLib
- Landscapist / Coil
- Amplituda、TarsosDSP

模块职责：

| 模块 | 职责 |
| --- | --- |
| `app` | Compose UI、云端来源、ViewModel、PlaybackService、运行时装配 |
| `common` | 跨模块共享实体和模型 |
| `core-preferences` | DataStore 偏好设置及首帧渲染缓存 |
| `feature-library-data` | Room、DAO、MediaStore 扫描和音乐库仓库 |
| `feature-library-domain` | Song、Album、Playlist、PlayHistory、MusicScan 用例 |
| `feature-player-data` | MediaBrowser 客户端桥接和播放器实现 |
| `feature-player-domain` | 播放控制领域接口 |
| `feature-lyrics-data` | 歌词数据源实现 |
| `feature-lyrics-domain` | 歌词查询领域接口 |
| `feature-settings-domain` | 设置领域接口 |
| `navkit` | 场景导航和返回栈 |

## 安装

从 [GitHub Releases](https://github.com/GGBond-xxg/SPICaMusic_Android/releases)
下载最新版 APK：

- `no-telegram-api`：公开版本，不包含 Telegram API。
- `with-telegram-api`：维护者构建的内置 API 版本。

当前 APK 面向 `arm64-v8a`，最低支持 Android 10（API 29）。

## 从源码构建

环境要求：

- JDK 21
- Android SDK / compileSdk 37
- Android Studio 或命令行 Gradle 环境

Windows：

```powershell
git clone https://github.com/GGBond-xxg/SPICaMusic_Android.git
cd SPICaMusic_Android
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
git clone https://github.com/GGBond-xxg/SPICaMusic_Android.git
cd SPICaMusic_Android
./gradlew :app:assembleDebug
```

常用任务：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:assembleWithApi
.\gradlew.bat :app:ktlintCheck
```

## 项目来源与许可证

- 当前项目主页：
  [GGBond-xxg/SPICaMusic_Android](https://github.com/GGBond-xxg/SPICaMusic_Android)
- 源项目：
  [yangSpica27/SPICaMusic_Android](https://github.com/yangSpica27/SPICaMusic_Android)
- 云端串流产品与交互参考：
  [r3n011/XiangsuPlayerHQ](https://github.com/r3n011/XiangsuPlayerHQ)
- 圆形揭示动画参考：
  [YenalyLiew/CircularRevealSwitch](https://github.com/YenalyLiew/CircularRevealSwitch)

本仓库继续遵循 [MIT License](LICENSE)。第三方依赖、参考项目及适配实现的许可说明见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 和应用内“开源许可”页面。

XiangsuPlayerHQ 仅作为产品和交互参考，其源码与二进制未复制或打包到本项目。

## 参与贡献

欢迎提交 Issue 或 Pull Request。提交前建议至少执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

若改动云端来源，请避免在日志、测试数据、截图、提交记录或 PR 描述中包含真实账号、
Token、Cookie、Telegram API ID/API Hash。
