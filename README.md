<div align="center">

# 🍋 柠檬音乐 (SPICa Music)

**现代化 Android 音乐播放器 | Jetpack Compose + Media3 + Koin**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-29+-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)

</div>

## ✨ 预览

### 亮色主题

<p align="center">
<img src="/img/light1.png" width="22%"/>
<img src="/img/light2.png" width="22%"/>
<img src="/img/light3.png" width="22%"/>
<img src="/img/light4.png" width="22%"/>
</p>

### 暗色主题

<p align="center">
<img src="/img/night1.png" width="22%"/>
<img src="/img/night2.png" width="22%"/>
<img src="/img/night3.png" width="22%"/>
<img src="/img/night4.png" width="22%"/>
</p>

## 📖 项目简介

柠檬音乐是一款基于 **Media3 ExoPlayer** 和 **Jetpack Compose** 精心打造的现代化 Android 音乐播放器。

采用**Clean Architecture + 模块化**设计，核心逻辑与 UI 完全解耦，面向接口编程。支持多种高品质音频格式 (FLAC/ALAC/Opus 等)，提供歌词显示、EQ 调节、歌单管理等丰富功能。

## 🏗️ 架构设计

```
SPICaMusic_Android/
├── app/                    # 应用入口与组装层（导航、Service、Koin 启动）
├── common/                 # 共享实体与通用模型
├── core-preferences/       # 偏好存储基础设施
├── feature-library-data/   # 音乐库 data 层（Room、MediaStore、扫描）
├── feature-library-domain/ # 音乐库 domain 层（use case / repository facade）
├── feature-player-data/    # 播放 data 层（Media3、播放器桥接、DSP）
├── feature-player-domain/  # 播放 domain 层
├── feature-lyrics-data/    # 歌词 data 层（API、缓存 DI）
├── feature-lyrics-domain/  # 歌词 domain 层
└── feature-settings-domain/# 设置 domain 层
```

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

## 🚧 待完成功能

- [ ] 歌单相关功能
- [ ] 播放队列管理
- [ ] 指定文件夹扫描
- [ ] 最常播放/播放历史/等智能歌单功能
- [ ] 新的EQ和音效增强功能
- [ ] 更多在线歌词源支持
- [ ] 主题和界面自定义功能
- [ ] 其他一些UI和交互细节优化

## 🚀 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/spica27/SPICaMusic_Android.git

# 2. Android Studio 打开项目

# 3. 构建运行
./gradlew assembleDebug
```

**环境要求**:
- Android Studio Otter Feature Drop 3 / 2026.1.3+（或兼容 AGP 9.1 的更新版本）
- JDK 17+
- Android SDK 29+ (minSdk 29, targetSdk 36, compileSdk 37)

## ⚠️ 注意事项

本仓库遵循 **MIT 协议**。

WAV、ALAC 等格式的软解码依赖于 FFmpeg，需单独许可证。如需自行编译，请参考:
[decoder_ffmpeg](https://github.com/androidx/media/tree/release/libraries/decoder_ffmpeg)

## 📚 技术栈

| 类别 | 技术 |
|------|------|
| **UI 框架** | [Jetpack Compose](https://developer.android.com/compose) - 声明式 UI |
| **媒体播放** | [Media3 ExoPlayer](https://github.com/androidx/media) - 音视频播放 |
| **组件库** | [MIUIX](https://github.com/compose-miuix-ui/miuix) - 仿MIUI组件库 |
| **依赖注入** | [Koin](https://github.com/InsertKoinIO/koin) - 轻量级 DI 框架 |
| **网络请求** | [Retrofit](https://github.com/square/retrofit) + [OkHttp](https://github.com/square/okhttp) |
| **JSON 解析** | [Moshi](https://github.com/square/moshi) |
| **图片加载** | [Coil3](https://github.com/coil-kt/coil) |
| **音频分析** | [Amplituda](https://github.com/lincollincol/Amplituda) + [TarsosDSP](https://github.com/JorenSix/TarsosDSP) |
| **视觉效果** | [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) - 液态玻璃效果 |
| **解码器** | [FFmpeg](https://github.com/FFmpeg/FFmpeg) - 多格式音频解码 |

## 📄 License

```
MIT License

Copyright (c) 2025 SPICa27
```

---

<div align="center">

**如果这个项目对你有帮助，欢迎给个 ⭐ Star！**

</div>
