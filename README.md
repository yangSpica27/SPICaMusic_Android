<div align="center">

# 🍋 柠檬音乐 (SPICa Music)

**现代化 Android 音乐播放器 | Jetpack Compose + Media3 + Koin + navkit**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-29+-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3+-purple.svg)](https://kotlinlang.org)

</div>

## ✨ 预览

### 亮色主题

<p align="center">
<img src="/img/light_1.png" width="22%"/>
<img src="/img/light_2.png" width="22%"/>
<img src="/img/light_3.png" width="22%"/>
<img src="/img/light_4.png" width="22%"/>
</p>

### 暗色主题

<p align="center">
<img src="/img/night_1.png" width="22%"/>
<img src="/img/night_2.png" width="22%"/>
<img src="/img/night_3.png" width="22%"/>
<img src="/img/night_4.png" width="22%"/>
</p>

## 📖 项目简介

柠檬音乐是一款基于 **Media3 ExoPlayer**、**Jetpack Compose** 与 **Koin** 打造的现代化 Android 音乐播放器。

当前仓库采用**分层 + 多模块**架构：`app` 负责 Compose UI、应用级 DI 与后台播放服务；`feature-*-domain` 暴露 use case / facade；`feature-*-data` 提供数据实现；`common` 与 `core-preferences` 提供跨模块共享能力。支持多种高品质音频格式（FLAC / ALAC / Opus 等），并提供歌词、EQ、频谱分析、歌单与听歌统计等能力。

## 🏗️ 架构设计

### 模块职责

| 模块 | 当前职责 |
|------|----------|
| `app` | Compose UI、ViewModel、`PlaybackService`、应用级 `AppModule`、Koin 启动与运行时装配 |
| `common` | 跨层共享实体、模型与部分导航参数对象 |
| `core-preferences` | `PreferencesManager` + DataStore 偏好存储基础设施 |
| `feature-library-data` | Room 数据库、DAO、MediaStore 扫描、音乐库/歌单/播放历史仓库实现 |
| `feature-library-domain` | Song / Album / Playlist / PlayHistory / MusicScan 等 use case facade |
| `feature-player-data` | `IMusicPlayer`、`PlayerAction`、`PlayMode`、`SpicaPlayer`（MediaBrowser 客户端桥接） |
| `feature-player-domain` | 播放控制与播放器状态 facade |
| `feature-lyrics-data` | 歌词 API 能力与数据实现 |
| `feature-lyrics-domain` | 歌词查询 facade |
| `feature-settings-domain` | 设置读写 facade，复用 `core-preferences` |

## 🎵 功能特性

| 功能类别 | 具体特性 |
|----------|----------|
| 🎧 音频格式 | FLAC, ALAC, Opus, Vorbis, MP3, AAC, WAV, AC3, EAC3, DCA, MLP, TrueHD 等 |
| 📝 歌单管理 | 新增 / 编辑 / 删除歌单，智能分类 |
| 🎨 主题切换 | 亮色 / 暗色模式，动态取色 |
| 🎤 歌词功能 | 在线歌词搜索与同步显示 |
| 🎛️ 音效调节 | 自定义 EQ 均衡器，频谱可视化 |
| 📊 音频分析 | FFT 频谱分析，振幅波形显示 |
| 🔄 播放模式 | 顺序 / 随机 / 单曲循环 |

## 🚧 开发计划

- [ ] 播放队列管理
- [x] 指定文件夹扫描
- [x] 最常播放、最近播放等智能歌单能力
- [ ] 新的 EQ 和音效增强功能
- [ ] 更多在线歌词源支持
- [ ] 主题和界面自定义功能
- [ ] 其他 UI 和交互细节优化

## 🚀 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/yangSpica27/SPICaMusic_Android.git

# 2. 使用 Android Studio 打开项目

# 3. Windows 下调试构建
.\gradlew.bat :app:assembleDebug
```

**环境要求**:
- Android Studio Narwhal 2025.1.1+（或兼容 AGP 9.2.1 的版本）
- JDK 21+
- Android SDK 29+（`minSdk 29` / `targetSdk 37` / `compileSdk 37`）

### 常用 Gradle 任务

| 场景 | 命令 |
|------|------|
| 调试构建 | `.\gradlew.bat :app:assembleDebug` |
| Release 构建 | `.\gradlew.bat :app:assembleRelease` |
| Kotlin 格式化 | `.\gradlew.bat :app:ktlintFormat` |
| Kotlin 检查 | `.\gradlew.bat :app:ktlintCheck` |
| App 单元测试 | `.\gradlew.bat :app:testDebugUnitTest` |

> `app` 模块的 `preBuild` 会自动依赖 `:app:ktlintFormat`，因此构建前会先格式化 Kotlin 源码。

## ⚠️ 注意事项

本仓库遵循 **MIT 协议**。

WAV、ALAC 等格式的软解码依赖于FFmpeg，需单独许可证。
项目默认引用[jellyfin-androidx-media](https://github.com/jellyfin/jellyfin-androidx-media/releases)中的完成制品

如需自行编译，请参考:
[decoder_ffmpeg](https://github.com/androidx/media/tree/release/libraries/decoder_ffmpeg)

## 📚 技术栈

| 类别 | 技术 |
|------|------|
| **UI 框架** | [Jetpack Compose](https://developer.android.com/compose) - 声明式 UI |
| **媒体播放** | [Media3 ExoPlayer](https://github.com/androidx/media) + MediaSession / MediaLibraryService |
| **依赖注入** | [Koin](https://github.com/InsertKoinIO/koin) - 轻量级 DI 框架 |
| **本地存储** | [Room](https://developer.android.com/training/data-storage/room) + DataStore |
| **网络请求** | [Retrofit](https://github.com/square/retrofit) + [OkHttp](https://github.com/square/okhttp) |
| **JSON 解析** | [Moshi](https://github.com/square/moshi) |
| **图片加载** | [Coil3](https://github.com/coil-kt/coil) |
| **音频分析** | [Amplituda](https://github.com/lincollincol/Amplituda) + [TarsosDSP](https://github.com/JorenSix/TarsosDSP) |
| **视觉效果** | [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) - 液态玻璃效果 |
| **解码器** | [FFmpeg](https://github.com/FFmpeg/FFmpeg) - 多格式音频解码 |

## 📄 License

```
MIT License

Copyright (c) 2024 杨为智

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**如果这个项目对你有帮助，欢迎给个 ⭐ Star！**

</div>
